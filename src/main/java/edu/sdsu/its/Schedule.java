package edu.sdsu.its;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * TODO JavaDoc
 *
 * @author Tom Paulus
 *         Created on 2/3/17.
 */
public class Schedule {
    private static Scheduler scheduler = null;
    private static final Logger LOGGER = Logger.getLogger(Schedule.class);

    public static Scheduler getScheduler() {
        try {
            if (scheduler == null) {
                scheduler = StdSchedulerFactory.getDefaultScheduler();
            }
        } catch (SchedulerException e) {
            LOGGER.fatal("Problem Initializing Scheduler", e);
        }

        return scheduler;
    }
}
