package edu.sdsu.its.fit_welcome_tests;

import edu.sdsu.its.fit_welcome.DB;
import edu.sdsu.its.fit_welcome.Models.Event;
import edu.sdsu.its.fit_welcome.Models.Staff;
import edu.sdsu.its.fit_welcome.Models.User;
import edu.sdsu.its.fit_welcome.Quote;
import edu.sdsu.its.fit_welcome.Vault;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test Database Methods
 *
 * @author Tom Paulus
 *         Created on 3/27/16.
 */
public class TestDB {
    final static int TEST_USER_ID = 100000001;
    final static int TEST_STAFF_ID = 123456789;
    final static int TEST_ADMIN_ID = 123123123;
    final static Logger LOGGER = Logger.getLogger(TestDB.class);

    /**
     * Check if the KeyServer has access to the correct credentials
     */
    @Test
    public void checkParams() {
        final String db_url = Vault.getParam("db-url");
        LOGGER.debug("Vault.db-url = " + db_url);
        assertTrue("URL is Empty", db_url != null && db_url.length() > 0);
        assertTrue("Invalid URL", db_url.startsWith("jdbc:mysql://"));

        final String db_user = Vault.getParam("db-user");
        LOGGER.debug("Vault.db-user = " + db_user);
        assertTrue("Username is Empty", db_user != null && db_user.length() > 0);

        final String db_password = Vault.getParam("db-password");
        LOGGER.debug("Vault.db-password = " + db_password);
        assertTrue("Password is Empty", db_password != null && db_password.length() > 0);
    }


    /**
     * Test DB Connection
     */
    @Test
    public void connect() {
        Connection connection = null;
        try {
            LOGGER.debug("Attempting to connect to the DB Server");
            connection = DB.getConnection();
            LOGGER.info("DB Connection established");
            assertTrue(connection.isValid(5));
        } catch (SQLException e) {
            LOGGER.error("Problem connecting to the DB Server", e);
            fail("SQL Exception thrown while trying to connect to the DB - " + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    LOGGER.debug("DB Connection Closed");
                }
            } catch (SQLException e) {
                LOGGER.error("Problem closing the DB Connection", e);
            }
        }
    }

    /**
     * Retrieve a Users data both by their ID and their email.
     * The latter is used primarily to unsubscribe users.
     */
    @Test
    public void getUser() {
        LOGGER.info(String.format("Retrieving User Info for User with id \"%d\"", TEST_USER_ID));
        User userByID = DB.getUser(TEST_USER_ID); // Testing with WalkIn User

        assertTrue("UserByID doesn't exist", userByID != null);
        assertTrue(userByID.firstName != null);
        assertTrue(userByID.lastName != null);
        assertTrue(userByID.email != null);

        LOGGER.debug("UserByID.firstName = " + userByID.firstName);
        LOGGER.debug("UserByID.lastName = " + userByID.lastName);
        LOGGER.debug("UserByID.email = " + userByID.email);

        LOGGER.info(String.format("Retrieving User Info for User with email \"%s\"", userByID.email));
        final User userByEmail = DB.getUser(userByID.email);
        assertTrue("UserByID doesn't exist", userByEmail != null);
        assertTrue(userByEmail.firstName != null);
        assertTrue(userByEmail.lastName != null);
        assertTrue(userByEmail.email != null);

        LOGGER.debug("UserByID.firstName = " + userByEmail.firstName);
        LOGGER.debug("UserByEmail.lastName = " + userByEmail.lastName);
        LOGGER.debug("UserByEmail.email = " + userByEmail.email);

        assertTrue("Get User by Email returned a different user than Get user by ID", userByID.equals(userByEmail));
    }

    /**
     * Get a Staff Members data by their ID
     */
    @Test
    public void getStaff() {
        LOGGER.info(String.format("Retrieving Staff Info for User with id \"%d\"", TEST_STAFF_ID));
        Staff staff = DB.getStaff(TEST_STAFF_ID);

        assertTrue("Staff User doesn't exist", staff != null);
        assertTrue(staff.firstName != null);
        assertTrue(staff.lastName != null);
        assertTrue(staff.email != null);

        LOGGER.debug("Staff.firstName = " + staff.firstName);
        LOGGER.debug("Staff.lastName = " + staff.lastName);
        LOGGER.debug("Staff.email = " + staff.email);
    }

    /**
     * Get all the Staff Members, with and without a restriction in place.
     */
    @Test
    public void getAllStaff() {
        LOGGER.info("Retrieving all Staff Members");
        Staff[] allStaff = DB.getAllStaff("");

        assertTrue("Result is Empty", allStaff != null);
        LOGGER.debug("All Staff Query Returned - " + Arrays.toString(allStaff));
        assertTrue(allStaff.length >= 2); // At least 2 users are in the Test DB

        Staff[] admins = DB.getAllStaff("WHERE admin = 1");
        assertTrue("Result is Empty", admins != null);
        LOGGER.debug("All Admin Query Returned - " + Arrays.toString(admins));

        boolean testIDFound = false;
        LOGGER.info(String.format("Searching for Admin with ID \"%d\"", TEST_ADMIN_ID));
        for (Staff s : admins) {
            if (s.id == TEST_ADMIN_ID) {
                testIDFound = true;
                LOGGER.debug("Found Test ID in Query Set");
            }
        }
        assertTrue("Test Admin was not found", testIDFound);
    }

    /**
     * Create and retrieve an event.
     *
     * @throws InterruptedException
     */
    @Test
    public void events() throws InterruptedException {
        LOGGER.info("Creating new Event");
        DB.logEvent(new Timestamp(new java.util.Date().getTime()).toString(), TEST_USER_ID, "Test Action", "Params");

        TimeUnit.SECONDS.sleep(1);

        List<Event> eventList = DB.getEventsSince(0);
        assertTrue(eventList != null && eventList.size() > 0);
        LOGGER.debug("Query for Events Returned - " + Arrays.toString(eventList.toArray()));
    }

    @Test
    public void quote() {
        LOGGER.info("Retrieving Daily Quote from DB");
        final int numQuotes = DB.getNumQuotes();
        LOGGER.debug(String.format("DB contains %d quote(s).", numQuotes));
        assertTrue("No Quotes in DB", numQuotes > 0);

        Quote.QuoteModel quote = DB.getQuote(numQuotes);
        assertTrue("Quote Return is Null", quote != null);

        LOGGER.debug(String.format("Retrieved \"%s\" & Author = \"%s\"", quote.text, quote.author));
        assertTrue("Quote text is blank or null", quote.text != null && quote.text.length() > 0);
        assertTrue("Quote author is blank or null", quote.author != null && quote.author.length() > 0);


    }
}
