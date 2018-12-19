package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.Entry;
import com.github.sharispe.cut_summary.IEEE_Summarizer;
import com.github.sharispe.cut_summary.MapUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

/**
 *
 * @author sharispe
 */
public class XPThesis_FitParams {

    public static List<Query> loadQueries(String queryFile, URIFactory factory, G onto, String namespace) throws Exception {
        List<Query> queries = new ArrayList();
        Query currentQuery = null;
        boolean loadQuestion = false;

        try (BufferedReader br = new BufferedReader(new FileReader(queryFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("processing: " + line);

                line = line.trim();

                if (line.charAt(0) == '#') {
                    // new Query - save old query / create new one
                    System.out.println("Starting new query definition");
                    currentQuery = new Query();
                    queries.add(currentQuery);
                    loadQuestion = true;
                } else if (loadQuestion) { // loading question
                    String[] data = line.split(":");
                    currentQuery.id = data[0];

                    System.out.println("-- Loading question: " + data[0]);
                    String[] data2 = data[1].split(",");
                    for (String cm : data2) {
                        String[] data3 = cm.split("/");
                        String conceptName = data3[0].trim();
                        Double conceptMass = Double.parseDouble(data3[1]);

                        System.out.println(conceptName + " : " + conceptMass);

                        URI conceptURI = factory.getURI(namespace + conceptName);

                        if (!onto.containsVertex(conceptURI)) {
                            throw new Exception("Error: Cannot find concept " + conceptURI + " in the taxonomy");
                        }
                        currentQuery.queryConceptMasses.add(new Entry(conceptURI, conceptMass));
                        currentQuery.queryConcepts.add(conceptURI);
                    }
                    loadQuestion = false;
                } else { // loading summary
                    String[] data = line.split(":");
                    System.out.println("summary data " + Arrays.toString(data));

                    if (data.length != 2) {
                        throw new Exception("Invalid summary definition: '" + line + "' parsed as: '" + Arrays.toString(data) + "'");
                    }

                    Double score = Double.parseDouble(data[1]);
                    String[] data2 = data[0].split(",");

                    System.out.println("Loading summary " + Arrays.toString(data2) + " score: " + score);
                    Set<URI> summary = new HashSet();
                    for (String conceptName : data2) {
                        conceptName = conceptName.trim();
                        URI conceptURI = factory.getURI(namespace + conceptName);

                        if (!onto.containsVertex(conceptURI)) {
                            throw new Exception("Error: Cannot find concept " + conceptURI + " in the taxonomy");
                        }

                        summary.add(conceptURI);
                    }
                    currentQuery.summaries.put(summary, score);
                }

            }
        }
        return queries;
    }

    public static void main(String[] args) throws Exception {

        String dataFolder = "/home/sharispe/Dropbox/doc/publications/drafts/massissilia_thesis/data/";
        String taxonomy_file = dataFolder + "/taxonomie_finale.owl";
        String namespace = "http://www.semanticweb.org/mmedjkoune/taxonomie_thematique#";
        String queryFile = dataFolder + "/data.txt";
        String bestResultLogFilePath = dataFolder + "/best_results.log";

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
        List<Query> queries = loadQueries(queryFile, factory, taxonomy, namespace);

        System.out.println("Queries loaded: " + queries.size());

        SM_Engine engine = new SM_Engine(taxonomy);

        int k = 3;

        RQueue<String, Double> bestResults = new RQueue(1000, true);

        long it_count = 0;

        for (double p_delta_p_plus = 1.0; p_delta_p_plus < 1000; p_delta_p_plus += 200) {

            for (double p_delta_p_minus = 1.0; p_delta_p_minus < 1000; p_delta_p_minus += 200) {

                for (double EPSILON_LAMBDA = 100; EPSILON_LAMBDA < 1000; EPSILON_LAMBDA += 200) {

                    for (double p_delta_d = 500000; p_delta_d < 10000000; p_delta_d += 2000000) {
                        for (double ALPHA_BETA_DELTA_D = 1.0; ALPHA_BETA_DELTA_D < 10000000; ALPHA_BETA_DELTA_D += 2000000) {

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
                                        double globalScore = 0;

                                        for (int i = 0; i < queries.size(); i++) {

                                            IEEE_Summarizer summarizer = new IEEE_Summarizer(engine, parameters, false, false);

                                            Query q = queries.get(i);
                                            System.out.println("Processing query " + (i + 1) + "/" + queries.size());
                                            System.out.println(q.id);
                                            System.out.println(q.queryConceptMasses);

                                            Map<Set<URI>, Double> summary_scores = summarizer.summarize(q.queryConceptMasses,null);

                                            System.out.println("Query: " + q.queryConceptMasses);

                                            int k_tmp = 1;
                                            for (Map.Entry<Set<URI>, Double> ee : MapUtils.sortByValueDecreasing(summary_scores).entrySet()) {

                                                if (k_tmp > k) {
                                                    break;
                                                }

                                                Set<URI> summary = ee.getKey();
                                                double val = ee.getValue();

                                                System.out.println("k=" + k_tmp + "/" + k + "\t" + summary + "\t" + val);

                                                for (Map.Entry<Set<URI>, Double> e : q.summaries.entrySet()) {

                                                    Set<URI> p_summary = e.getKey();
                                                    double p_summary_score = e.getValue();

                                                    //System.out.println("**** +" + p_summary_score + "\t" + p_summary);
                                                    if (summary.equals(p_summary)) {
                                                        System.out.println("**** +" + p_summary_score + "\t" + p_summary);
                                                        globalScore += p_summary_score;

                                                    }
                                                    //UtilDebug.exit();
                                                }
                                                k_tmp++;
                                            }
                                        }
                                        System.out.println("Global Score: " + globalScore);
                                        bestResults.add(parameters.toString(), globalScore);

                                        if (it_count % 100 == 0) {
                                            System.out.println("Best Parameters");
                                            System.out.println(bestResults.toString());
                                            try (PrintWriter out = new PrintWriter(bestResultLogFilePath)) {
                                                out.println("Tested configuration: " + it_count);
                                                out.println(bestResults.toString());
                                            }
                                        }
                                        ////////////////////////////////////////////////////////
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("[FINAL]Best Parameters");
        System.out.println(bestResults.toString());
    }

}
