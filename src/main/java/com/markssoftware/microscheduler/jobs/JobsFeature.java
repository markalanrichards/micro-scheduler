package com.markssoftware.microscheduler.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.markssoftware.microscheduler.Config;
import com.markssoftware.microscheduler.jobs.controller.DeleteJob;
import com.markssoftware.microscheduler.jobs.controller.GetJobs;
import com.markssoftware.microscheduler.jobs.controller.PostJob;
import com.markssoftware.microscheduler.jobs.jooq.JobsTable;
import com.markssoftware.microscheduler.jobs.repository.JobsRepository;
import com.markssoftware.microscheduler.jobs.service.JobsService;
import com.markssoftware.microscheduler.rest.HttpserverFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class JobsFeature {

    private final JobsRepository jobsRepository;

    public JobsFeature(Config config) {
        DSLContext dslContext = getDslContext(config);
        JobsTable jobsTable = new JobsTable();
        jobsRepository = new JobsRepository(dslContext, jobsTable);
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new EclipseCollectionsModule());
        objectMapper.registerModule(new ParameterNamesModule());
        return objectMapper;
    }

    private static DSLContext getDslContext(Config config) {
        final HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(config.getMyDsUrl());
        configuration.setUsername(config.getMyDsUser());
        configuration.setPassword(config.getMyDsPassword());
        configuration.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
        configuration.setAutoCommit(false);
        configuration.setReadOnly(false);
        return DSL.using(new HikariDataSource(configuration), SQLDialect.MYSQL);
    }

    public JobsRepository getJobsRepository() {
        return jobsRepository;
    }

    public void run(Runnable sync) {
        JobsService jobsService = new JobsService(jobsRepository, sync);
        ObjectMapper objectMapper = createObjectMapper();
        GetJobs getJobs = new GetJobs(objectMapper, jobsService);
        PostJob postJob = new PostJob(objectMapper, jobsService);
        DeleteJob deleteJob = new DeleteJob("key", jobsService);
        new HttpserverFactory(getJobs, postJob, "key", deleteJob).startAndBlockServer();
    }
}
