/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary.xp;

import com.github.sharispe.cut_summary.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;

/**
 *
 * @author sharispe
 */
public class Query {

    public String id;
    public Set<URI> queryConcepts;
    public Set<Entry> queryConceptMasses;
    public Map<Set<URI>, Double> summaries;

    public Query() {
        this.id = "None";
        this.queryConceptMasses = new HashSet();
        this.queryConcepts = new HashSet<>();
        this.summaries = new HashMap<>();
    }

    public Query(String id, HashSet<Entry> queryConceptMasses, Map<Set<URI>, Double> summaries) {
        this.id = id;
        this.queryConceptMasses = queryConceptMasses;
        this.queryConcepts = new HashSet();
        for(Entry e : queryConceptMasses){
            queryConcepts.add(e.descriptor);
        }
        
        this.summaries = summaries;
    }
    
    @Override
    public String toString(){
        return "id:"+id+" "+queryConceptMasses;
    }
}
