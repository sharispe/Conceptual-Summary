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
package com.github.sharispe.cut_summary.trash;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.openrdf.model.URI;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.utils.SMConstants;
import slib.utils.ex.SLIB_Exception;
import slib.utils.impl.SetUtils;

/**
 *
 * @author Sébastien Harispe <sebastien.harispe@gmail.com>
 */
public class Mainold {

    /**
     * Compute the loss of summarizing X by C
     *
     * @param g
     * @param engine
     * @param C
     * @param X
     * @return
     */
    public static double loss(G g, SM_Engine engine, Set<URI> C, Set<URI> X) throws SLIB_Exception {

        double loss = 0;
        double epsilon = 0.000001;
        double big_penalty = 100000;
        double uncovered_penalty = big_penalty;
        IC_Conf_Topo icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SECO_2004);
        Set<URI> leaves = engine.getTaxonomicLeaves();

        System.out.println("leaves: " + leaves);
        Set<URI> coveredConcepts = new HashSet();

        for (URI c : C) {

            double ic = engine.getIC(icConf, c);
            double neg_log_ic = -Math.log(ic + epsilon);

            double loss_delta = 0;
            Set<URI> desc_c = engine.getDescendantsInc(c);
            Set<URI> coveredConcepts_c = SetUtils.intersection(desc_c, X);
            coveredConcepts.addAll(coveredConcepts_c);

            Set<URI> uselessConcepts = new HashSet(desc_c);

            for (URI u : coveredConcepts_c) {
                uselessConcepts.removeAll(engine.getDescendantsInc(u));
            }

            // compute loss delta
            if (coveredConcepts_c.isEmpty()) {
                loss_delta = big_penalty;
            } else {
                // we consider the fuzzy3E representation
                // 1 penalty for a leaf node, 2 for a non leaf node
                int leaves_penalty = SetUtils.intersection(uselessConcepts, leaves).size();
                int non_leaves_penalty = (uselessConcepts.size() - leaves_penalty) * 2;
                loss_delta = leaves_penalty + non_leaves_penalty;
            }

            double loss_c = neg_log_ic * loss_delta;

            System.out.println(c);
            System.out.println("IC\t" + ic);
            System.out.println("-log " + neg_log_ic);
            System.out.println("covered " + coveredConcepts_c);
            System.out.println("uselessConcepts " + uselessConcepts);
            System.out.println("loss delta " + loss_delta);
            System.out.println("loss c " + loss_c);

            loss += loss_c;
        }

        Set<URI> uncovered = new HashSet(X);
        uncovered.removeAll(coveredConcepts);
        System.out.println("covered: " + coveredConcepts);
        System.out.println("uncovered: " + uncovered);

        double uncovering_loss = uncovered.size() * uncovered_penalty;

        loss += uncovering_loss;

        System.out.println("LOSS: " + C + " / " + X);
        System.out.println("loss: " + loss);

        return loss;
    }

    public static void main(String[] args) throws Exception {

        String graphNT = "/data/experiments/cuts/graph.nt";

        URIFactory uriFactory = URIFactoryMemory.getSingleton();
        GDataConf data = new GDataConf(GFormat.NTRIPLES, graphNT);
        GraphConf gconf = new GraphConf(uriFactory.getURI("http://graph"));

        gconf.addGDataConf(data);

        G graph = GraphLoaderGeneric.load(gconf);

        System.out.println(graph);

        SM_Engine engine = new SM_Engine(graph);

        URI cat = uriFactory.getURI("http://ex/cat");
        URI dog = uriFactory.getURI("http://ex/dog");
        URI mammal = uriFactory.getURI("http://ex/Mammal");
        URI animal = uriFactory.getURI("http://ex/Animal");
        URI reptile = uriFactory.getURI("http://ex/Reptile");
        URI snake = uriFactory.getURI("http://ex/Snake");
        URI bigcat = uriFactory.getURI("http://ex/Bigcat");
        URI feline = uriFactory.getURI("http://ex/Feline");
        
        

        Set<URI> X = new HashSet();
        X.add(bigcat);
        X.add(snake);
        X.add(dog);

        Set<URI> C = new HashSet(Arrays.asList(feline, dog,snake));
        Set<URI> C1 = new HashSet(Arrays.asList(mammal,snake));
//        Set<URI> C2 = new HashSet(Arrays.asList(animal));
        

        System.out.println("Classes: " + engine.getClasses());
        
//        double loss = loss(graph, engine, C, X);
        loss(graph, engine, C1, X);
//        loss(graph, engine, C2, X);
        
    }
}
