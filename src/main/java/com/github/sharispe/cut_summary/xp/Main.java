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
package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.*;
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
 * @author Sébastien Harispe <sebastien.harispe@gmail.com>
 */
public class Main {




    public static void main(String[] args) throws Exception {
        
        if(args.length != 4){
            System.out.println("Usage: ");
            System.out.println("[1] Taxonomy OWL ");
            System.out.println("[2] file specifying concept's label  (TSV format: URI [tab] label)");
            System.out.println("[3] file specifying the query to be summarized (line format: label [TAB] frequency)");
            System.out.println("[4] output file)");
            System.exit(0);
        }
        
        
        String onto_file  = args[0];
        String label_file = args[1];
        String query_file = args[2];
        String output_file = args[3];
        
        System.out.println("onto: "+onto_file);
        System.out.println("label file: "+label_file);
        System.out.println("query file: "+query_file);
        System.out.println("output file: "+output_file);

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
        
        System.out.println("Classes: ");
        for(URI u : engine.getClasses()){
            System.out.println("\t"+u);
        }

        Set<EntryString> entries_String = Utils.loadEntries(query_file, uriFactory);
        // add entries manually entries_String.add(new EntryString("tobacco", 10));

        Set<Entry> entries = Utils.ConvertToEntry(index_LabelToURI, entries_String);
        
        IEEE_Summarizer summarizer = new IEEE_Summarizer(engine);

        Map<Set<URI>, Double> summary = summarizer.summarize(entries, output_file);
        System.out.println("Summary: "+summary);
    }

    
}
