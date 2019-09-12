package com.markssoftware.microscheduler.sync;

import com.markssoftware.microscheduler.rabbitmq.AmqpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.markssoftware.microscheduler.sync.SyncConfig.SYNC_QUEUE;

public class QueueSync implements Runnable {
    private final AmqpClient amqpClient;
    private final Logger LOGGER = LogManager.getLogger(QueueSync.class);

    public QueueSync(AmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }


    @Override
    public void run() {
        try {
            amqpClient.createChannel(SYNC_QUEUE);
            amqpClient.send(SYNC_QUEUE);
        } catch (Throwable t) {
            LOGGER.warn("Error sending sync request", t);
            throw t;
        }
    }
}
