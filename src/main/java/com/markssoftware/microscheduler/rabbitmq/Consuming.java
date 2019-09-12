package com.markssoftware.microscheduler.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class Consuming implements AutoCloseable {
    private final Connection connection;
    private final Channel channel;

    public Consuming(ConnectionFactory factory, String queueName, Consumer<String> consumer) {
        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        try {
            channel = connection.createChannel();
        } catch (Throwable re) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (re instanceof RuntimeException) {
                    throw (RuntimeException) re;
                } else {
                    throw new RuntimeException(re);
                }
            }
        }
        startConsuming(queueName, consumer);
    }

    private void startConsuming(String queueName, Consumer<String> consumer) {
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
        } catch (IOException e) {
            close();
        }
    }

    public void close() {
        try {
            channel.close();
        } catch (TimeoutException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
