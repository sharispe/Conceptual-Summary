/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary.trash;

import com.github.sharispe.cut_summary.Entry;
import com.github.sharispe.cut_summary.Main_IEEE;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.utils.GraphActionExecutor;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.graph.utils.Direction;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.utils.impl.UtilDebug;

/**
 *
 * @author sharispe
 */
public class Main_Todel {
    
    public static void main(String[] args) throws Exception {

//        String onto = "/data/experiments/cuts/graph.nt";
//
//        URIFactory uriFactory = URIFactoryMemory.getSingleton();
//
//
//        GDataConf data = new GDataConf(GFormat.NTRIPLES, onto);
//        GraphConf gconf = new GraphConf(uriFactory.getURI("http://graph"));
//        GAction tr = new GAction(GActionType.TRANSITIVE_REDUCTION);
//
//        gconf.addGDataConf(data);
//        gconf.addGAction(tr);
//
//        G graph = GraphLoaderGeneric.load(gconf);
//
//        System.out.println(graph);
//
//        SM_Engine engine = new SM_Engine(graph);
//        
//        URI dog = uriFactory.getURI("http://ex/dog");
//        URI snake = uriFactory.getURI("http://ex/snake");
//        URI cat = uriFactory.getURI("http://ex/cat");
//
//        // MX massed X
//        Set<Entry> MX = new HashSet();
//        MX.add(new Entry(dog, 4));
//        MX.add(new Entry(snake, 4));
//        MX.add(new Entry(cat, 4));
//
//        Set<URI> X = new HashSet();
//        Map<URI, Double> M = new HashMap();
//        for (Entry e : MX) {
//            X.add(e.descriptor);
//            M.put(e.descriptor, e.weight);
//        }
//
//        System.out.println(X);
//        System.out.println("Masses ------------------");
//        for (Map.Entry<URI, Double> entrySet : M.entrySet()) {
//            System.out.println(entrySet.getKey() + "\t" + entrySet.getValue());
//        }
//        System.out.println("-------------------------");
//
//        System.out.println("Generate Ancestor restriction");
//
//        // Compute propagated mass for each concept
//        Map<URI, Double> MP = new HashMap();
//        for (URI x : engine.getClasses()) {
//            double mp = 0;
//            for (URI y : engine.getDescendantsInc(x)) {
//                mp += M.containsKey(y) ? M.get(y) : 0;
//            }
//            MP.put(x, mp);
//        }
//
//        System.out.println("Masses Propagated ------------------");
//        for (Map.Entry<URI, Double> entrySet : MP.entrySet()) {
//            System.out.println(entrySet.getKey() + "\t" + entrySet.getValue());
//        }
//        System.out.println("-------------------------");
//
//        System.out.println("Generate Ancestor restriction");
//
//        Set<URI> addInfoConcepts = new HashSet(); // all the concepts adding information
//        
//        
//        for (URI x : engine.getClasses()) {
//            
//            if (MP.get(x) != 0) {
//                
//                if (engine.getDescendantsInc(x).size() == 1) { // leaf
//                    addInfoConcepts.add(x);
//                    System.out.println("\tKEEP LEAF "+x);
//                } else {
//                    // check if the propagated mass associated to the concept
//                    // is more important than the propagated mass of each 
//                    // of its children
//                    double mp = MP.get(x);
//                    double max_mp_c = 0;
//                    System.out.println("EVALUATING "+x + "\t" + MP.get(x));
//                    
//                    for (URI c : graph.getV(x, RDFS.SUBCLASSOF, Direction.IN)) {
//                        System.out.println("\t" + c + "\t" + MP.get(c));
//                        if (MP.get(c) > max_mp_c) {
//                            max_mp_c = MP.get(c);
//                        }
//                    }
//                    if (mp > max_mp_c) {
//                        addInfoConcepts.add(x);
//                        System.out.println("KEEP "+x);
//                    }
//                    else{
//                        System.out.println("Remove "+x+"(mp: "+mp+") "+"(max_mp_c: "+max_mp_c+")");
//                    }
//                }
//            }
//            else{
//                System.out.println("Remove "+x);
//            }
//        }
//        
//        // Modify the set of ancestors only considering the informative concepts
//        Map<URI,Set<URI>> ancestors_reduced = new HashMap();
//        long init_link = 0;
//        long reduced_link = 0;
//        for (URI x : engine.getClasses()) {
//            Set<URI> ancs_info = new HashSet();
//            init_link += engine.getAncestorsInc(x).size();
//            for(URI a : engine.getAncestorsInc(x)){
//                if(addInfoConcepts.contains(a)){
//                    ancs_info.add(a);
//                    reduced_link++;
//                }
//            }
//            ancestors_reduced.put(x, ancs_info);
//        }
//        System.out.println("init link: "+init_link);
//        System.out.println("reduced link: "+reduced_link);
//        
//        System.out.println("Ancestor reduced:");
//        for(URI x : addInfoConcepts){
//            System.out.println(x);
//        }
//        System.out.println("---------------------");
//        
//        
//        
//        G reduced_graph = new GraphMemory(null);
//        for(URI x : ancestors_reduced.keySet()){
//            for(URI a : ancestors_reduced.get(x)){
//                reduced_graph.addE(x, RDFS.SUBCLASSOF, a);
//            }
//        }
//        // apply transitive reduction
//        GAction tr2 = new GAction(GActionType.TRANSITIVE_REDUCTION);
//        GraphActionExecutor.applyAction(tr2, reduced_graph);
//        System.out.println("reduced graph: "+reduced_graph);
//        SM_Engine engine_reducedGraph = new SM_Engine(reduced_graph);
//        
//
//        Set<Set<URI>> cuts_X = Main_IEEE.generate_cuts(reduced_graph, engine_reducedGraph, X);
//        
//        System.out.println("Summaries");
//        for(Set<URI> c : cuts_X){
//            System.out.println(c);
//        }
//        UtilDebug.exit();
//
//        Main_IEEE.search_best_cut(X, M, MP, cuts_X, engine_reducedGraph);
//
//        UtilDebug.exit();

    }

    
}
