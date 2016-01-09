package edu.sdsu.its.fit_welcome.followup;

import edu.sdsu.its.fit_welcome.followup.Models.Event;
import edu.sdsu.its.fit_welcome.followup.Models.User;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface with the MySQL DB
 *
 * @author Tom Paulus
 *         Created on 12/15/15.
 */
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class DB {
    private static final String db_url = Param.getParam("fit_welcome", "db-url");
    private static final String db_user = Param.getParam("fit_welcome", "db-user");
    private static final String db_password = Param.getParam("fit_welcome", "db-password");
    private static final Logger Log = Logger.getLogger(DB.class);

    /**
     * Create and return a new DB Connection
     * Don't forget to close the connection!
     *
     * @return {@link Connection} DB Connection
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(db_url, db_user, db_password);
        } catch (Exception e) {
            Log.fatal("Problem Initializing DB Connection", e);
            System.exit(69);
        }

        return conn;
    }

    private static String sanitize(final String input) {
        return input.replace("'", "");
    }

    private static void executeStatement(final String sql) {
        new Thread() {
            @Override
            public void run() {
                Statement statement = null;
                Connection connection = getConnection();

                try {
                    statement = connection.createStatement();
                    Log.info(String.format("Executing SQL Statement - \"%s\"", sql));
                    statement.execute(sql);

                } catch (SQLException e) {
                    Log.error("Problem Executing Statement \"" + sql + "\"", e);
                } finally {
                    if (statement != null) {
                        try {
                            statement.close();
                            connection.close();
                        } catch (SQLException e) {
                            Log.warn("Problem Closing Statement", e);
                        }
                    }
                }
            }
        }.start();
    }

    /**
     * Get User for the specified ID
     *
     * @param id {@link int} User's ID (Commonly their RedID)
     * @return {@link User} User
     */
    public static User getUser(final int id) {
        Connection connection = getConnection();
        Statement statement = null;
        User user = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM bbusers WHERE id = " + id + ";";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                user = new User(id, resultSet.getString("first_name"), resultSet.getString("last_name"), resultSet.getString("email"), resultSet.getBoolean("send_emails"));
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for UserID", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return user;
    }


    /**
     * Export All events that happened within maxAge from Now
     *
     * @param maxAge {@link int} Maximum Age of the Events to fetch
     * @return {@link Event[]} All events that fall between the specified time range
     */
    public static Event[] exportEvents(final int maxAge) {
        final String sql = "SELECT *\n" +
                "FROM events\n" +
                "WHERE TIMESTAMP BETWEEN DATE_SUB(now(), INTERVAL " + maxAge + " DAY) AND NOW()\n" +
                "ORDER BY TIMESTAMP ASC;";

        Connection connection = getConnection();
        Statement statement = null;

        Event[] rarray = null;

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            List<Event> eventList = new ArrayList<>();

            while (resultSet.next()) {
                eventList.add(new Event(resultSet.getInt("ID"), resultSet.getInt("redid"), new DateTime(resultSet.getTimestamp("TIMESTAMP"))));
            }

            rarray = new Event[eventList.size()];
            for (int e = 0; e < eventList.size(); e++) {
                rarray[e] = eventList.get(e);
            }

        } catch (SQLException e) {
            Log.error("Problem retrieving Event entries from DB", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return rarray;
    }

    /**
     * Check when the User was last Emailed
     *
     * @param userID {@link int} User's ID
     * @return {@link Duration} Time since last email
     */
    public static Duration lastEmailed(final int userID) {
        final String sql = "SELECT MAX(TIMESTAMP) FROM email WHERE ID=" + userID + " AND TYPE = '" + Main.Email_Name + "' ;";

        Connection connection = getConnection();
        Statement statement = null;
        DateTime last = new DateTime(0); // The beginning of Joda Time

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                Timestamp ts = resultSet.getTimestamp(1);
                if (ts != null) last = new DateTime(ts);
            }

        } catch (SQLException e) {
            Log.error("Problem retrieving Email entries from DB");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return new Duration(last, DateTime.now());

    }

    /**
     * Log emails that are sent
     *
     * @param recipientID {@link int} Recipient ID
     * @param type        {@link String} Type of Email Sent
     */

    public static void logEmail(final int recipientID, final String type) {
        final String sql = "INSERT INTO email (TIMESTAMP, ID, TYPE) VALUES (NOW(), " + recipientID + ", '" + sanitize(type) + "');";
        executeStatement(sql);
    }
}
