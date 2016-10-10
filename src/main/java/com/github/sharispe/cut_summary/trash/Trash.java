/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary.trash;

import java.util.HashSet;
import java.util.Set;
import org.openrdf.model.URI;
import slib.graph.model.graph.G;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.utils.SMConstants;
import slib.utils.ex.SLIB_Exception;
import slib.utils.impl.SetUtils;

/**
 *
 * @author sharispe
 */
public class Trash {
    
    @Deprecated
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
    
}
