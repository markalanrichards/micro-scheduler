package com.markssoftware.microscheduler.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markssoftware.microscheduler.jooq.JobsTable;
import org.jooq.DSLContext;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.function.BiFunction;

import static java.nio.charset.StandardCharsets.UTF_8;
import static reactor.core.publisher.Mono.just;

public class GetJobs implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

    private final JobsTable jobsTable;
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public GetJobs(JobsTable jobsTable, DSLContext dslContext, ObjectMapper objectMapper) {
        this.jobsTable = jobsTable;
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        try {
            return httpServerResponse.header("Content-Type", "application/json").sendString(
                    just(objectMapper.writeValueAsString(dslContext.transactionResult(jobsTable::jobs))), UTF_8
            );
        } catch (JsonProcessingException e2) {
            throw new RuntimeException(e2);
        }
    }
}
