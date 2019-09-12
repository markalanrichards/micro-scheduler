package com.markssoftware.microscheduler.jobs.service;

import com.markssoftware.microscheduler.jobs.model.JobInfo;
import com.markssoftware.microscheduler.jobs.repository.JobsRepository;
import org.eclipse.collections.api.set.ImmutableSet;

import java.util.UUID;

public class JobsService {
    private final JobsRepository jobsRepository;
    private final Runnable sync;

    public JobsService(JobsRepository jobsRepository, Runnable sync) {
        this.jobsRepository = jobsRepository;
        this.sync = sync;
    }

    public ImmutableSet<JobInfo> jobInfos() {
        return jobsRepository.jobInfos();
    }


    public void saveJob(JobInfo jobInfo) {
        jobsRepository.save(jobInfo, sync);
    }

    public void deleteJob(UUID name) {
        jobsRepository.delete(name);
    }
}
