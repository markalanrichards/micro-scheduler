package com.markssoftware.microscheduler.jobs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markssoftware.microscheduler.jobs.model.JobInfo;
import com.markssoftware.microscheduler.jobs.service.JobsService;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.function.BiFunction;

public class PostJob implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private final ObjectMapper objectMapper;
    private final JobsService jobsService;

    public PostJob(ObjectMapper objectMapper, JobsService jobsService) {
        this.objectMapper = objectMapper;
        this.jobsService = jobsService;
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
                            jobsService.saveJob(jobInfo);
                            return jobsService.jobInfos();
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
