package edu.sdsu.its.fit_welcome_tests;

import edu.sdsu.its.Welcome.Clock;
import edu.sdsu.its.API.Models.Staff;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static edu.sdsu.its.fit_welcome_tests.TestDB.TEST_STAFF_ID;
import static org.junit.Assert.assertTrue;

/**
 * Test Clock - Used by Hourly Staff to track their hours
 *
 * @author Tom Paulus
 *         Created on 3/27/16.
 */
public class TestClock {
    private static final Logger LOGGER = Logger.getLogger(TestClock.class);

    /**
     * Test is a user can clock in and out.
     *
     * @throws InterruptedException
     */
    @Test
    public void clock() throws InterruptedException {
        LOGGER.info(String.format("Testing Clock for User with ID \"%d\"", TEST_STAFF_ID));
        Clock clock = new Clock(Staff.getStaff(TEST_STAFF_ID));

        LOGGER.debug("User is currently " + (clock.getStatus() ? "Clocked In" : "Clocked Out"));
        boolean bStatus = clock.getStatus();

        clock.toggle();
        TimeUnit.SECONDS.sleep(1); // Clock Toggles are run in the background and take a little to execute.
        assertTrue("Problem toggling Clock Status", clock.getStatus() == !bStatus);
        LOGGER.debug("User is currently " + (clock.getStatus() ? "Clocked In" : "Clocked Out"));

        clock.toggle();
        TimeUnit.SECONDS.sleep(1); // Clock Toggles are run in the background and take a little to execute.
        assertTrue("Problem toggling Clock Status", clock.getStatus() == bStatus);
        LOGGER.debug("User is currently " + (clock.getStatus() ? "Clocked In" : "Clocked Out"));
    }
}
