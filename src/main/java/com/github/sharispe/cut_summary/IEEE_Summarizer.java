/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.utils.GraphActionExecutor;
import slib.graph.model.graph.G;
import slib.graph.model.graph.elements.E;
import slib.graph.model.graph.utils.Direction;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.utils.SMConstants;
import slib.utils.ex.SLIB_Exception;
import slib.utils.impl.SetUtils;

/**
 *
 * @author sharispe
 */
public class IEEE_Summarizer {

    // minimal number of observations required for a concept to be considered
    // note that the observations will not be excluded but propagated to 
    // the ancestors - only the ancestors with a minimal number of observations
    // in accordance to the specified threshold will be considered. 
    // This threshold is used to reduce the solution space. 
    private final static double MIN_NUMBER_OBSERVATIONS_IMPLICIT = 2;
    // Gives the possibility to apply a restriction to the solution explored
    // only the concepts having a propagated mass greater than the sum of the masses 
    // of the children of a concept will be considered if set to true
    private final static boolean PROPAGATED_MASS_MUST_IMPROVE = false;

    // Apply various strategies in order to reduce the number of summaries evaluated
    private boolean REDUCE_ELIGBLE_SUMMARY_SET = true;

    // --------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------
    // PARAMETERS
    // --------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------
    // Parameter defining the weight with regard to coverage
    private static double p_psi = 1;
    private static double p_delta = 1;

    // Parameter defining the penalty with regard to loss of exact information
    private static double p_delta_e_minus = 2;

    // Parameter defining the penalty with regard to the addition of plausible information
    private static double p_delta_p_plus = 0.2;

    // Parameter defining the penalty with regard to loss of plausible information
    private static double p_delta_p_minus = 0.2;

    // Parameter defining the penalty with regard to distortion of information
    private static double p_delta_d = 10;

    // Parameter defining the penalty with regard to mass losses 
    // when evaluating the distortion. 
    // The larger the value, the more important will be the penalty wrt mass values in [1;+inf]
    private static double ALPHA_BETA_DELTA_D = 1;

    // Parameter defining the penalty with regard to conciseness and redundancy
    // the more epsilon is important, the more the summaries will tend to be
    // abstract
    private static double EPSILON_LAMBDA = 20.0;

    // --------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------
    final SM_Engine engine;
    final G onto;

    private Set<URI> descriptorsInEntries;

    private int total_observations = 0;
    private Map<URI, Double> observations;
    private Map<URI, Double> observations_with_indirect;
    private Map<URI, Double> observations_4computing_plausibility;
    private Map<Set<URI>, Double> summary_scores;
    private Set<Set<URI>> summaries;

    private Set<URI> best_summary = null;
    private Double score_best_summary = null;

    public static boolean log = true;

    public static void log(Object s) {
        if (log) {
            System.out.println(s.toString());
        }
    }

    public IEEE_Summarizer(SM_Engine engine) {
        this.engine = engine;
        onto = engine.getGraph();
    }

    public IEEE_Summarizer(SM_Engine engine, Map<String, Double> parameters, boolean log, boolean REDUCE_ELIGBLE_SUMMARY_SET) {
        this(engine);
        this.log = log;
        p_psi = parameters.containsKey("p_psi") ? parameters.get("p_psi") : IEEE_Summarizer.p_psi;
        p_delta = parameters.containsKey("p_delta") ? parameters.get("p_delta") : IEEE_Summarizer.p_delta;
        p_delta_e_minus = parameters.containsKey("p_delta_e_minus") ? parameters.get("p_delta_e_minus") : IEEE_Summarizer.p_delta_e_minus;
        p_delta_p_plus = parameters.containsKey("p_delta_p_plus") ? parameters.get("p_delta_p_plus") : IEEE_Summarizer.p_delta_p_plus;
        p_delta_p_minus = parameters.containsKey("p_delta_p_minus") ? parameters.get("p_delta_p_minus") : IEEE_Summarizer.p_delta_p_minus;
        p_delta_d = parameters.containsKey("p_delta_d") ? parameters.get("p_delta_d") : IEEE_Summarizer.p_delta_d;
        ALPHA_BETA_DELTA_D = parameters.containsKey("ALPHA_BETA_DELTA_D") ? parameters.get("ALPHA_BETA_DELTA_D") : IEEE_Summarizer.ALPHA_BETA_DELTA_D;
        EPSILON_LAMBDA = parameters.containsKey("EPSILON_LAMBDA") ? parameters.get("EPSILON_LAMBDA") : IEEE_Summarizer.EPSILON_LAMBDA;
        this.REDUCE_ELIGBLE_SUMMARY_SET = REDUCE_ELIGBLE_SUMMARY_SET;
    }

    public Set<URI> getBestSummary(Set<Entry> entries, String outputFilePath) throws Exception {
        summarize(entries, outputFilePath);
        return best_summary;
    }

    public Map<Set<URI>, Double> summarize(Set<Entry> entries, String outputFilePath) throws Exception {

        log("entries: " + entries);
        log("Removing duplicate entries if any");
        entries = Utils.removeDuplicate(entries);

        log("entries: " + entries);

        descriptorsInEntries = new HashSet();

        observations = new HashMap();
        for (Entry e : entries) {
            observations.put(e.descriptor, e.weight);
            total_observations += e.weight;

            descriptorsInEntries.add(e.descriptor);
        }
        log("Total observations: " + total_observations);

        log("Observations (Explicit, i.e. focal) ------------------");
        for (URI descriptor : observations.keySet()) {
            log(descriptor + "\t" + observations.get(descriptor));
        }
        log("-------------------------");

        log("Observations (with implicit references) ------------------");

        // Compute observations considering implicit references
        observations_with_indirect = new HashMap();
        for (URI x : engine.getClasses()) {
            double owi = 0;
            for (URI y : engine.getDescendantsInc(x)) {
                owi += observations.containsKey(y) ? observations.get(y) : 0;
            }
            observations_with_indirect.put(x, owi);
//            log(x + "\t" + owi);
        }

        log("Propagate observations (to compute Plausibility) ------------------");
        observations_4computing_plausibility = new HashMap();
        for (URI x : engine.getClasses()) {

            double o_plausibilty = 0;

            for (URI y : engine.getClasses()) {

                if (engine.getDescendantsInc(x).contains(y) || engine.getDescendantsInc(y).contains(x)
                        || !SetUtils.intersection(engine.getDescendantsInc(x), engine.getDescendantsInc(y)).isEmpty()) {
                    o_plausibilty += observations.containsKey(y) ? observations.get(y) : 0;
                }
            }
            observations_4computing_plausibility.put(x, o_plausibilty);
            log(x + "\t" + o_plausibilty);
        }

        log("Computing relevant descriptors");
        log("min number of implicit observations: " + MIN_NUMBER_OBSERVATIONS_IMPLICIT);
        // a descriptor is considered relevant if the implicit observation is greater 
        // than a given threshold and if the number of implicit observations of the descriptor
        // is greater than the sum of the observations of its children

        Set<URI> relevant_descriptors = new HashSet(); // all the concepts adding information

        if (REDUCE_ELIGBLE_SUMMARY_SET) {

            for (URI x : engine.getClasses()) {

                boolean isRelevant = true;

                if (observations_with_indirect.get(x) >= MIN_NUMBER_OBSERVATIONS_IMPLICIT) {

                    // check if the propagated mass associated to the concept
                    // is more important than the propagated mass of each of its children
                    if (PROPAGATED_MASS_MUST_IMPROVE) {
                        double nb_observations_current = observations_with_indirect.get(x);

                        for (URI c : onto.getV(x, RDFS.SUBCLASSOF, Direction.IN)) {

                            if (!c.equals(x) && observations_with_indirect.get(c) >= nb_observations_current) {
                                isRelevant = false;
                                break;
                            }
                        }
                    }
                } else {
                    isRelevant = false;
                }

                if (isRelevant) {
                    relevant_descriptors.add(x);
                    log("Add: " + x);
                } else {
                    log("Remove " + x);
                }
            }
        } else {
            relevant_descriptors.addAll(engine.getClasses());
        }

        log("Compute relevant ancestors");

        // Modify the set of ancestors only considering the informative concepts
        Map<URI, Set<URI>> ancestors_relevant = new HashMap();

        for (URI x : relevant_descriptors) {

            Set<URI> ancs_relevant_current = new HashSet();

            for (URI a : engine.getAncestorsInc(x)) {
                if (relevant_descriptors.contains(a)) {
                    ancs_relevant_current.add(a);
                }
            }
            ancestors_relevant.put(x, ancs_relevant_current);
        }

        log("Compute graph only considering relevant concepts");
        // We generate the new graph by contructing the transitive closure
        // and then applying the transitive reduction
        G onto_relevant_descriptors = new GraphMemory(null);
        for (URI x : relevant_descriptors) {
            log(x);
            log(ancestors_relevant.get(x));
            for (URI a : ancestors_relevant.get(x)) {
                onto_relevant_descriptors.addE(x, RDFS.SUBCLASSOF, a);
            }
        }
        // apply transitive reduction
        GAction tr2 = new GAction(GActionType.TRANSITIVE_REDUCTION);
        GraphActionExecutor.applyAction(tr2, onto_relevant_descriptors);
        log("reduced graph: " + onto_relevant_descriptors);

        for (E e : onto_relevant_descriptors.getE()) {
            log(e);
        }

        SM_Engine engine_relevant_descriptors = new SM_Engine(onto_relevant_descriptors);

        log("Reducing set of entries");

        Set<URI> relevant_descriptors_in_entries = new HashSet();

        for (Entry e : entries) {
            if (relevant_descriptors.contains(e.descriptor)) {
                relevant_descriptors_in_entries.add(e.descriptor);
            }
        }

        log("Reduction applied: " + entries.size() + "/" + relevant_descriptors_in_entries.size());

        Set<URI> most_specific_non_ordered_relevant_descriptors = new HashSet();
        log("Removing redundant concepts for computing the set of summaries to evaluate");
        for (URI x : relevant_descriptors_in_entries) {
            boolean redundant = false;
            for (URI y : relevant_descriptors_in_entries) {
                if (!x.equals(y) && engine_relevant_descriptors.getAncestorsInc(y).contains(x)) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                most_specific_non_ordered_relevant_descriptors.add(x);
            }
        }

        log("Reduction applied: " + most_specific_non_ordered_relevant_descriptors.size() + "/" + entries.size());

        // génération des coupes
        summaries = generate_summaries(onto_relevant_descriptors, engine_relevant_descriptors, most_specific_non_ordered_relevant_descriptors);

        // search best summary
        return compute_summary_scores(outputFilePath);
    }

    /**
     * @param graph a transitive reduction has to be applied on the graph first
     * @param engine the engine associated to the graph
     * @param X initial set of concepts
     * @return the set of summaries summarizing X
     */
    public static Set<Set<URI>> generate_summaries(G graph, SM_Engine engine, Set<URI> X) {

        log("Generating summaries  for " + X.size() + " concepts, size: " + Math.pow(2, X.size()));

        Set<Set<URI>> summaries = new HashSet();
        generate_summaries_inner(graph, engine, X, summaries);

        for (Set<URI> subset_X : Utils.powerSet(X)) {
            log("generating summaries for: " + subset_X);
            summaries.add(subset_X);
            generate_summaries_inner(graph, engine, subset_X, summaries);
        }

        log(summaries.size() + " summaries generated for " + X);

        return summaries;
    }

    private static Set<Set<URI>> generate_summaries_inner(G graph, SM_Engine engine, Set<URI> X, Set<Set<URI>> summaries) {

        log("total=" + summaries.size() + "\tgenerating summaries for (" + X.size() + ")\t" + Utils.printSet(X));

        int c = 0;

        for (URI d : X) {

            c++;
            Set<URI> A_d = graph.getV(d, RDFS.SUBCLASSOF, Direction.OUT);
            Set<URI> X_ = new HashSet(X);
            X_.remove(d);
            Set<URI> R = new HashSet();

            for (URI d_ : X_) {
                if (!SetUtils.intersection(A_d, engine.getAncestorsInc(d_)).isEmpty()) {
                    R.add(d_);
                }
            }
            X_.removeAll(R);

            for (Set<URI> As_d : Utils.powerSet(A_d)) {
                Set<URI> X__ = new HashSet(X_);
                X__.addAll(As_d);
                if (!summaries.contains(X__) && !X__.isEmpty()) {
                    summaries.add(X__);
                    generate_summaries_inner(graph, engine, X__, summaries);
                }
            }
        }
        return summaries;
    }

    public Map<Set<URI>, Double> compute_summary_scores(String outputFilePath) throws SLIB_Exception, IOException {

        IC_Conf_Topo icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SECO_2004);

        Set<URI> union_anc_descriptors = new HashSet();
        Set<URI> union_desc_descriptors = new HashSet();

        for (URI x : descriptorsInEntries) {
            union_anc_descriptors.addAll(engine.getAncestorsInc(x));
            union_desc_descriptors.addAll(engine.getDescendantsInc(x));
        }

        // computing IC
        log("IC");
        engine.getIC_results(icConf);

        log("Computing belief");
        double max_observations_with_indirect = 0;
        for (URI u : observations_with_indirect.keySet()) {
            if (observations_with_indirect.get(u) > max_observations_with_indirect) {
                max_observations_with_indirect = observations_with_indirect.get(u);
            }
        }
        Map<URI, Double> belief = new HashMap();
        for (URI u : observations_with_indirect.keySet()) {
            belief.put(u, observations_with_indirect.get(u) / max_observations_with_indirect);

        }

        Map<URI, Double> plausibility = new HashMap();
        for (URI u : observations_4computing_plausibility.keySet()) {
            plausibility.put(u, observations_4computing_plausibility.get(u) / max_observations_with_indirect);

        }

        score_best_summary = null;
        best_summary = null;

        summary_scores = new HashMap();

        long count = 0;

        for (Set<URI> summary : summaries) {

            count++;

            log("Evaluating " + count + "/" + summaries.size());
            double score = eval_summmary(summary, descriptorsInEntries, belief, plausibility, observations, union_anc_descriptors, union_desc_descriptors, engine);
            summary_scores.put(summary, score);

            log(Utils.printSet(summary) + ": " + score);

            if (score_best_summary == null || score > score_best_summary) {
                score_best_summary = score;
                best_summary = summary;
            }
            log("[best summary]" + Utils.printSet(best_summary) + ": " + score);
        }

        //log(descriptorsInEntries + "\t" + eval_cut(descriptorsInEntries, descriptorsInEntries, observations_with_indirect, observations, engine));
        boolean outputInFile = outputFilePath != null;
        String outfile = "";
        for (Map.Entry<Set<URI>, Double> e : MapUtils.sortByValue(summary_scores).entrySet()) {

            String sval = "";

            for (URI c : e.getKey()) {
                sval += c.getLocalName() + " (" + observations_with_indirect.get(c) + ") - ";
            }

            log(e.getValue() + "\t" + sval);
            outfile += e.getValue() + "\t" + sval+"\n";
        }

        if(outputInFile){
            FileWriter fileWriter = new FileWriter(outputFilePath);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(outfile);
            printWriter.printf(outfile);
            printWriter.close();

        }
        log("best : " + Utils.printSet(best_summary) + "\t" + score_best_summary);

        return new HashMap(summary_scores);
    }

    private Set<URI> search_best_summary(String outputFilePath) throws SLIB_Exception, IOException {

        compute_summary_scores(outputFilePath);

        return best_summary;
    }

    private double eval_summmary(Set<URI> summary,
            Set<URI> descriptorsInEntries,
            Map<URI, Double> belief,
            Map<URI, Double> plausibilty,
            Map<URI, Double> observations,
            Set<URI> union_anc_descriptors,
            Set<URI> union_desc_descriptors,
            SM_Engine engine) throws SLIB_Exception {

        IC_Conf_Topo icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SECO_2004);

        log("----------------------------------------------------");
        log("Y=" + Utils.printSet(summary));
        log("----------------------------------------------------");

        Set<URI> union_anc_summary = new HashSet();
        Set<URI> union_desc_summary = new HashSet();

        for (URI y : summary) {
            union_anc_summary.addAll(engine.getAncestorsInc(y));
            union_desc_summary.addAll(engine.getDescendantsInc(y));
        }

        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
        // Computing LOSS
        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
        // - Delta  -- penalty of abstraction
        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
        // Delta E-
        double delta_e_minus = 0;

        Set<URI> descriptor_ancestors_not_covered = new HashSet(union_anc_descriptors);
        descriptor_ancestors_not_covered.removeAll(union_anc_summary);

        for (URI u : descriptor_ancestors_not_covered) {
            delta_e_minus += belief.get(u) * engine.getIC(icConf, u);
        }

        // --------------------------------------------------------------------
        // Delta P-
        // --------------------------------------------------------------------
        double delta_p_minus = 0;
        Set<URI> descriptor_descendants_not_covered = new HashSet(union_desc_descriptors);
        descriptor_descendants_not_covered.removeAll(descriptorsInEntries);
        descriptor_descendants_not_covered.removeAll(union_desc_summary);
        descriptor_descendants_not_covered.removeAll(union_anc_summary);

        log("Delta P- members: " + descriptor_descendants_not_covered);

        for (URI u : descriptor_descendants_not_covered) {
            delta_p_minus += plausibilty.get(u) * engine.getIC(icConf, u);
        }
        // --------------------------------------------------------------------
        // Delta P+
        // --------------------------------------------------------------------
        double delta_p_plus = 0;
        Set<URI> descriptor_descendants_added = new HashSet(union_desc_summary);
        descriptor_descendants_added.removeAll(union_desc_descriptors);
        descriptor_descendants_added.removeAll(union_anc_descriptors);

        log("Delta P+ members: " + descriptor_descendants_added);

        for (URI u : descriptor_descendants_added) {
            delta_p_plus += plausibilty.get(u) * engine.getIC(icConf, u);
        }
        log(delta_p_plus);

        // --------------------------------------------------------------------
        // Delta D
        // --------------------------------------------------------------------
        double delta_d = 0;

        double tau = 0;
        Set<URI> descriptors_not_covered_by_summary = new HashSet(descriptorsInEntries);
        descriptors_not_covered_by_summary.removeAll(union_desc_summary);

        for (URI u : descriptors_not_covered_by_summary) {

            tau += observations.get(u);

            Set<URI> uncovered_ancestors = new HashSet(engine.getAncestorsInc(u));
            uncovered_ancestors.removeAll(union_anc_summary);

            for (URI uu : uncovered_ancestors) {

                double rel_belief = 0;
                Set<URI> set_of_descriptors_to_consider = SetUtils.intersection(
                        SetUtils.intersection(new HashSet(descriptorsInEntries), engine.getDescendantsInc(uu)), engine.getDescendantsInc(u));

                for (URI d : set_of_descriptors_to_consider) {
                    rel_belief += observations.get(d) / total_observations;
                }

                delta_d += rel_belief * engine.getIC(icConf, uu);
            }
        }

        tau /= total_observations;

        log("tau (tmp): " + tau);
        tau = -Math.log(1 - Math.pow(tau, ALPHA_BETA_DELTA_D));
        log("tau: " + tau);
        //delta_d = tau * delta_d;
        delta_d = tau;

        double delta = p_delta_e_minus * delta_e_minus + p_delta_p_minus * delta_p_minus + p_delta_p_plus * delta_p_plus + p_delta_d * delta_d;

        // --------------------------------------------------------------------
        // - Lambda  -- Conciseness and redundancies penalties
        // --------------------------------------------------------------------
        double lambda = 0;
        for (URI u : union_anc_summary) {

            double count = 0;

            for (URI x : summary) {
                if (engine.getAncestorsInc(x).contains(u)) {
                    count += 1;
                }
            }
            lambda += (count - 1.0) * engine.getIC(icConf, u);
        }
        lambda *= EPSILON_LAMBDA;

        // --------------------------------------------------------------------
        // - Gamma  -- Additionnal constraint
        // --------------------------------------------------------------------
        double gamma = 0;

        double loss = delta + lambda + gamma;

        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
        // Computing PSI
        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
        double psi = 0;
        Set<URI> descriptor_ancestors_covered = SetUtils.intersection(union_anc_descriptors, union_anc_summary);

        // computing relative belief
        log("PSI --------------------");
        for (URI u : descriptor_ancestors_covered) {

            double rel_belief = 0;
            Set<URI> set_of_descriptors_to_consider = SetUtils.intersection(SetUtils.intersection(new HashSet(descriptorsInEntries), engine.getDescendantsInc(u)), union_desc_summary);

            for (URI d : set_of_descriptors_to_consider) {
                rel_belief += observations.get(d) / total_observations;
            }
            log(u + "rel bel: " + rel_belief + "\t" + engine.getIC(icConf, u));
            psi += rel_belief * engine.getIC(icConf, u);
        }
        log("END PSI --------------------");

        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
        double score = p_psi * psi - p_delta * loss;

        log("Psi :  " + psi);
        log("Loss :  " + loss);
        log("\t Delta :  " + delta);
        log("\t\t delta E- :  " + p_delta_e_minus + " * " + delta_e_minus);
        log("\t\t delta P+ :  " + p_delta_p_plus + " * " + delta_p_plus);
        log("\t\t delta P- :  " + p_delta_p_minus + " * " + delta_p_minus);
        log("\t\t delta D  :  " + p_delta_d + " * " + delta_d);
        log("lambda    :  " + lambda);
        log("gamma     :  " + gamma);
        log("score:  " + score);
        log("----------------------------------------------------");

        return score;
    }

    public Map<URI, Double> getObservations() {
        return observations;
    }

    public Map<URI, Double> getObservations_with_indirect() {
        return observations_with_indirect;
    }

    public Map<Set<URI>, Double> getSummary_scores() {
        return summary_scores;
    }

    public Set<Set<URI>> getSummaries() {
        return summaries;
    }

    public Set<URI> getBest_summary() {
        return best_summary;
    }

    public Double getScore_best_summary() {
        return score_best_summary;
    }

    public Set<URI> getDescriptorsInEntries() {
        return descriptorsInEntries;
    }

}
