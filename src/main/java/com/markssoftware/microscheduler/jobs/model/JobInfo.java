package com.markssoftware.microscheduler.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class JobInfo {
    private final UUID uuid;
    private final String cron;
    private final String source;

}
