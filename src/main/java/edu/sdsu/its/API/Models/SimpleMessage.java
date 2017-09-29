package edu.sdsu.its.API.Models;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * @author Tom Paulus
 *         Created on 6/26/17.
 */
public class SimpleMessage {
    @Expose String status;
    @Expose String message;

    public SimpleMessage(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public SimpleMessage(String message) {
        this.message = message;
    }

    public String asJson() {
        return new Gson().toJson(this);
    }
}