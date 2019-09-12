package com.markssoftware.microscheduler.quartz;

import com.markssoftware.microscheduler.rabbitmq.AmqpJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.Properties;

import static com.markssoftware.microscheduler.sync.SyncConfig.groupName;

public class SchedulerService implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(SchedulerService.class);
    private final Scheduler scheduler;

    public SchedulerService(Properties properties) {
        try {
            this.scheduler = new StdSchedulerFactory(properties).getScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public ImmutableSet<TriggerKey> allTriggers() {
        try {
            return Sets.adapt(scheduler.getTriggerKeys(GroupMatcher.anyGroup())).toImmutable();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteJob(JobKey jobKey) {
        LOGGER.info("Deleting job {}", jobKey.getName());
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void unscheduleJob(TriggerKey triggerKey) {
        try {
            scheduler.unscheduleJob(triggerKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public ImmutableSet<JobKey> allJobs() {
        try {
            return Sets.adapt(scheduler.getJobKeys(GroupMatcher.anyGroup())).toImmutable();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void createJob(String name, String source, String cron) {
        JobDetail job = JobBuilder.newJob(AmqpJob.class)
                .withIdentity(JobKey.jobKey(name, groupName))
                .withDescription(source)
                .build();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(name, groupName))
                .withDescription(source)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public Trigger getTrigger(TriggerKey triggerKey) {
        try {
            return scheduler.getTrigger(triggerKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public JobDetail getJobDetail(JobKey jobKey) {
        try {
            return scheduler.getJobDetail(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
