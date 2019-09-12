package com.markssoftware.microscheduler;

import com.markssoftware.microscheduler.amqp.AmqpClient;
import com.markssoftware.microscheduler.amqp.AmqpJob;
import com.markssoftware.microscheduler.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

public class Worker {
    private static final Logger LOGGER = LogManager.getLogger(Worker.class);

    public static void main(String[] args) {
        try {
            Config config = Config.builder().build();
            Properties o = config.getProperties();
            AmqpClient amqpClient = new AmqpClient(config.getRabbitmqHost());
            final Scheduler scheduler = new StdSchedulerFactory(o).getScheduler();
            scheduler.setJobFactory((triggerFiredBundle, scheduler1) -> new AmqpJob(amqpClient));
            scheduler.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    scheduler.shutdown();
                } catch (SchedulerException e) {
                    LOGGER.warn("Unexpected shutdown exception", e);
                }
            })
            );
            Thread.currentThread().join();

        } catch (SchedulerException | InterruptedException se) {
            LOGGER.warn("Unexpected exception", se);
        }
    }

}
