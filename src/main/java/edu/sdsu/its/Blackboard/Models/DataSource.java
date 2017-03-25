package edu.sdsu.its.Blackboard.Models;

/**
 * Models a DataSource for the Blackboard Learn API.
 *
 * @author Tom Paulus
 *         Created on 2/10/17.
 */
public class DataSource {
    public String id;
    public String externalId;

    public DataSource(String externalId) {
        this.externalId = externalId;
    }
}
