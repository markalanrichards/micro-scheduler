package com.markssoftware.microscheduler.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class AmqpClient {
    private final ConnectionFactory factory;
    private final static Logger LOGGER = LogManager.getLogger(AmqpClient.class);

    public static class Consuming implements AutoCloseable {
        private final Connection connection;
        private final Channel channel;

        public Consuming(ConnectionFactory factory, String queueName, Consumer<String> consumer) throws IOException, TimeoutException {
            connection = factory.newConnection();
            try {
                channel = connection.createChannel();
            } catch (RuntimeException re) {
                connection.close();
                throw re;
            }
            startConsuming(queueName, consumer);
        }

        public void startConsuming(String queueName, Consumer<String> consumer) throws IOException, TimeoutException {
            try {
                channel.basicQos(1);
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    try {
                        consumer.accept(message);
                    } finally {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                };
                channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
                });
            } catch (IOException | RuntimeException e) {
                close();
                throw e;
            }
        }

        public void close() throws IOException, TimeoutException {
            try {
                channel.close();
            } finally {

                connection.close();
            }
        }
    }

    public AmqpClient(String host) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.useNio();
        factory.setAutomaticRecoveryEnabled(true);
        this.factory = factory;


    }

    public void send(final String queueName) throws IOException, TimeoutException {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.basicPublish("", queueName, null, queueName.getBytes());
            LOGGER.info("Sent {}", queueName);
        }
    }

    public void createChannel(final String queueName) throws IOException, TimeoutException {
        LOGGER.info("Creating channel {}", queueName);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);

        }
    }

    public void deleteChannel(final String queueName) throws IOException, TimeoutException {
        LOGGER.info("Deleting channel {}", queueName);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDelete(queueName);
        }
    }

    public Consuming consume(final String queueName, Consumer<String> consumer) throws IOException, TimeoutException {
        return new Consuming(factory, queueName, consumer);
    }

}
