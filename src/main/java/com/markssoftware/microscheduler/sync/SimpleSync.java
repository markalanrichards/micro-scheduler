package com.markssoftware.microscheduler.sync;

import com.markssoftware.microscheduler.jobs.model.JobInfo;
import com.markssoftware.microscheduler.jobs.repository.JobsRepository;
import com.markssoftware.microscheduler.quartz.SchedulerService;
import com.markssoftware.microscheduler.rabbitmq.AmqpClient;
import com.markssoftware.microscheduler.rabbitmq.AmqpJob;
import lombok.Builder;
import lombok.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.partition.set.PartitionImmutableSet;
import org.eclipse.collections.api.set.ImmutableSet;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.util.Optional;

import static com.markssoftware.microscheduler.sync.SyncConfig.groupName;

public class SimpleSync implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(SimpleSync.class);


    private final SchedulerService schedulerService;
    private final JobsRepository jobsRepository;
    private final AmqpClient amqpClient;

    public SimpleSync(SchedulerService schedulerService, JobsRepository jobsRepository, AmqpClient amqpClient) {
        this.schedulerService = schedulerService;
        this.jobsRepository = jobsRepository;
        this.amqpClient = amqpClient;
    }


    public void run() {
        LOGGER.info("Syncing");
        try {
            jobsRepository.lockWithJobInfos(jobInfos -> validateAndReturnMissingJobs(getCurrentState(jobInfos)));
        } catch (Throwable t) {
            LOGGER.warn("Error syncing", t);
        }
    }

    private ImmutableSet<ValidateJobInfoHasKeys> getCurrentState(ImmutableSet<JobInfo> jobInfos) {
        final MutableMap<String, ValidateJobInfoHasKeys> toValidates = Maps.mutable.empty();
        jobInfos.forEach(jobInfo -> {
            final String name = jobInfo.getUuid().toString();
            toValidates.put(name, ValidateJobInfoHasKeys.builder().jobInfo(jobInfo).name(name).jobKey(Optional.empty()).triggerKey(Optional.empty()).build());
        });
        schedulerService.allTriggers().forEach(triggerKey -> {
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
        schedulerService.allJobs().forEach(jobKey -> {
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
        return toValidates.valuesView().toSet().toImmutable();
    }

    private void validateAndReturnMissingJobs(ImmutableSet<ValidateJobInfoHasKeys> toValidates) {
        PartitionImmutableSet<ValidateJobInfoHasKeys> partition = toValidates.partition(this::validate);
        ImmutableSet<JobInfo> missingJobs = partition.getRejected().collect(validateJobInfoHasKeys -> {
            validateJobInfoHasKeys.triggerKey.ifPresent(this::deleteTrigger);
            validateJobInfoHasKeys.jobKey.ifPresent(this::deleteJob);
            return validateJobInfoHasKeys.jobInfo;
        });
        createNewJobs(missingJobs);

    }

    private void deleteJob(JobKey jobKey) {
        amqpClient.deleteChannel(jobKey.getName());
        schedulerService.deleteJob(jobKey);
    }

    private boolean validate(ValidateJobInfoHasKeys validateJobInfoHasKeys) {
        if (validateJobInfoHasKeys.jobKey.filter(jk -> groupName.equals(jk.getGroup())).isEmpty()) {
            return false;
        }
        JobKey jobKey = validateJobInfoHasKeys.jobKey.get();
        if (validateJobInfoHasKeys.triggerKey.filter(tk -> groupName.equals(tk.getGroup())).isEmpty()) {
            return false;
        }
        TriggerKey triggerKey = validateJobInfoHasKeys.triggerKey.get();
        JobInfo jobInfo = validateJobInfoHasKeys.getJobInfo();
        Trigger trigger = schedulerService.getTrigger(triggerKey);
        String source = jobInfo.getSource();
        if (!source.equals(trigger.getDescription()) || !(trigger instanceof CronTrigger) || !jobInfo.getCron().equals(((CronTrigger) trigger).getCronExpression())) {
            return false;
        }
        JobDetail jobDetail = schedulerService.getJobDetail(jobKey);
        if (!source.equals(jobDetail.getDescription()) || !AmqpJob.class.equals(jobDetail.getJobClass())) {
            return false;
        }
        return true;
    }

    private void createNewJobs(ImmutableSet<JobInfo> missingJobs) {
        LOGGER.info("New Jobs {}", missingJobs);
        missingJobs.forEach(jobInfo -> {
            try {
                String name = jobInfo.getUuid().toString();
                amqpClient.createChannel(name);
                schedulerService.createJob(name, jobInfo.getSource(), jobInfo.getCron());
            } catch (Throwable e) {
                LOGGER.warn("Error creating new jobs", e);
            }
        });
    }

    private void deleteTrigger(TriggerKey triggerKey) {
        LOGGER.info("Deleting trigger {}", triggerKey.getName());
        amqpClient.deleteChannel(triggerKey.getName());
        schedulerService.unscheduleJob(triggerKey);

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
