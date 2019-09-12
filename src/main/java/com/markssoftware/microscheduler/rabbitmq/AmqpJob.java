package com.markssoftware.microscheduler.rabbitmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;


public class AmqpJob implements Job {
    private final static Logger logger = LogManager.getLogger(AmqpJob.class);
    private final AmqpClient amqpClient;

    public AmqpJob(AmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        String name = jobExecutionContext.getJobDetail().getKey().getName();
        amqpClient.send(name);
        logger.info("Executed {}", name);
    }
}
