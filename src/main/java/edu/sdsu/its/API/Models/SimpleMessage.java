package edu.sdsu.its.API.Models;

import com.google.gson.annotations.Expose;

/**
 * @author Tom Paulus
 *         Created on 6/26/17.
 */
public class SimpleMessage {
    @Expose String message;

    public SimpleMessage(String message) {
        this.message = message;
    }
}