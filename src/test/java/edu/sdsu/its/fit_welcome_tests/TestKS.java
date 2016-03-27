package edu.sdsu.its.fit_welcome_tests;

import edu.sdsu.its.fit_welcome.Param;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test Key Server Environment Variable configuration and Test Connection to Server.
 *
 * @author Tom Paulus
 *         Created on 3/27/16.
 */
public class TestKS {
    final static Logger LOGGER = Logger.getLogger(TestKS.class);

    /**
     * Check that the environment variables that are used by the Key Server are set.
     */
    @Test
    public void checkENV() {
        final String path = System.getenv("KSPATH");
        final String key = System.getenv("KSKEY");
        final String name = System.getenv("WELCOME_APP");

        LOGGER.debug("ENV.KSPATH =" + path);
        LOGGER.debug("ENV.KSKEY =" + key);
        LOGGER.debug("ENV.WELCOME_APP =" + name);

        assertTrue("Empty KS URL", path != null && path.length() > 0);
        assertTrue("Empty KS API Key", key != null && key.length() > 0);
        assertTrue("Empty App Name", name != null && name.length() > 0);
    }

    /**
     * Perform a self-test of the connection to the server.
     * Validity of the app-name and api-key are NOT checked.
     */
    @Test
    public void checkConnection() {
        assertTrue(Param.testConnection());
    }
}
