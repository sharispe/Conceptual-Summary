/*
 *  Copyright or © or Copr. Ecole des Mines d'Alès (2012-2014) 
 *  
 *  This software is a computer program whose purpose is to provide 
 *  several functionalities for the processing of semantic data 
 *  sources such as ontologies or text corpora.
 *  
 *  This software is governed by the CeCILL  license under French law and
 *  abiding by the rules of distribution of free software.  You can  use, 
 *  modify and/ or redistribute the software under the terms of the CeCILL
 *  license as circulated by CEA, CNRS and INRIA at the following URL
 *  "http://www.cecill.info". 
 * 
 *  As a counterpart to the access to the source code and  rights to copy,
 *  modify and redistribute granted by the license, users are provided only
 *  with a limited warranty  and the software's author,  the holder of the
 *  economic rights,  and the successive licensors  have only  limited
 *  liability. 

 *  In this respect, the user's attention is drawn to the risks associated
 *  with loading,  using,  modifying and/or developing or reproducing the
 *  software by the user in light of its specific status of free software,
 *  that may mean  that it is complicated to manipulate,  and  that  also
 *  therefore means  that it is reserved for developers  and  experienced
 *  professionals having in-depth computer knowledge. Users are therefore
 *  encouraged to load and test the software's suitability as regards their
 *  requirements in conditions enabling the security of their systems and/or 
 *  data to be ensured and,  more generally, to use and operate it in the 
 *  same conditions as regards security. 
 * 
 *  The fact that you are presently reading this means that you have had
 *  knowledge of the CeCILL license and that you accept its terms.
 */
package com.github.sharispe.cut_summary;

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
import org.openrdf.model.vocabulary.RDFS;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.graph.utils.Direction;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.utils.impl.UtilDebug;

/**
 * @author Sébastien Harispe <sebastien.harispe@gmail.com>
 */
public class Main_FAST {

    // are the descriptors considered weighted
    static boolean CONSIDER_WEIGHTED_DESCRIPTORS = true;
    static double WEIGHTED_IMPORTANCE = 1;
    // weight associated to the cardinality
    static double ALPHA_GAMMA = 100;
    static double BETA_GAMMA = 0;
    static double WEIGHT_B_COVERED = 1;
    static double WEIGHT_P = 0;

    public static void main(String[] args) throws Exception {

        String onto = "/data/experiments/cuts/taxo_vins.owl";
        String label_file = "/data/experiments/cuts/labels_taxo_vins.tsv";
        String query_file = "/data/experiments/cuts/query1.tsv";

        URIFactory uriFactory = URIFactoryMemory.getSingleton();

        // Loading labels
        Map<String, URI> labelIndex = loadOntoLabels(label_file, uriFactory);
        System.out.println("Label Index loaded, size: " + labelIndex.size());

        System.out.println("Considering weighted descriptors: " + CONSIDER_WEIGHTED_DESCRIPTORS);

        GDataConf data = new GDataConf(GFormat.RDF_XML, onto);
        GraphConf gconf = new GraphConf(uriFactory.getURI("http://graph"));
        GAction reroot = new GAction(GActionType.REROOTING);
        GAction tr = new GAction(GActionType.TRANSITIVE_REDUCTION);

        gconf.addGDataConf(data);
        gconf.addGAction(reroot);
        gconf.addGAction(tr);

        G graph = GraphLoaderGeneric.load(gconf);

        System.out.println(graph);

        SM_Engine engine = new SM_Engine(graph);

        // MX massed X
        Set<EntryString> MX_String = new HashSet();
//        MX_String.add(new EntryString("tobacco", 10));
//        MX_String.add(new EntryString("roasted", 10));

        MX_String = loadQuery(query_file, uriFactory);

        Set<Entry> MX = convertEntryString(labelIndex, MX_String);

        Set<URI> X = new HashSet();
        Map<URI, Double> M = new HashMap();
        for (Entry e : MX) {
            X.add(e.descriptor);
            M.put(e.descriptor, e.weight);
        }

        System.out.println(X);
        double sum_mass = 0;
        System.out.println("Masses ------------------");
        for (Map.Entry<URI, Double> entrySet : M.entrySet()) {
            System.out.println(entrySet.getKey() + "\t" + entrySet.getValue());
            sum_mass += entrySet.getValue();
        }
        System.out.println("-------------------------");

        // Compute propagated mass for each concept
        Map<URI, Double> MP = new HashMap();
        for (URI x : engine.getClasses()) {
            double mp = 0;
            for (URI y : engine.getDescendantsInc(x)) {
                mp += M.containsKey(y) ? M.get(y) : 0;
            }
            MP.put(x, mp);
        }

        System.out.println("Masses Propagated ------------------");
        for (Map.Entry<URI, Double> entrySet : MP.entrySet()) {
            System.out.println(entrySet.getKey() + "\t" + entrySet.getValue());
        }
        System.out.println("-------------------------");
        System.out.println("Mass sum: " + sum_mass);
        System.out.println("-------------------------");

        for (int threshold = 10; threshold <= 100; threshold += 10) {

            double massThreshold = sum_mass * threshold / 100;
            System.out.println("threshold: " + threshold + " % ");
            System.out.println("mass >= " + massThreshold);

            URI root = engine.getRoot();

            List<URI> queue = new ArrayList();
            Set<URI> visited = new HashSet();
            Set<URI> summary = new HashSet();

            queue.add(root);
            visited.add(root);
            while (!queue.isEmpty()) {

                URI c = queue.remove(0);

                boolean childAdded = false;
                for (URI cc : graph.getV(c, RDFS.SUBCLASSOF, Direction.IN)) {

                    visited.add(cc);

                    if (MP.get(cc) > massThreshold) {
                        queue.add(cc);
                        childAdded = true;
                    }
                }

                if (!childAdded) { // restriction regarding masses is respected
                    summary.add(c);
                }

            }

            System.out.println("Summary (" + threshold + "): " + summary);

        }

        UtilDebug.exit();

    }

    private static Map<String, URI> loadOntoLabels(String label_file, URIFactory uriFactory) throws Exception {

        Map<String, URI> index = new HashMap();

        try (BufferedReader br = new BufferedReader(new FileReader(label_file))) {
            String line = br.readLine();

            while (line != null) {

                String[] data = line.split("\t");

                if (data.length != 2) {
                    System.out.println("[Warning] excluding " + line + "\t" + Arrays.toString(data));
                } else {
                    String label = data[1].toLowerCase().trim();

                    if (index.containsKey(label) && !index.get(label).stringValue().equals(data[0])) {
                        System.out.println("[Warning] Duplicate entry for label '" + label + "' in the label index");
                        System.out.println(index.get(label) + "\t and " + data[0] + "\tregistred");
                        UtilDebug.exit();
                    }
                    index.put(label, uriFactory.getURI(data[0]));
                }

                line = br.readLine();
            }
        }
        return index;
    }

    private static Set<EntryString> loadQuery(String query_file, URIFactory uriFactory) throws Exception {

        Set<EntryString> entries = new HashSet();

        try (BufferedReader br = new BufferedReader(new FileReader(query_file))) {
            String line = br.readLine();

            while (line != null) {

                String[] data = line.split("\t");

                if (data.length != 2) {
                    System.out.println("[Warning] excluding " + line + "\t" + Arrays.toString(data));
                } else {
                    entries.add(new EntryString(data[0].trim(), Double.parseDouble(data[1])));
                }

                line = br.readLine();
            }
        }
        return entries;
    }

    private static Set<Entry> convertEntryString(Map<String, URI> labelIndex, Set<EntryString> MX_String) {

        Set<Entry> entries = new HashSet();

        for (EntryString e : MX_String) {

            if (!labelIndex.containsKey(e.descriptor.toLowerCase())) {
                System.out.println("[ERROR] cannot locate concept for descriptor: '" + e.descriptor + "'");
                System.out.println("Existing mappings");
                for (String s : labelIndex.keySet()) {
                    System.out.println("'" + s + "'\t" + labelIndex.get(s));
                }
                UtilDebug.exit();
            }

            entries.add(new Entry(labelIndex.get(e.descriptor), e.weight));
        }
        return entries;

    }

    private static String printCut(Set<URI> X) {
        String s = "";
        for (URI x : X) {
            s += x.getLocalName() + " - ";
        }
        return s;
    }

}
