package com.markssoftware.microscheduler.jobs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markssoftware.microscheduler.jobs.model.JobInfo;
import com.markssoftware.microscheduler.jobs.service.JobsService;
import org.eclipse.collections.api.set.ImmutableSet;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.function.BiFunction;

import static java.nio.charset.StandardCharsets.UTF_8;
import static reactor.core.publisher.Mono.just;

public class GetJobs implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

    private final JobsService jobsService;
    private final ObjectMapper objectMapper;

    public GetJobs(ObjectMapper objectMapper, JobsService jobsService) {
        this.jobsService = jobsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        try {
            ImmutableSet<JobInfo> value = jobsService.jobInfos();
            return httpServerResponse.header("Content-Type", "application/json").sendString(
                    just(objectMapper.writeValueAsString(value)), UTF_8
            );
        } catch (JsonProcessingException e2) {
            throw new RuntimeException(e2);
        }
    }
}
