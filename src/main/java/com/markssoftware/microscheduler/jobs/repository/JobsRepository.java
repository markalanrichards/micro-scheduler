package com.markssoftware.microscheduler.jobs.repository;

import com.markssoftware.microscheduler.jobs.jooq.JobsTable;
import com.markssoftware.microscheduler.jobs.model.JobInfo;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jooq.DSLContext;

import java.util.UUID;
import java.util.function.Consumer;

public class JobsRepository {
    private final DSLContext dslContext;
    private final JobsTable jobsTable;

    public JobsRepository(DSLContext dslContext, JobsTable jobsTable) {
        this.dslContext = dslContext;
        this.jobsTable = jobsTable;
    }

    public ImmutableSet<JobInfo> jobInfos() {
        return dslContext.transactionResult(jobsTable::jobs);
    }

    public void lockWithJobInfos(Consumer<ImmutableSet<JobInfo>> jobInfosConsumer) {
        dslContext.transaction(configuration -> {
            ImmutableSet<JobInfo> jobs = jobsTable.jobsForUpdate(configuration);
            jobInfosConsumer.accept(jobs);

        });
    }

    public void save(JobInfo jobInfo, Runnable postSavewithinTransaction) {
        dslContext.transaction(configuration -> {
            jobsTable.save(jobInfo, configuration);
            postSavewithinTransaction.run();
        });
    }

    public void delete(UUID name) {
        dslContext.transaction(configuration -> {
            jobsTable.delete(name, configuration);
        });
    }
}
