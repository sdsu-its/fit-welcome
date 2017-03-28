package edu.sdsu.its.Blackboard.Models;

import java.util.Map;

/**
 * Models a User for the Blackboard Learn API.
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

    public String toString() {
        return "User{" +
                "studentId='" + studentId + '\'' +
                ", userName='" + userName + '\'' +
                ", externalId='" + externalId + '\'' +
                ", DSK=" + DSK +
                ", availability=" + availability +
                ", name=" + name +
                ", job=" + job +
                ", contact=" + contact +
                '}';
    }
}
