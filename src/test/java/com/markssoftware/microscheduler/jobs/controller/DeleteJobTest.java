package com.markssoftware.microscheduler.jobs.controller;

import com.markssoftware.microscheduler.jobs.service.JobsService;
import org.junit.jupiter.api.Test;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DeleteJobTest {
    @Test
    public void testDeleteJob() {
        final JobsService jobsService = mock(JobsService.class);
        final String param = "uuid";
        final DeleteJob deleteJob = new DeleteJob(param, jobsService);
        final HttpServerResponse httpServerResponse = mock(HttpServerResponse.class);
        when(httpServerResponse.status(204)).thenReturn(httpServerResponse);
        final HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
        final UUID uuid = UUID.randomUUID();
        when(httpServerRequest.param(param)).thenReturn(uuid.toString());
        deleteJob.apply(httpServerRequest, httpServerResponse);
        verify(jobsService, times(1)).deleteJob(uuid);
    }
}