/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

/**
 * @author muhammad
 */
public enum Color {

    BLUE("3498db"), GREEN("3fb19b"), ORANGE("f39c12"), RED("e74c3c"), WHITE("f9f9f9");
    private final String value;

    private Color(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
