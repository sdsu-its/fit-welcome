package edu.sdsu.its.fit_welcome_tests;

import edu.sdsu.its.Blackboard.Auth;
import edu.sdsu.its.Vault;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Authenticate with Application Key and Secret to retrieve Token from the Learn Server
 *
 * @author Tom Paulus
 *         Created on 1/27/17.
 */
public class TestBbAuth {
    private static final Logger LOGGER = Logger.getLogger(TestBbAuth.class);

    @Before
    public void setUp() throws Exception {
        assumeTrue(Boolean.parseBoolean(Vault.getParam("syncEnable")));
    }

    @Test
    public void TestToken() {
        String token = Auth.getToken();
        assertNotNull("Unable to retrieve token", token);
        assertFalse("Empty Token", token.isEmpty());
        LOGGER.info("Token Obtained from Learn Server");
        LOGGER.debug("Token: " + token);
    }
}
