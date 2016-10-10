/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.sharispe.cut_summary;

/**
 *
 * @author sharispe
 */
public class EntryString {

    public double weight = 1;
    public String descriptor = null;

    public EntryString(String d) {
        descriptor = d;
    }

    public EntryString(String d, double w) {
        this(d);
        weight = w;
    }

}
