package com.markssoftware.microscheduler;

import com.markssoftware.microscheduler.jobs.JobsFeature;
import com.markssoftware.microscheduler.quartz.SchedulerService;
import com.markssoftware.microscheduler.rabbitmq.AmqpClient;
import com.markssoftware.microscheduler.rabbitmq.Consuming;
import com.markssoftware.microscheduler.sync.QueueSync;
import com.markssoftware.microscheduler.sync.SimpleSync;
import com.markssoftware.microscheduler.sync.SyncConfig;

public class UI {


    public static void main(String[] args) {
        Config config = Config.builder().build();
        try (SchedulerService schedulerService = new SchedulerService(config.getProperties())) {
            AmqpClient amqpClient = new AmqpClient(config.getRabbitmqHost());
            QueueSync queueSync = new QueueSync(amqpClient);
            JobsFeature jobsFeature = new JobsFeature(config);
            SimpleSync simpleSync = new SimpleSync(schedulerService, jobsFeature.getJobsRepository(), amqpClient);
            try (Consuming ignored = amqpClient.consume(SyncConfig.SYNC_QUEUE, name -> simpleSync.run())) {
                jobsFeature.run(queueSync);
            }

        }


    }

}
