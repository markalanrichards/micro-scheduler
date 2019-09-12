CREATE TABLE IF NOT EXISTS quartz.jobs(
    uuid BINARY(16) NOT NULL,
    source TEXT NOT NULL,
    cron TEXT NOT NULL,
    PRIMARY KEY (uuid)
)