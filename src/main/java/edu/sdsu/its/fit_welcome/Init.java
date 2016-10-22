package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.Staff;
import org.apache.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * Initialize and Teardown the WebApp and DB
 *
 * @author Tom Paulus
 *         Created on 10/21/2016.
 */
@WebListener
public class Init implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(Init.class);
    private static final int DEFAULT_ID = 550046348;
    private static final String DEFAULT_FIRST_NAME = "Administrator";
    private static final String DEFAULT_LAST_NAME = "User";
    private static final String DEFAULT_EMAIL = "";


    /**
     * Initialize the Webapp with the Default User if no users exist.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Staff[] users = DB.getAllStaff("");
        LOGGER.info(String.format("Starting Webapp. Found %d staff in DB", users.length));
        if (users.length == 0) {
            LOGGER.info("No users were found in the DB. Creating default User.");
            Staff staff = new Staff(DEFAULT_ID, DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME, DEFAULT_EMAIL, false, true, false);
            // int id, String firstName, String lastName, String email, boolean clockable, boolean admin, boolean instructional_designer
            DB.createNewStaff(staff);

            LOGGER.info(String.format("Initial Staff Created. ID: \"%d\"", DEFAULT_ID));
        }
    }

    /**
     * Deregister DB Driver to prevent memory leaks.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // Loop through all drivers
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                // This driver was registered by the webapp's ClassLoader, so deregister it:
                try {
                    LOGGER.info(String.format("Deregistering JDBC driver: %s", driver));
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ex) {
                    LOGGER.fatal(String.format("Error deregistering JDBC driver: %s", driver), ex);
                }
            } else {
                // driver was not registered by the webapp's ClassLoader and may be in use elsewhere
                LOGGER.info(String.format("Not deregistering JDBC driver %s as it does not belong to this webapp's ClassLoader", driver));
            }
        }
    }
}