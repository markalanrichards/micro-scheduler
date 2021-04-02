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
        final String param = httpServerRequest.param(this.param);
        final UUID uuid = UUID.fromString(param);
        jobsService.deleteJob(uuid);
        final HttpServerResponse status = httpServerResponse.status(204);
        return status.send();
    }
}
