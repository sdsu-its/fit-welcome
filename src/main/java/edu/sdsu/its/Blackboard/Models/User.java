package edu.sdsu.its.Blackboard.Models;

import java.util.Map;

/**
 * TODO JavaDoc
 *
 * @author Tom Paulus
 *         Created on 1/27/17.
 */
public class User {
    public String studentId;
    public String userName;
    public String externalId;
    public String dataSourceId;
    public DataSource DSK;

    public Map<String, String> availability;
    public Map<String, String> name;
    public Map<String, String> job;
    public Map<String, String> contact;
}
