package edu.sdsu.its.fit_welcome_tests;

import edu.sdsu.its.Blackboard.Models.User;
import edu.sdsu.its.Blackboard.Users;
import edu.sdsu.its.Vault;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * TODO JavaDoc
 *
 * @author Tom Paulus
 *         Created on 3/24/17.
 */
public class TestBbUsers {
    private static final Logger LOGGER = Logger.getLogger(TestBbUsers.class);

    @Before
    public void setUp() throws Exception {
        assumeTrue(Boolean.parseBoolean(Vault.getParam("syncEnable")));
    }

    @Test
    public void getAllUsers() throws Exception {
        LOGGER.info("Retrieving All Users from DB");

        User[] users = Users.getAllUsers(25);
        assertNotNull(users);
        assertTrue("No Users Returned", users.length > 0);
        LOGGER.info(String.format("Retrieved %d users from Bb", users.length));

        for (User user : users) {
            assertNotNull(user.externalId);
            assertNotNull(user.dataSourceId);
            assertNotNull(user.DSK);
            if (user.studentId == null)
                LOGGER.warn("Student ID is not defined for " + user.externalId);
            assertNotNull(user.name);
            assertNotNull("No First Name", user.name.get("given"));
            assertNotNull("No Last Name", user.name.get("family"));
            if (user.contact == null || user.contact.get("email") == null)
                LOGGER.warn("Email is not defined for " + user.externalId);
        }
    }
}
