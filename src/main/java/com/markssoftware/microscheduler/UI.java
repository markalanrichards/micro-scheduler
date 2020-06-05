package com.markssoftware.microscheduler;

import com.markssoftware.microscheduler.jobs.JobsFeature;
import com.markssoftware.microscheduler.quartz.SchedulerService;
import com.markssoftware.microscheduler.rabbitmq.AmqpClient;
import com.markssoftware.microscheduler.rabbitmq.Consuming;
import com.markssoftware.microscheduler.sync.QueueSync;
import com.markssoftware.microscheduler.sync.SimpleSync;
import com.markssoftware.microscheduler.sync.SyncConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.markssoftware.microscheduler.sync.SyncConfig.SYNC_QUEUE;

public class UI {


    private static final Logger LOGGER = LogManager.getLogger(UI.class);
    public static void main(String[] args) {
        Config config = Config.builder().build();
        try (SchedulerService schedulerService = new SchedulerService(config.getProperties())) {

            AmqpClient amqpClient = new AmqpClient(config.getRabbitmqHost());

            amqpClient.createChannel(SYNC_QUEUE);
            QueueSync queueSync = new QueueSync(amqpClient);
            JobsFeature jobsFeature = new JobsFeature(config);
            SimpleSync simpleSync = new SimpleSync(schedulerService, jobsFeature.getJobsRepository(), amqpClient);
            try (Consuming ignored = amqpClient.consume(SyncConfig.SYNC_QUEUE, name -> simpleSync.run())) {
                jobsFeature.run(queueSync);
            }

        }
        catch (Exception e) {
                LOGGER.error("Error running UI", e);
        }


    }

}
