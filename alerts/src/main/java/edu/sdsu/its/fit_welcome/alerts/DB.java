package edu.sdsu.its.fit_welcome.alerts;

import edu.sdsu.its.fit_welcome.alerts.Models.Staff;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
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
     * Create and return a DB Connection
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
     * Get all Staff Users. Restrictions can be imposed with the restrictions param.
     * Use restrictions with care as they are un sanitized and not checked.
     *
     * @param restriction {@link String} Restriction of which users should be included. Uses SQL format
     *                    ex. "WHERE admin = 1"
     *                    Use empty string to get all staff users.
     * @return {@link Staff[]} All staff users who meet the supplied criteria.
     */
    public static Staff[] getAllStaff(final String restriction) {
        Connection connection = getConnection();
        Statement statement = null;
        Staff[] staff = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM staff " + restriction + ";";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            final List<Staff> staffList = new ArrayList<>();


            while (resultSet.next()) {
                staffList.add(new Staff(resultSet.getInt("id"), resultSet.getString("first_name"), resultSet.getString("last_name"),
                        resultSet.getString("email")));
            }

            Collections.sort(staffList, (staff1, staff2) -> staff1.lastName.compareToIgnoreCase(staff2.lastName));


            staff = new Staff[staffList.size()];

            for (int s = 0; s < staffList.size(); s++) {
                staff[s] = staffList.get(s);
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for Staff List", e);
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

        return staff;
    }


    /**
     * @param id {@link int} Staff ID
     * @return True if Clocked IN, False if Clocked OUT
     */
    public static boolean clockStatus(final int id) {
        Connection connection = getConnection();
        Statement statement = null;
        boolean status = false;

        try {
            statement = connection.createStatement();
            final String sql = String.format("SELECT * FROM clock WHERE id = %d AND time_out = '0000-00-00 00:00:00' AND time_in >= CURDATE();", id);
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            status = resultSet.next();

        } catch (SQLException e) {
            Log.error("Problem Adding Action to DB", e);
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

        return status;
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
