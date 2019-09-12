package com.markssoftware.microscheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.markssoftware.microscheduler.amqp.AmqpClient;
import com.markssoftware.microscheduler.config.Config;
import com.markssoftware.microscheduler.controller.GetJobs;
import com.markssoftware.microscheduler.controller.PostJob;
import com.markssoftware.microscheduler.jooq.JobsTable;
import com.markssoftware.microscheduler.sync.SimpleSync;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static reactor.core.publisher.Mono.just;

public class UI {
    private static final Logger LOGGER = LogManager.getLogger(UI.class);

    public static void main(String[] args) throws SchedulerException {
        Config config = Config.builder().build();
        withScheduler(config, scheduler -> {
            try {
                start(scheduler, config);
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        });

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

    private static void start(Scheduler scheduler, Config config) throws IOException, TimeoutException {
        DSLContext dslContext = getDslContext(config);
        final ObjectMapper objectMapper = new ObjectMapper();
        JobsTable jobsTable = new JobsTable();
        objectMapper.registerModule(new EclipseCollectionsModule());
        objectMapper.registerModule(new ParameterNamesModule());
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        withSimpleSync(config, simpleSync -> {
            PostJob postJob = new PostJob(objectMapper, dslContext, jobsTable, simpleSync);
            GetJobs getJobs = new GetJobs(jobsTable, dslContext, objectMapper);
            withHttpServer(routes -> routes
                    .get("/ping", (request, response) -> response.sendString(just("Pong")))
                    .get("/jobs", getJobs)
                    .post("/job", postJob));
        }, scheduler, jobsTable, dslContext);

    }

    public static void withSimpleSync(Config config, Consumer<SimpleSync> simpleSyncConsumer, Scheduler scheduler, JobsTable jobsTable, DSLContext dslContext) throws IOException, TimeoutException {
        AmqpClient amqpClient = new AmqpClient(config.getRabbitmqHost());
        try (SimpleSync jobsSync = new SimpleSync(scheduler, jobsTable, dslContext, amqpClient)) {
            jobsSync.queueSync();
            simpleSyncConsumer.accept(jobsSync);
        }
    }

    private static void withHttpServer(Consumer<HttpServerRoutes> httpServerRoutesConsumer) {
        DisposableServer server = HttpServer.create()
                .host("0.0.0.0")
                .port(8080)
                .route(httpServerRoutesConsumer)
                .bindNow();
        LOGGER.info("HTTP Server should be starting");
        server.onDispose()
                .block();
    }

    private static void withScheduler(Config config, Consumer<Scheduler> schedulerConsumer) throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory(config.getProperties()).getScheduler();
        try {
            schedulerConsumer.accept(scheduler);
        } finally {
            scheduler.shutdown();
        }
    }

}
