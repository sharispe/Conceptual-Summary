package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.Entry;
import com.github.sharispe.cut_summary.IEEE_Summarizer;
import com.github.sharispe.cut_summary.MapUtils;
import java.io.BufferedReader;
import java.io.FileReader;
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
import slib.utils.impl.UtilDebug;

/**
 *
 * @author sharispe
 */
public class XPThesis {


    public static void main(String[] args) throws Exception {

        String dataFolder = "/home/sharispe/Dropbox/doc/publications/drafts/massissilia_thesis/data/";
        String taxonomy_file = dataFolder + "/taxonomie_finale.owl";
        String namespace = "http://www.semanticweb.org/mmedjkoune/taxonomie_thematique#";
        String queryFile = dataFolder + "/data.txt";

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

        Map<String, Double> parameters = new HashMap<>();

        
        //{p_psi=300.0, p_delta_p_minus=1.0, p_delta_e_minus=400.0, EPSILON_LAMBDA=500.0, ALPHA_BETA_DELTA_D=1.0, p_delta_p_plus=1.0, p_delta=100.0, p_delta_d=500000.0}

        // Parameter defining the weight with regard to coverage
        parameters.put("p_psi", 300.0);
        parameters.put("p_delta", 100.0);

        // Parameter defining the penalty with regard to loss of exact information
        parameters.put("p_delta_e_minus", 400.0);

        // Parameter defining the penalty with regard to the addition of plausible information
        parameters.put("p_delta_p_plus", 1.0);
        // Parameter defining the penalty with regard to loss of plausible information
        parameters.put("p_delta_p_minus", 1.0);
        // Parameter defining the penalty with regard to distortion of information
        parameters.put("p_delta_d", 500000.0);

        // Parameter defining the penalty with regard to mass losses 
        // when evaluating the distortion. 
        // The larger the value, the more important will be the penalty wrt mass values in [1;+inf]
        parameters.put("ALPHA_BETA_DELTA_D", 1.0);

        // Parameter defining the penalty with regard to conciseness and redundancy
        // the more epsilon is important, the more the summaries will tend to be
        // abstract
        parameters.put("EPSILON_LAMBDA", 500.0);

        double globalScore = 0;
        
        int k = 10;

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

                if(k_tmp > k) 
                    break;
                
                
                Set<URI> summary = ee.getKey();
                double val = ee.getValue();
                
                System.out.println("k="+k_tmp+"/"+k+"\t"+summary+"\t"+val);
                
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
    }

}
