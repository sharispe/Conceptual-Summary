/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.Entry;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openrdf.model.URI;
import slib.graph.model.graph.G;
import slib.graph.model.repo.URIFactory;

/**
 *
 * @author sharispe
 */
public class XPThesisUtils {
    
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
}
