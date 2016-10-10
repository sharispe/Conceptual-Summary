/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary;

import org.openrdf.model.URI;

/**
 *
 * @author sharispe
 */
public class Entry {

    public final double weight;
    public final URI descriptor;

    public Entry(URI d, double w) {
        descriptor = d;
        weight = w;
    }
    
    public Entry(URI d) {
        this(d,1.0);
    }
    
    @Override
    public String toString(){
        return descriptor +"w="+weight ;
    }

}
