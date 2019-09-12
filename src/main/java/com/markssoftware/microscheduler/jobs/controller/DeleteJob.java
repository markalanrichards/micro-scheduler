package com.markssoftware.microscheduler.jobs.controller;

import com.markssoftware.microscheduler.jobs.service.JobsService;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.UUID;
import java.util.function.BiFunction;

public class DeleteJob implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private final String param;
    private final JobsService jobsService;

    public DeleteJob(String param, JobsService jobsService) {
        this.param = param;
        this.jobsService = jobsService;
    }


    @Override
    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {

        String param = httpServerRequest.param(this.param);
        jobsService.deleteJob(UUID.fromString(param));
        return httpServerResponse.status(204).send();
    }
}
