import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.lang3.concurrent.CircuitBreaker;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import util.LiftInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerThread implements Runnable {
    private static final String EXCHANGE_NAME = "postRequest";
    private final Connection connection;
    public JedisPool pool;
    public EventCountCircuitBreaker breaker;
    private final GsonBuilder builder;
    private final Gson gson;

    public ConsumerThread(JedisPool pool, Connection connection, EventCountCircuitBreaker breaker) {
        this.pool = pool;
        this.connection = connection;
        this.breaker = breaker;
        this.builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();
    }

    @Override
    public void run() {
        Channel channel = null;
        if (breaker.checkState()) {
            try {
                channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, EXCHANGE_NAME, "");
                System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
                channel.basicQos(10); // accept only one unack-ed message at a time
                Channel finalChannel = channel;
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received '" + message + "'");
                    try {
                        processMessage(message);
                    } finally {
                        System.out.println(" [x] Done");
                        finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                };
                boolean autoAck = false;
                channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {
                });
            } catch (IOException e) {
                breaker.incrementAndCheckState();
            }
        } else {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    void processMessage(String message) {
        LiftInfo receivedInfo = gson.fromJson(message, LiftInfo.class);
        Jedis jedis = pool.getResource();
        try {
            //save to redis
            String skierId = receivedInfo.getSkierId().toString();
            String liftId = receivedInfo.getLiftId().toString();
            String minute = receivedInfo.getMinute().toString();
            String waitTime = receivedInfo.getWaitTime().toString();
            Integer vertical = (receivedInfo.getLiftId()*10);
            String strVertical = vertical.toString();
            Long dateTime = System.currentTimeMillis();
            String strDateTime = dateTime.toString();

            // add all info
            updateIfExists(jedis,skierId,"liftId",liftId);
            updateIfExists(jedis,skierId,"minute",minute);
            updateIfExists(jedis,skierId,"waitTime",waitTime);
            updateIfExists(jedis,skierId,"vertical",strVertical);
            updateIfExists(jedis,skierId,"dateTime",strDateTime);

        } catch (JedisException e) {
            // return to pool if needed
            if (null != jedis) {
                pool.returnBrokenResource(jedis);
                jedis = null;
            }
        } finally {
            // return to pool after finishing
            if (null != jedis)
                pool.returnResource(jedis);
        }
    }

    void updateIfExists(Jedis jedis, String skierId, String fieldName, String value){
        if (jedis.hexists(skierId,fieldName)){
            Map<String, String> fields = jedis.hgetAll(skierId);
            String currentValue = fields.get(fieldName);
            StringBuilder newValue = new StringBuilder();
            newValue.append(currentValue);
            newValue.append(" , ");
            newValue.append(value);
            jedis.hset(skierId, fieldName, newValue.toString());
            String output = jedis.hget(skierId, fieldName);
            System.out.println(output);
        } else {
            jedis.hset(skierId, fieldName, value);
        }
    }

}
