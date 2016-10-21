/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
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
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;

/**
 *
 * @author sharispe
 */
public class XP_IEEE_SSCI_CIHLI_2016 {

    public static void main(String[] args) throws Exception {

        // see data directory
        String dir = System.getProperty("user.dir");
        String onto_file = dir + "/data/xp/taxo_vins.owl";
        String label_file = dir + "/data/xp/labels_taxo_vins.tsv";
        String query_file_directory = dir + "/data/xp/queries/";

        

        String file_product_ids = query_file_directory + "product_ids.csv";

        // Loading associated data
        URIFactory uriFactory = URIFactoryMemory.getSingleton();
      

        // Loading labels of URIs
        Map<String, URI> index_LabelToURI = Utils.loadOntoLabels(label_file, uriFactory);
        System.out.println("Label Index loaded, size: " + index_LabelToURI.size());

        System.out.println("Loading Ontology");
        GDataConf data = new GDataConf(GFormat.RDF_XML, onto_file);
        GraphConf gconf = new GraphConf(uriFactory.getURI("http://graph"));
        GAction rerooting = new GAction(GActionType.REROOTING);
        GAction tr = new GAction(GActionType.TRANSITIVE_REDUCTION);

        gconf.addGDataConf(data);
        gconf.addGAction(rerooting);
        gconf.addGAction(tr);

        G onto = GraphLoaderGeneric.load(gconf);
        System.out.println(onto);

        // Loading Engine
        SM_Engine engine = new SM_Engine(onto);
        IEEE_Summarizer summarizer = new IEEE_Summarizer(engine);

        List<Integer> product_ids = new ArrayList();

        BufferedReader reader = new BufferedReader(new FileReader(file_product_ids));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            product_ids.add(Integer.parseInt(line.trim()));
        }
        reader.close();

        double average_precision = 0;
        double average_recall = 0;
        double average_fmesure = 0;

        for (Integer productID : product_ids) {

            System.out.println("Processing product " + productID);

            String query_file = query_file_directory + "/concepts/concepts_" + productID + ".csv";
            Set<EntryString> entries_String = Utils.loadJudgeEntries(query_file, uriFactory);

            Set<Entry> entries = Utils.ConvertToEntry(index_LabelToURI, entries_String);

            Set<URI> given_summary = summarizer.summarize(entries);
            System.out.println("Summary: " + given_summary);

            String summary_file = query_file_directory + "/summaries/summary_" + productID + ".csv";
            entries_String = Utils.loadEntries(summary_file, uriFactory, ";");
            Set<Entry> entries_expected_summary = Utils.ConvertToEntry(index_LabelToURI, entries_String);

            Set<URI> expected_summary = Utils.getDescriptors(entries_expected_summary);

            System.out.println("Expected summary: " + expected_summary);

            double precision = Utils.precision(expected_summary, given_summary);
            double recall = Utils.precision(expected_summary, given_summary);
            double fmesure = Utils.fmesure(expected_summary, given_summary);

            average_precision += precision;
            average_recall += recall;
            average_fmesure += fmesure;

            System.out.println("expected : " + expected_summary);
            System.out.println("given    : " + given_summary);

            System.out.println("Precision " + precision);
            System.out.println("Recall " + recall);
            System.out.println("F-mesure " + fmesure);

        }

        average_precision /= product_ids.size();
        average_recall /= product_ids.size();
        average_fmesure /= product_ids.size();

        System.out.println("Average Precision " + average_precision);
        System.out.println("Average Recall " + average_recall);
        System.out.println("Average F-mesure " + average_fmesure);
    }

}
