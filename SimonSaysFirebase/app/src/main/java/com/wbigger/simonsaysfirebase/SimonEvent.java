package com.wbigger.simonsaysfirebase;

/**
 * Created by claudio on 3/21/17 for Droidcon Turin
 */

public class SimonEvent {

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
