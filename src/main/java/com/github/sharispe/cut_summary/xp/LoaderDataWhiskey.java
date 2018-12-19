/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary.xp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;

/**
 *
 * @author sharispe
 */
public class LoaderDataWhiskey {

    public static class EntryWhiskey {

        public String entryName;
        public Map<String, URI> annots;

        public EntryWhiskey(String name) {
            this.entryName = name;
            this.annots = new HashMap();
        }

        public EntryWhiskey(String name, Map<String, URI> annots) {
            this.entryName = name;
            this.annots = annots;
        }

        @Override
        public String toString() {
            return entryName + " : " + annots;
        }

    }

    public static Set<EntryWhiskey> loadData(String filePath, URIFactory f) throws Exception {

        Set<EntryWhiskey> data = new HashSet();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            EntryWhiskey currentEntry = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    System.out.println(line);
                    String currentEntryName = line.substring(1);
                    currentEntry = new EntryWhiskey(currentEntryName);
                    System.out.println("Loading entry: " + currentEntryName);
                    data.add(currentEntry);
                } else if (line.length() > 2) {

                    if (!line.substring(0, 2).equals("//")) {

                        
                        String[] dataSplit = line.split(";");
                        
                        System.out.println(Arrays.toString(dataSplit));
                        if (dataSplit.length == 2) {
                            
                            System.out.println(line);
                            URI u = f.getURI(dataSplit[1].trim());
                            String n = dataSplit[0].trim();
                            currentEntry.annots.put(n, u);
                        }
                    }
                }
            }
        }
        return data;
    }

    public static void main(String[] a) throws Exception {
        String fpathWhiskeyAnnots = "/home/sharispe/Dropbox/doc/publications/drafts/massissilia_thesis/data/data_whiskies_annot.csv";
        String fpathWhiskeyQueries = "/home/sharispe/Dropbox/doc/publications/drafts/massissilia_thesis/data/data_whiskies_queries.csv";
        Set<EntryWhiskey> data = loadData(fpathWhiskeyAnnots, URIFactoryMemory.getSingleton());
        Set<EntryWhiskey> queries = loadData(fpathWhiskeyQueries, URIFactoryMemory.getSingleton());
        for(EntryWhiskey e : queries){
            System.out.println(e);
        }
    }

}
