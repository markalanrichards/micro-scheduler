package com.markssoftware.microscheduler.amqp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class AmqpJob implements Job {
    private final static Logger logger = LogManager.getLogger(AmqpJob.class);
    private final AmqpClient amqpClient;

    public AmqpJob(AmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String name = jobExecutionContext.getJobDetail().getKey().getName();
        try {
            amqpClient.send(name);
        } catch (IOException | TimeoutException e) {
            throw new JobExecutionException(e);
        }
        logger.info("Executed {}", name);
    }
}
