package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.IEEE_Summarizer;
import com.github.sharispe.cut_summary.MapUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.utils.impl.UtilDebug;

/**
 *
 * @author sharispe
 */
public class XPThesis_FitParams_KFold {

    public static class ResultConfig {

        double score;
        Map<String, Double> parameters;

        public ResultConfig(double score, Map<String, Double> parameters) {
            this.score = score;
            this.parameters = parameters;
        }

    }

    public static void logFile(String s, String logFile) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(new FileOutputStream(new File(logFile), true));
        out.print(s);
        out.close();
    }

    public static void main(String[] args) throws Exception {

        String dataFolder = "/home/sharispe/Dropbox/doc/publications/drafts/massissilia_thesis/data/";
        String taxonomy_file = dataFolder + "/taxonomie_finale.owl";
        String namespace = "http://www.semanticweb.org/mmedjkoune/taxonomie_thematique#";
        String queryFile = dataFolder + "/data.txt";
        String kfoldLog = dataFolder + "/kfold.log";

        // Loading Graph
        System.out.println("Loading Taxonomy from: " + taxonomy_file);
        System.out.println("using namespace: " + namespace);

        URIFactory factory = URIFactoryMemory.getSingleton();
        URI graphURI = factory.getURI("http://graph/");
        G taxonomy = new GraphMemory(graphURI);

        GDataConf dataConf = new GDataConf(GFormat.RDF_XML, taxonomy_file);

        // We specify an action to root the vertices, typed as class without outgoing rdfs:subclassOf relationship 
        // Those vertices are linked to owl:Thing by an eddge x  rdfs:subClassOf owl:Thing 
        GAction actionRerootConf = new GAction(GActionType.REROOTING);

        // We now create the configuration we will specify to the generic loader
        GraphConf gConf = new GraphConf();
        gConf.addGDataConf(dataConf);
        gConf.addGAction(actionRerootConf);

        GraphLoaderGeneric.load(gConf, taxonomy);
        List<Query> queries = XPThesisUtils.loadQueries(queryFile, factory, taxonomy, namespace);

        System.out.println("Queries loaded: " + queries.size());

        SM_Engine engine = new SM_Engine(taxonomy);

        int summary_number = 3;
        int k_kfold = 11; // do not change defined considering the number of query 4 queries per block
        int nbQueries_block = queries.size() / k_kfold;
        System.out.println("Number queries per block " + nbQueries_block);
        List<List<Query>> query_blocks = new ArrayList<>();

        ArrayList<Query> currentQueryBlock = new ArrayList<>();
        for (int i = 0; i < queries.size(); i++) {

            currentQueryBlock.add(queries.get(i));

            if (currentQueryBlock.size() == nbQueries_block) {
                query_blocks.add(currentQueryBlock);
                currentQueryBlock = new ArrayList();
            }
        }

        for (int i = 0; i < query_blocks.size(); i++) {

            System.out.println("Query Block " + i);
            for (Query q : query_blocks.get(i)) {
                System.out.println("\t" + q);

            }
        }
        

        System.out.println("STARTING KFOLD");
        List<ResultConfig> testing_results = new ArrayList<>();
        // Fitting parameters using KFold
        for (int current_k_kfold = 0; current_k_kfold < k_kfold; current_k_kfold++) {

            String out = "Processing KFOLD nb=" + (current_k_kfold + 1) + "\n";
            out += "Testing set = query_blocks " + current_k_kfold + "\n";
            logFile(out, kfoldLog);

            List<Query> testing_queries = query_blocks.get(current_k_kfold);
            List<Query> training_queries = new ArrayList();
            for (int j = 0; j < 10; j++) {
                if (j != current_k_kfold) {
                    training_queries.addAll(query_blocks.get(j));
                }
            }
            System.out.println("Training: ");
            for (Query q : training_queries) {
                System.out.println("\t" + q);
            }
            System.out.println("Testing: ");
            for (Query q : testing_queries) {
                System.out.println("\t" + q);
            }

            // ----------------------------------------------------------------
            // TRAINING
            // ----------------------------------------------------------------
            // Fitting parameters for the training set: 
            double p_delta_p_plus = 1.0;
            double p_delta_p_minus = 1.0;
            double EPSILON_LAMBDA = 500;
            double ALPHA_BETA_DELTA_D = 1.0;

            Map<String, Double> best_parameters_training = null;
            Double best_score_training = null;

            long it_count = 0;

            for (double p_delta_d = 500000; p_delta_d < 10000000; p_delta_d += 2000000) {

                for (double p_psi = 0; p_psi < 1000; p_psi += 100) {

                    for (double p_delta = 0; p_delta < 1000; p_delta += 100) {

                        for (double p_delta_e_minus = 0; p_delta_e_minus < 1000; p_delta_e_minus += 100) {

                            Map<String, Double> parameters = new HashMap<>();

                            // Parameter defining the weight with regard to coverage
                            parameters.put("p_psi", p_psi);
                            parameters.put("p_delta", p_delta);

                            // Parameter defining the penalty with regard to loss of exact information
                            parameters.put("p_delta_e_minus", p_delta_e_minus);

                            // Parameter defining the penalty with regard to the addition of plausible information
                            parameters.put("p_delta_p_plus", p_delta_p_plus);
                            // Parameter defining the penalty with regard to loss of plausible information
                            parameters.put("p_delta_p_minus", p_delta_p_minus);
                            // Parameter defining the penalty with regard to distortion of information
                            parameters.put("p_delta_d", p_delta_d);

                            // Parameter defining the penalty with regard to mass losses 
                            // when evaluating the distortion. 
                            // The larger the value, the more important will be the penalty wrt mass values in [1;+inf]
                            parameters.put("ALPHA_BETA_DELTA_D", ALPHA_BETA_DELTA_D);

                            // Parameter defining the penalty with regard to conciseness and redundancy
                            // the more epsilon is important, the more the summaries will tend to be abstract
                            parameters.put("EPSILON_LAMBDA", EPSILON_LAMBDA);

                            System.out.println("TESTING CONFIGURATION: " + it_count + " : " + parameters.toString());
                            it_count++;

                            ////////////////////////////////
                            double globalScore_training_current_conf = 0;

                            for (int i = 0; i < training_queries.size(); i++) {

                                IEEE_Summarizer summarizer = new IEEE_Summarizer(engine, parameters, false, true);

                                Query q = training_queries.get(i);
//                                System.out.println("Processing query " + (i + 1) + "/" + training_queries.size());
//                                System.out.println(q.id);
//                                System.out.println(q.queryConceptMasses);

                                Map<Set<URI>, Double> summary_scores = summarizer.summarize(q.queryConceptMasses);

//                                System.out.println("Query: " + q.queryConceptMasses);
                                int k_tmp = 1;
                                for (Map.Entry<Set<URI>, Double> ee : MapUtils.sortByValueDecreasing(summary_scores).entrySet()) {

                                    if (k_tmp > summary_number) {
                                        break;
                                    }

                                    Set<URI> summary = ee.getKey();
                                    double val = ee.getValue();

//                                    System.out.println("k=" + k_tmp + "/" + summary_number + "\t" + summary + "\t" + val);
                                    for (Map.Entry<Set<URI>, Double> e : q.summaries.entrySet()) {

                                        Set<URI> p_summary = e.getKey();
                                        double p_summary_score = e.getValue();

                                        //System.out.println("**** +" + p_summary_score + "\t" + p_summary);
                                        if (summary.equals(p_summary)) {
//                                            System.out.println("**** +" + p_summary_score + "\t" + p_summary);
                                            globalScore_training_current_conf += p_summary_score;

                                        }
                                        //UtilDebug.exit();
                                    }
                                    k_tmp++;
                                }
                            }

                            if (best_score_training == null || globalScore_training_current_conf > best_score_training) {
                                best_score_training = globalScore_training_current_conf;
                                best_parameters_training = parameters;
                            }

                            if (it_count % 100 == 0) {
                                out = "Tested " + it_count + "\n";
                                out += "Global Score Training: " + globalScore_training_current_conf + "\n";
                                out += "Current Best Parameters Training: " + best_parameters_training + "\n";
                                logFile(out, kfoldLog);
                            }

                            ////////////////////////////////////////////////////////
                        }
                    }
                }
            }
            out = "--------------------------------------------------------------\n";
            out += "[FINAL] Training Best Parameters KFOLD " + current_k_kfold + "\n";
            out += "Global Score Training: " + best_score_training + "\n";
            out += "Current Best Parameters Training: " + best_parameters_training + "\n";
            logFile(out, kfoldLog);

            // ----------------------------------------------------------------
            // TESTING
            // ----------------------------------------------------------------
            ////////////////////////////////
            double globalScore_testing = 0;

            for (int i = 0; i < testing_queries.size(); i++) {

                IEEE_Summarizer summarizer = new IEEE_Summarizer(engine, best_parameters_training, false, true);

                Query q = testing_queries.get(i);
                System.out.println("Processing query " + (i + 1) + "/" + testing_queries.size());
                System.out.println(q.id);
                System.out.println(q.queryConceptMasses);

                Map<Set<URI>, Double> summary_scores = summarizer.summarize(q.queryConceptMasses);

                System.out.println("Query: " + q.queryConceptMasses);

                int k_tmp = 1;
                for (Map.Entry<Set<URI>, Double> ee : MapUtils.sortByValueDecreasing(summary_scores).entrySet()) {

                    if (k_tmp > summary_number) {
                        break;
                    }

                    Set<URI> summary = ee.getKey();
                    double val = ee.getValue();

                    System.out.println("k=" + k_tmp + "/" + summary_number + "\t" + summary + "\t" + val);

                    for (Map.Entry<Set<URI>, Double> e : q.summaries.entrySet()) {

                        Set<URI> p_summary = e.getKey();
                        double p_summary_score = e.getValue();

                        //System.out.println("**** +" + p_summary_score + "\t" + p_summary);
                        if (summary.equals(p_summary)) {
                            System.out.println("**** +" + p_summary_score + "\t" + p_summary);
                            globalScore_testing += p_summary_score;

                        }
                        //UtilDebug.exit();
                    }
                    k_tmp++;
                }
            }

            out = "Parameters testing: " + best_parameters_training + "\n";
            out += "[FINAL_RESULT_TESTING] Global Score Testing KFOLD " + current_k_kfold + ": " + globalScore_testing + "\n";
            logFile(out, kfoldLog);
            testing_results.add(new ResultConfig(globalScore_testing, best_parameters_training));
            ////////////////////////////////////////////////////////
        }

        String out = "[FINAL KFOLD RESULTS]\n";
        double sum_score = 0;
        for (int i = 0; i < testing_results.size(); i++) {
            ResultConfig r = testing_results.get(i);
            out += "\tkfold=" + i + "\tscore:" + r.score + "\t" + r.parameters + "\n";
            sum_score += r.score;
        }
        out += "Average: " + (sum_score / testing_results.size()) + "\n";
        logFile(out, kfoldLog);
    }

}
