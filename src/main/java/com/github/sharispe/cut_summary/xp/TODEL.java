/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary.xp;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sharispe
 */
public class TODEL {

    public static void main(String[] args) {

        long it_count = 0;
        for (double p_psi = 0; p_psi < 1000; p_psi+=10) {
            for (double p_delta = 0; p_delta < 1000; p_delta+=10) {
                for (double p_delta_e_minus = 0; p_delta_e_minus < 1000; p_delta_e_minus+=10) {
                    for (double p_delta_p_plus = 0; p_delta_p_plus < 1000; p_delta_p_plus+=100) {
                        for (double p_delta_p_minus = 0; p_delta_p_minus < 1000; p_delta_p_minus+=100) {
                            for (double p_delta_d = 0; p_delta_d < 10000000; p_delta_d+=1000000) {
                                for (double ALPHA_BETA_DELTA_D = 0; ALPHA_BETA_DELTA_D < 10000000; ALPHA_BETA_DELTA_D+=1000000) {

                                    // Parameter defining the penalty with regard to conciseness and redundancy
                                    // the more epsilon is important, the more the summaries will tend to be abstract
                                    for (double EPSILON_LAMBDA = 0; EPSILON_LAMBDA < 1000; EPSILON_LAMBDA += 5) {

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

                                        System.out.println(it_count+" : "+parameters.toString());
                                        it_count++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
