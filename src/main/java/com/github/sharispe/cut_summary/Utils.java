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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import slib.graph.model.graph.G;
import slib.graph.model.graph.elements.E;
import slib.graph.model.graph.utils.Direction;
import slib.graph.model.impl.graph.elements.Edge;
import slib.graph.model.repo.URIFactory;
import slib.utils.impl.SetUtils;
import slib.utils.impl.UtilDebug;

/**
 *
 * @author Sébastien Harispe <sebastien.harispe@gmail.com>
 */
public class Utils {

    public static Map<String, URI> loadOntoLabels(String label_file, URIFactory uriFactory) throws Exception {

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

    /**
     * Format id_1\t2 id_2\t5 ...
     *
     * @param query_file
     * @param uriFactory
     * @return
     * @throws Exception
     */
    public static Set<EntryString> loadEntries(String query_file, URIFactory uriFactory) throws Exception {

        return loadEntries(query_file, uriFactory, "\t");
    }

    public static Set<EntryString> loadEntries(String query_file, URIFactory uriFactory, String separator){

        System.out.println("Load entries from: " + query_file);

        Set<EntryString> entries = new HashSet();

        try (BufferedReader br = new BufferedReader(new FileReader(query_file))) {
            String line = br.readLine();

            while (line != null) {
                
                System.out.println(line);

                String[] data = line.split(separator);
                System.out.println(Arrays.toString(data));

                if (data.length != 2) {
                    System.out.println("[Warning] excluding " + line + "\t" + Arrays.toString(data));
                } else {
                    entries.add(new EntryString(data[0].trim(), Double.parseDouble(data[1])));
                }

                line = br.readLine();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("entries loaded");
        return entries;
    }

    /**
     * Format id_1\t2 id_2\t5 ...
     *
     * @param query_file
     * @param uriFactory
     * @return
     * @throws Exception
     */
    public static Set<EntryString> loadJudgeEntries(String query_file, URIFactory uriFactory) throws Exception {

        System.out.println("Loading judge entries from: " + query_file);

        Set<EntryString> entries = new HashSet();

        try (BufferedReader br = new BufferedReader(new FileReader(query_file))) {
            String line = br.readLine();

            boolean header = true;

            while (line != null) {

                if (header) {
                    header = false;
                    continue;
                }

                String[] data = line.split(";");

                if (data.length != 3) {
                    System.out.println("[Warning] excluding " + line + "\t" + Arrays.toString(data) + " length: " + data.length);
                } else {
                    entries.add(new EntryString(data[2].trim(), 1));
                }
                line = br.readLine();
            }
        }
        return entries;
    }

    public static Set<Entry> ConvertToEntry(Map<String, URI> labelIndex, Set<EntryString> MX_String) {

        Map<URI, Double> observations = new HashMap();

        for (EntryString e : MX_String) {

            if (!labelIndex.containsKey(e.descriptor.toLowerCase())) {
                System.out.println("[ERROR] cannot locate concept for descriptor: '" + e.descriptor + "'");
                System.out.println("Existing mappings");
                for (String s : labelIndex.keySet()) {
                    System.out.println("'" + s + "'\t" + labelIndex.get(s));
                }
                //UtilDebug.exit();
                continue;
            }
            URI d = labelIndex.get(e.descriptor);

            if (!observations.containsKey(d)) {
                observations.put(d, 0.0);
            }
            observations.put(d, observations.get(d) + e.weight);
        }

        Set<Entry> entries = new HashSet();
        for (URI u : observations.keySet()) {
            entries.add(new Entry(u, observations.get(u)));
        }
        return entries;

    }

    public static Set<Entry> removeDuplicate(Set<Entry> entries) {

        Map<URI, Double> observations = new HashMap();

        for (Entry e : entries) {
            if (!observations.containsKey(e.descriptor)) {
                observations.put(e.descriptor, e.weight);
            } else {
                observations.put(e.descriptor, observations.get(e.descriptor) + e.weight);
            }
        }

        Set<Entry> entries_noduplicate = new HashSet();
        for (URI u : observations.keySet()) {
            entries_noduplicate.add(new Entry(u, observations.get(u)));
        }
        return entries_noduplicate;

    }

    public static String printSet(Set<URI> X) {
        String s = "";
        for (URI x : X) {
            s += x.getLocalName() + " - ";
        }
        return s;
    }

    public static Set<URI> addRequiredLeaves(URIFactory factory, G wordnet, Map<URI, String> labels) {

        Set newLeaves = new HashSet();
        // For each node that is not a leaf
        // and that do not have more than one children, 
        // we add a topic children
        Set newEdges = new HashSet();
        for (URI u : wordnet.getV()) {

            Set<E> childrens = wordnet.getE(RDFS.SUBCLASSOF, u, Direction.IN);
            Set<E> fathers = wordnet.getE(RDFS.SUBCLASSOF, u, Direction.OUT);
//            System.out.println(u+"\t"+nbChildren);
            if (childrens.size() == 1 && fathers.size() == 1) {

                String label = labels.get(u);
//                System.out.println("Adding Topic node to " + u + "\t" + label);
//                URI uriChild = childrens.iterator().next().getSource();
//                URI uriFather = fathers.iterator().next().getTarget();
//                System.out.println("\tFather: " + labels.get(uriFather));
//                System.out.println("\t *    : " + label);
//                System.out.println("\tChild : " + labels.get(uriChild));

                URI topicURI = factory.getURI(u.toString() + "__TOPIC");
                newLeaves.add(topicURI);
                newEdges.add(new Edge(topicURI, RDFS.SUBCLASSOF, u));
                labels.put(topicURI, labels.get(u) + " [TOPIC]");
            }
        }
        System.out.println("New edges: " + newEdges.size());
        wordnet.addE(newEdges);
        return newLeaves;

    }

    static double[] extractColumn(int i, double[][] scoreMatrix) {

        double[] col = new double[scoreMatrix[i].length];
        for (int j = 0; j < scoreMatrix[i].length; j++) {
            col[j] = scoreMatrix[i][j];
        }
        return col;
    }

    // adapted from from http://stackoverflow.com/questions/4640034/calculating-all-of-the-subsets-of-a-set-of-numbers
    public static Set<Set<URI>> powerSet(Set<URI> originalSet) {

        System.out.println("Computing power set of " + originalSet);

        Set<Set<URI>> sets = new HashSet();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<URI>());
            return sets;
        }
        List<URI> list = new ArrayList(originalSet);
        URI head = list.get(0);
        Set<URI> rest = new HashSet(list.subList(1, list.size()));
        for (Set<URI> set : powerSet(rest)) {
            Set<URI> newSet = new HashSet();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        System.out.println("pws done " + originalSet);
        return sets;
    }

    public static double precision(Set<URI> relevant, Set<URI> retrieved) {
        return (double) SetUtils.intersection(relevant, retrieved).size() / (double) retrieved.size();
    }

    public static double recall(Set<URI> relevant, Set<URI> retrieved) {
        return (double) SetUtils.intersection(relevant, retrieved).size() / (double) relevant.size();
    }

    public static double fmesure(Set<URI> relevant, Set<URI> retrieved) {
        double precision = precision(relevant, retrieved);
        double recall = recall(relevant, retrieved);
        
        if(precision + recall == 0) return 0;

        double fmesure = 2.0 * (precision * recall) / (precision + recall);
        return fmesure;
    }

    public static Set<URI> getDescriptors(Collection<Entry> entries) {
        Set<URI> descriptors = new HashSet();
        for (Entry u : entries) {
            descriptors.add(u.descriptor);
        }
        return descriptors;
    }

}
