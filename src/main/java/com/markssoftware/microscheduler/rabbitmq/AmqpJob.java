package com.markssoftware.microscheduler.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.jdbc.jmx.LoadBalanceConnectionGroupManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


public class AmqpJob implements Job {
    private final static Logger logger = LogManager.getLogger(AmqpJob.class);
    private final AmqpClient amqpClient;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public AmqpJob(AmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Date scheduledFireTime = jobExecutionContext.getScheduledFireTime();
        Date fireTime = jobExecutionContext.getFireTime();
        String offsetDateFireTime = dateToUtcOffsetDateTimeString(fireTime);
        String offsetDateScheduledFireTime = dateToUtcOffsetDateTimeString(scheduledFireTime);

        String name = jobDetail.getKey().getName();
        final String message;
        try {
            message = OBJECT_MAPPER.writeValueAsString(OBJECT_MAPPER.createObjectNode()
                    .put("jobKey", name)
                    .put("fireTime", offsetDateFireTime)
                    .put("scheduledFireTime", offsetDateScheduledFireTime)
            );
        } catch (JsonProcessingException e) {
            logger.warn("JSON message failure", e);
            throw new RuntimeException(e);
        }
        amqpClient.send(name, message);
        logger.info("Executed {}", name);
    }

    private String dateToUtcOffsetDateTimeString(Date fireTime) {
        return ZonedDateTime.ofInstant(fireTime.toInstant(), ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
