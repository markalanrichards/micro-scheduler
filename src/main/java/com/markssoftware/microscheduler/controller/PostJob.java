package com.markssoftware.microscheduler.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markssoftware.microscheduler.jobs.JobInfo;
import com.markssoftware.microscheduler.jooq.JobsTable;
import com.markssoftware.microscheduler.sync.SimpleSync;
import org.jooq.DSLContext;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

public class PostJob implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private final ObjectMapper objectMapper;
    private final DSLContext dslContext;
    private final JobsTable jobsTable;
    private final SimpleSync jobsSync;
    public PostJob(ObjectMapper objectMapper, DSLContext dslContext, JobsTable jobsTable, SimpleSync jobsSync) {
        this.objectMapper = objectMapper;
        this.dslContext = dslContext;
        this.jobsTable = jobsTable;
        this.jobsSync = jobsSync;
    }
    @Override
    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return httpServerResponse
                .header("Content-Type", "application/json")
                .sendString(httpServerRequest.receive().asByteArray().map(bytes -> {
                            try {
                                return objectMapper.readValue(bytes, JobInfo.class);
                            } catch (IOException e1) {
                                throw new RuntimeException(e1);
                            }
                        }).map(jobInfo -> {
                            dslContext.transaction(c -> jobsTable.save(jobInfo, c));
                            try {
                                jobsSync.queueSync();
                            } catch (IOException | TimeoutException e) {
                                throw new RuntimeException(e);
                            }
                            return dslContext.transactionResult(jobsTable::jobs);
                        }).map(jobInfos -> {
                            try {
                                return objectMapper.writeValueAsString(jobInfos);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                );
    }
}
