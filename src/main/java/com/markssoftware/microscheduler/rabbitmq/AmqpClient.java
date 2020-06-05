package com.markssoftware.microscheduler.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class AmqpClient {
    private final static Logger LOGGER = LogManager.getLogger(AmqpClient.class);
    private final ConnectionFactory factory;

    public AmqpClient(String host) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.useNio();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setAutomaticRecoveryEnabled(true);
        this.factory = factory;


    }

    public void send(final String queueName) {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.basicPublish("", queueName, null, queueName.getBytes());
            LOGGER.info("Sent {}", queueName);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void createChannel(final String queueName) {
        LOGGER.info("Creating channel {}", queueName);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);

        } catch (TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteChannel(final String queueName) {
        LOGGER.info("Deleting channel {}", queueName);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDelete(queueName);
        } catch (TimeoutException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Consuming consume(final String queueName, Consumer<String> consumer) {
        return new Consuming(factory, queueName, consumer);
    }

}
