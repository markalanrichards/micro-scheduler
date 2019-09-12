package com.markssoftware.microscheduler.rest;

import com.markssoftware.microscheduler.jobs.controller.DeleteJob;
import com.markssoftware.microscheduler.jobs.controller.GetJobs;
import com.markssoftware.microscheduler.jobs.controller.PostJob;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static reactor.core.publisher.Mono.just;

public class HttpserverFactory {
    private final GetJobs getJobs;
    private final PostJob postJob;
    private final DeleteJob deleteJob;
    private final String deletePath;

    public HttpserverFactory(GetJobs getJobs, PostJob postJob, String key, DeleteJob deleteJob) {
        this.getJobs = getJobs;
        this.postJob = postJob;
        deletePath = String.format("/job/{%s}", key);
        this.deleteJob = deleteJob;
    }

    public void startAndBlockServer() {
        DisposableServer server = HttpServer.create()
                .host("0.0.0.0")
                .port(8080)
                .route(httpServerRoutes ->
                        httpServerRoutes
                                .get("/ping", (request, response) -> response.sendString(just("Pong")))
                                .get("/jobs", getJobs)
                                .post("/job", postJob)
                                .delete(deletePath, deleteJob)
                )
                .bindNow();
        server.onDispose()
                .block();
    }
}
