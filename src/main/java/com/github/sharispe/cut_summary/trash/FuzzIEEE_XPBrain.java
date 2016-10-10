package com.github.sharispe.cut_summary.trash;

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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import slib.graph.algo.extraction.rvf.AncestorEngine;
import slib.graph.algo.extraction.rvf.DescendantEngine;
import slib.graph.algo.extraction.rvf.RVF_DAG;
import slib.graph.model.graph.G;
import slib.graph.model.graph.utils.Direction;
import slib.graph.model.repo.URIFactory;
import slib.utils.ex.SLIB_Ex_Critic;
import slib.utils.impl.SetUtils;

/**
 *
 * @author Sébastien Harispe <sebastien.harispe@gmail.com>
 */
public class FuzzIEEE_XPBrain {

    String file_concept_representation;
    URIFactory factory;
    G wordNet;
    Map<URI, Set<URI>> conceptRepresentations = new HashMap();
    Map<URI, String> labels;
    // a concept is represented by all the leaves it subsumes a specific leaf 
    // concept has been added to each nodes who have only descendant. This is to 
    // ensure that two concepts do not have the same concept representations
    Map<URI, Set<URI>> ancestorIndex;
    Map<URI, Set<URI>> descendantIndex;
    Set<URI> leaves;
//    Set<URI> fakeLeaves; // leaves that are added for computational purpose

    // Metrics 
    Map<URI, Double> masses;
    Map<URI, Double> belief;
    Map<URI, Double> plausibility;
    Map<URI, Double> pignisticProbabilities;
    Map<URI, Double> q_quantity;
    
    int mass_factor;

    public FuzzIEEE_XPBrain(URIFactory factory, G wordNet, Map<URI, String> labels, String file_concept_representation, boolean buildConceptRepresentation, int mass_factor) {

        this.factory = factory;
        this.wordNet = wordNet;
        this.labels = labels;
        this.file_concept_representation = file_concept_representation;
        this.mass_factor = mass_factor;

        try {
//            fakeLeaves = Utils.addRequiredLeaves(factory, wordNet, labels); // Add required leaves
            buildIndexes();
            loadConceptRepresentations(buildConceptRepresentation);
            // metrics
            computeMasses();
            computeBelief();
            computePlausibility();
            computePignisticProbabilities();
            computeQ();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occured: " + e.getMessage());
        }
    }

    private void loadConceptRepresentations(boolean build) throws Exception {

        System.out.println("Generating index file: " + build);
        
        if (build) {

            try (PrintWriter writer = new PrintWriter(file_concept_representation, "UTF-8")) {
                System.out.println("Computing concept representations for " + wordNet.getV().size() + " concepts");
                System.out.println("Write into: " + file_concept_representation);
                System.out.println("This may take times...");

                int i = 0;
                int size = wordNet.getV().size();

                for (URI u : wordNet.getV()) {
                    i++;

                    if (i % 1000 == 0) {
                        double p = (double) i * 100.0 / (double) size;
                        System.out.println("\t" + i + "/" + size + "\t" + p + "%");
                    }
                    Set<URI> repr = descendantIndex.get(u).size() == 1 ? new HashSet(descendantIndex.get(u)) : SetUtils.intersection(leaves, descendantIndex.get(u));

                    StringBuilder out = new StringBuilder(u.stringValue()).append('\t');
                    int c = 0;
                    for (URI r : repr) {
                        if (c != 0) {
                            out.append(';');
                        }
                        out.append(r.stringValue());
                        c++;
                    }
                    writer.println(out.toString());
                }
            }
            System.out.println("Representations have been flushed into: " + file_concept_representation);
        }
        System.out.println("Loading concept representations from: " + file_concept_representation);
        conceptRepresentations = new HashMap();
        try (BufferedReader br = new BufferedReader(new FileReader(file_concept_representation))) {

            String line;
            do {
                line = br.readLine();
                if (line != null) {

                    String[] data = line.split("\t");

                    if (data.length == 2) {
                        URI c = factory.getURI(data[0]);
                        String[] data2 = data[1].split(";");

                        Set<URI> c_repr = new HashSet();
                        for (String s : data2) {
                            c_repr.add(factory.getURI(s));
                        }
                        conceptRepresentations.put(c, c_repr);
                    }
                }
            } while (line != null);
        }
        System.out.println("Number of representations loaded " + conceptRepresentations.size());
    }

    private void computeQ() {
        System.out.println("Computing Q quantity");
        q_quantity = new HashMap();

        // Computing q quantity
        for (URI u : wordNet.getV()) {
            double q_quant = 0;
            for (URI c : ancestorIndex.get(u)) {
                q_quant += getMass(c);
            }
            q_quantity.put(u, q_quant);
        }
    }

    private void buildIndexes() throws SLIB_Ex_Critic {
        
        System.out.println("Building indexes");
        // Building ancestors and descendants indexes
        RVF_DAG ancestorAccessor = new AncestorEngine(wordNet);
        RVF_DAG descendantAccessor = new DescendantEngine(wordNet);

        ancestorIndex   = ancestorAccessor.getAllRV();
        descendantIndex = descendantAccessor.getAllRV();

        for (URI u : ancestorIndex.keySet()) {
            ancestorIndex.get(u).add(u);
        }
        for (URI u : descendantIndex.keySet()) {
            descendantIndex.get(u).add(u);
        }
        
        // We just check the correctness of the index
        for(URI u : ancestorIndex.keySet()){
            
            if(ancestorIndex.get(u).size()== 1 && !u.equals(OWL.THING)){
                
                System.out.println("Relationships: "+wordNet.getE(u, Direction.BOTH).size());
//                for(E e : wordNet.getE(u, Direction.BOTH)){
//                    System.out.println(e);
//                }
//                
                System.out.println("Warning with URI: "+u+"\t"+labels.get(u)+" it must be a root");
                //throw new SLIB_Ex_Critic("WTF");
            }
        }
        
        System.out.println("Building leaves index");

        // building leaves indexes
        leaves = new HashSet();
        for (URI u : wordNet.getV()) {
            if (wordNet.getE(RDFS.SUBCLASSOF, u, Direction.IN).isEmpty()) {
                leaves.add(u);
            }
        }

        System.out.println("Leaves: " + leaves.size());

    }

    private void computeMasses() throws SLIB_Ex_Critic {

        System.out.println("Computing masses");

        masses = new HashMap(wordNet.getV().size());
        int sum_occ = 0;
        
        Map<URI, Integer> nbOcc = new HashMap(masses.size()); // randomly generated now but this will be changed
        for (URI u : wordNet.getV()) {
//            if (fakeLeaves.contains(u)) {
//                continue;
//            }
            int nbOccTmp = wordNet.getE(RDF.TYPE, u, Direction.IN).size() * mass_factor;
            nbOccTmp += 1.0; 
//            System.out.println("occ: "+u+" ("+labels.get(u)+") "+nbOccTmp);
            nbOcc.put(u, nbOccTmp);
            sum_occ += nbOccTmp;
        }
        double sum_mass = 0.0;
        
        for (URI u : nbOcc.keySet()) {
            double mass = (double) nbOcc.get(u) / (double) sum_occ;
            masses.put(u, mass);
//            System.out.println("mass"+u + "--> " + mass);
            sum_mass += mass;
        }

        double error_masses = sum_mass - 1.0;

        System.out.println("Sum of masses: " + sum_mass);
        System.out.println("Error masses : " + error_masses);
        if ((error_masses > -0.01 && error_masses > 0.01)) {
            throw new SLIB_Ex_Critic("Error sum of masses not equal to 1: " + sum_mass + "...");
        }
        
        System.out.println("Checking mass values");
        
        for(Entry<URI,Double> e : masses.entrySet()){
            
            double mass = e.getValue();
            
            // in our setting the mass cannot be set to 0
            if(mass == Double.NaN || mass == Double.NEGATIVE_INFINITY || mass == Double.POSITIVE_INFINITY || mass == 0 ){
                throw new SLIB_Ex_Critic("Error computing masses: mass of "+e.getKey()+" has been set to "+mass+"...");
            }
        }
    }

    private void computeBelief() throws SLIB_Ex_Critic {
        // Belief computation
        System.out.println("Computing Belief: ");

        belief = new HashMap();
        for (URI u : wordNet.getV()) {

            double belief_u = 0;
            for (URI d : descendantIndex.get(u)) {
                belief_u += getMass(d);
            }
            if(belief_u == Double.NaN || belief_u == Double.NEGATIVE_INFINITY || belief_u == Double.POSITIVE_INFINITY || belief_u == 0 ){
                throw new SLIB_Ex_Critic("Error computing belief: belief "+u+" has been set to "+belief_u+"...");
            }
            belief.put(u, belief_u);
        }
    }

    private void computePlausibility() {

        plausibility = new HashMap();
        System.out.println("Computing Plausibility:");

        for (URI u : wordNet.getV()) {

            // subsumed Leaves
            Set<URI> leavesUnderU = SetUtils.intersection(descendantIndex.get(u), leaves);
            // ancestors subsumed leaves
            Set<URI> ancestorsSubsumedLeaves = new HashSet();
            for (URI l : leavesUnderU) {
                ancestorsSubsumedLeaves.addAll(ancestorIndex.get(l));
            }

//            System.out.println(labels.get(u)+ "\t"+u);
//            System.out.println("Ancestors subsumed leaves: "+ancestorsSubsumedLeaves.size());
            double plausibility_u = 0;
            for (URI d : ancestorsSubsumedLeaves) {
//                System.out.println("\t"+labels.get(d)+"\t"+getMass(d));
                plausibility_u += getMass(d);
            }
//            System.out.println("plausibility: "+plausibility_u);
            plausibility.put(u, plausibility_u);
        }
    }

    private void computePignisticProbabilities() {
        System.out.println("Computing pignistic probability");
        pignisticProbabilities = new HashMap();

        // Computing pignitic probabilities for leaves
        for (URI u : leaves) {

//            System.out.println(labels.get(u)+"\t"+ancestorIndex.get(u).size());
            double pignistic_p = 0;
            for (URI a : ancestorIndex.get(u)) {
//                System.out.println("\t"+labels.get(a)+"\t"+getMass(a));
                pignistic_p += (double) getMass(a) / (double) conceptRepresentations.get(a).size();
//                System.out.println("\t"+pignistic_p);
            }
            pignisticProbabilities.put(u, pignistic_p);
        }

        // Computing pignitic probabilities for the other concepts 
        for (URI u : wordNet.getV()) {
            if (!leaves.contains(u)) {
                // leaves have already been processed

                double pignistic_p = 0;
                for (URI c : conceptRepresentations.get(u)) {
                    pignistic_p += pignisticProbabilities.get(c);
                }
                pignisticProbabilities.put(u, pignistic_p);
            }
        }
    }

    private double getMass(URI u) {
        return masses.containsKey(u) ? masses.get(u) : 0.0;
    }

    public Map<URI, Double> getBelief() {
        return belief;
    }

    public Map<URI, Double> getPlausibility() {
        return plausibility;
    }

    public Map<URI, Double> getPignisticProbabilities() {
        return pignisticProbabilities;
    }

    public Map<URI, Double> getQ_quantity() {
        return q_quantity;
    }

    public Double getBelief(URI u) {
        return belief.get(u);
    }
    
    public Double getIC_Belief(URI u) {
        return - Math.log(belief.get(u));
    }

    public Double getPlausibility(URI u) {
        return plausibility.get(u);
    }

    public Double getIC_Plausibility(URI u) {
        return - Math.log(plausibility.get(u));
    }

    public Double getPignisticProbabilities(URI u) {
        return pignisticProbabilities.get(u);
    }
    
    public Double getIC_PignisticProbabilities(URI u) {
        return - Math.log(pignisticProbabilities.get(u));
    }

    public Double getQ_quantity(URI u) {
        return q_quantity.get(u);
    }
    
    public Double getIC_Q_quantity(URI u) {
        return - Math.log(q_quantity.get(u));
    }

    Set<URI> getLeaves() {
        return leaves;
    }

}
