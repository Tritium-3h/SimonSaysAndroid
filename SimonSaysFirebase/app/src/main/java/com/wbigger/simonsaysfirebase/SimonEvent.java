package com.wbigger.simonsaysfirebase;

/**
 * Created by claudio on 3/21/17 for Droidcon Turin
 *
 * Do not remove public access modifier, public is requested by Firebase
 */

public class SimonEvent {

    // Do not remove public access modifier, public is requested by Firebase
    public String color;
    public String counter;

    public SimonEvent(String color, String counter) {
        this.color = color;
        this.counter = counter;
    }

    public SimonEvent(String color, Long counter) {
        this.color = color;
        this.counter = ""+counter;
    }
}
