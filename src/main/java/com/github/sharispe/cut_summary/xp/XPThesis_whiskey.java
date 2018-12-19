package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.Entry;
import com.github.sharispe.cut_summary.IEEE_Summarizer;
import com.github.sharispe.cut_summary.MapUtils;
import static com.github.sharispe.cut_summary.xp.LoaderDataWhiskey.loadData;
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
import slib.utils.impl.SetUtils;
import slib.utils.impl.UtilDebug;

/**
 *
 * @author sharispe
 */
public class XPThesis_whiskey {

    public static void main(String[] args) throws Exception {

        String dataFolder = "/home/sharispe/Dropbox/doc/publications/drafts/massissilia_thesis/data/";
        String taxonomy_file = dataFolder + "/taxo_whiskies.owl";
        String namespace = "http://www.semanticweb.org/mmedjkoune/taxonomie_thematique#";
        String annotFile = dataFolder + "/data_whiskies_annot.csv";
        String queryFile = dataFolder + "/data_whiskies_queries.csv";

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
        Set<LoaderDataWhiskey.EntryWhiskey> whiskey_annot = loadData(annotFile, URIFactoryMemory.getSingleton());
        Set<LoaderDataWhiskey.EntryWhiskey> queries = loadData(queryFile, URIFactoryMemory.getSingleton());

        System.out.println("Queries loaded: " + queries.size());

        SM_Engine engine = new SM_Engine(taxonomy);

        // check queries and whiskey
        for (LoaderDataWhiskey.EntryWhiskey e : whiskey_annot) {
            for (URI u : e.annots.values()) {
                if (!engine.getClasses().contains(u)) {
                    throw new Exception("Error Annots: cannot identify URI " + u + " in provided taxonomy");
                }
            }
        }
        for (LoaderDataWhiskey.EntryWhiskey e : queries) {
            for (URI u : e.annots.values()) {
                if (!engine.getClasses().contains(u)) {
                    throw new Exception("Error QUERIES: cannot identify URI " + u + " in provided taxonomy");
                }
            }
        }

        Map<String, Double> parameters = new HashMap<>();

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
        parameters.put("EPSILON_LAMBDA", 150.0);

        String output = "";

        for (LoaderDataWhiskey.EntryWhiskey q : queries) {

            System.out.println("Processing query: " + q);

            IEEE_Summarizer summarizer = new IEEE_Summarizer(engine, parameters, false, false);

            System.out.println(q.entryName);
            System.out.println(q.annots);

            // convert 
            Set<Entry> entries = new HashSet<>();
            for (URI u : q.annots.values()) {
                entries.add(new Entry(u, 2));
            }

            Map<Set<URI>, Double> summary_scores = summarizer.summarize(entries,null);

            String best_summary = null;

            for (Map.Entry<Set<URI>, Double> ee : MapUtils.sortByValueDecreasing(summary_scores).entrySet()) {

                Set<URI> summary = ee.getKey();
                double val = ee.getValue();

//                if (best_summary == null) {
                for (LoaderDataWhiskey.EntryWhiskey w : whiskey_annot) {

                    if (setEquals(new HashSet(w.annots.values()), summary)) {
                        System.out.println(q.entryName+"\tfound "+w.entryName+"\tscore="+val);
                        if (best_summary == null) {
                            best_summary = w.entryName;
                        }
                    }
//                    }
                }
            }
            output += q.entryName + "=" + best_summary + "\n";
        }
        System.out.println(output);
    }

    public static boolean setEquals(Set<URI> a, Set<URI> b) {

        if (a.size() != b.size()) {
            return false;
        }
        return a.containsAll(b);
    }

}
