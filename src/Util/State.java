/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 * @author muhammad
 */
public enum State {

    ACTIVE("Downloading"),
    PAUSED("Paused"),
    CMPLTD("Completed"),
    FAILED("Failed"),
    SHDLED("Scheduled");

    String value;

    State(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
