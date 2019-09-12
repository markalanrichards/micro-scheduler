package com.markssoftware.microscheduler.sync;

import com.markssoftware.microscheduler.amqp.AmqpClient;
import com.markssoftware.microscheduler.amqp.AmqpJob;
import com.markssoftware.microscheduler.jobs.JobInfo;
import com.markssoftware.microscheduler.jooq.JobsTable;
import lombok.Builder;
import lombok.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.partition.set.PartitionImmutableSet;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jooq.DSLContext;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class SimpleSync implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(SimpleSync.class);
    private static final String groupName = "GROUP_NAME";
    private static final String SYNC_QUEUE = "sync";
    private final Scheduler scheduler;
    private final JobsTable jobsTable;
    private final DSLContext dslContext;
    private final AmqpClient amqpClient;
    private final AmqpClient.Consuming syncing;


    public SimpleSync(Scheduler scheduler, JobsTable jobsTable, DSLContext dslContext, AmqpClient amqpClient) throws IOException, TimeoutException {
        this.scheduler = scheduler;
        this.jobsTable = jobsTable;
        this.dslContext = dslContext;
        this.amqpClient = amqpClient;
        amqpClient.createChannel(SYNC_QUEUE);
        syncing = amqpClient.consume(SYNC_QUEUE, s -> this.executeSync());
    }

    public void queueSync() throws IOException, TimeoutException {
        amqpClient.createChannel(SYNC_QUEUE);
        amqpClient.send(SYNC_QUEUE);
    }

    private void executeSync() {
        LOGGER.info("Syncing");
        dslContext.transaction(configuration -> {
            ImmutableSet<JobInfo> jobInfos = jobsTable.jobs(configuration);
            final MutableMap<String, ValidateJobInfoHasKeys> toValidates = Maps.mutable.empty();
            jobInfos.forEach(jobInfo -> {
                final String name = jobInfo.getUuid().toString();
                toValidates.put(name, ValidateJobInfoHasKeys.builder().jobInfo(jobInfo).name(name).jobKey(Optional.empty()).triggerKey(Optional.empty()).build());
            });
            allTriggers().forEach(triggerKey -> {
                String name = triggerKey.getName();
                if (toValidates.containsKey(name)) {
                    ValidateJobInfoHasKeys validateJobInfoHasKeys = toValidates.get(name);
                    if (validateJobInfoHasKeys.triggerKey.isPresent()) {
                        deleteTrigger(triggerKey);
                    } else {
                        toValidates.put(name, validateJobInfoHasKeys.toBuilder().triggerKey(Optional.of(triggerKey)).build());
                    }
                } else {
                    deleteTrigger(triggerKey);
                }
            });
            allJobs().forEach(jobKey -> {
                String name = jobKey.getName();
                if (toValidates.containsKey(name)) {
                    ValidateJobInfoHasKeys validateJobInfoHasKeys = toValidates.get(name);
                    if (validateJobInfoHasKeys.jobKey.isPresent()) {
                        deleteJob(jobKey);
                    } else {
                        toValidates.put(name, validateJobInfoHasKeys.toBuilder().jobKey(Optional.of(jobKey)).build());
                    }
                } else {
                    deleteJob(jobKey);
                }
            });
            validate(toValidates.valuesView().toSet().toImmutable());
        });
    }

    private void validate(ImmutableSet<ValidateJobInfoHasKeys> toValidates) {
        PartitionImmutableSet<ValidateJobInfoHasKeys> partition = toValidates.partition(this::validate);
        ImmutableSet<JobInfo> missingJobs = partition.getRejected().collect(validateJobInfoHasKeys -> {
            validateJobInfoHasKeys.triggerKey.ifPresent(this::deleteTrigger);
            validateJobInfoHasKeys.jobKey.ifPresent(this::deleteJob);
            return validateJobInfoHasKeys.jobInfo;
        });
        createNewJobs(missingJobs);

    }

    private boolean validate(ValidateJobInfoHasKeys validateJobInfoHasKeys) {
        try {
            if (validateJobInfoHasKeys.jobKey.filter(jk -> groupName.equals(jk.getGroup())).isEmpty()) {
                return false;
            }
            JobKey jobKey = validateJobInfoHasKeys.jobKey.get();
            if (validateJobInfoHasKeys.triggerKey.filter(tk -> groupName.equals(tk.getGroup())).isEmpty()) {
                return false;
            }
            TriggerKey triggerKey = validateJobInfoHasKeys.triggerKey.get();
            JobInfo jobInfo = validateJobInfoHasKeys.getJobInfo();
            Trigger trigger = scheduler.getTrigger(triggerKey);
            String source = jobInfo.getSource();
            if (!source.equals(trigger.getDescription()) || !(trigger instanceof CronTrigger) || !jobInfo.getCron().equals(((CronTrigger) trigger).getCronExpression())) {
                return false;
            }
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (!source.equals(jobDetail.getDescription()) || !AmqpJob.class.equals(jobDetail.getJobClass())) {
                return false;
            }
            return true;
        } catch (SchedulerException e) {
            LOGGER.warn(e);
            return false;
        }

    }

    private void createNewJobs(ImmutableSet<JobInfo> missingJobs) {
        LOGGER.info("New Jobs {}", missingJobs);
        missingJobs.forEach(jobInfo -> {

            try {
                String name = jobInfo.getUuid().toString();
                amqpClient.createChannel(name);
                JobDetail job = JobBuilder.newJob(AmqpJob.class)
                        .withIdentity(JobKey.jobKey(name, groupName))
                        .withDescription(jobInfo.getSource())
                        .build();
                CronTrigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(TriggerKey.triggerKey(name, groupName))
                        .withDescription(jobInfo.getSource())
                        .withSchedule(CronScheduleBuilder.cronSchedule(jobInfo.getCron()))
                        .build();
                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException | IOException | TimeoutException e) {
                LOGGER.warn(e);
            }
        });
    }

    private void deleteTrigger(TriggerKey triggerKey) {
        LOGGER.info("Deleting trigger {}", triggerKey.getName());
        try {
            amqpClient.deleteChannel(triggerKey.getName());
            scheduler.unscheduleJob(triggerKey);
        } catch (SchedulerException | IOException | TimeoutException e) {
            LOGGER.warn(e);
        }
    }

    private ImmutableSet<TriggerKey> allTriggers() {
        try {
            return Sets.adapt(scheduler.getTriggerKeys(GroupMatcher.anyGroup())).toImmutable();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteJob(JobKey jobKey) {
        LOGGER.info("Deleting job {}", jobKey.getName());

        try {
            amqpClient.deleteChannel(jobKey.getName());
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException | IOException | TimeoutException e) {
            LOGGER.warn(e);
        }
    }

    private ImmutableSet<JobKey> allJobs() {
        try {
            return Sets.adapt(scheduler.getJobKeys(GroupMatcher.anyGroup())).toImmutable();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException, TimeoutException {
        syncing.close();

    }

    @Builder(toBuilder = true)
    @Value
    public static class ValidateJobInfoHasKeys {
        String name;
        JobInfo jobInfo;
        Optional<JobKey> jobKey;
        Optional<TriggerKey> triggerKey;
    }

}
