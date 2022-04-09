import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.lang3.concurrent.CircuitBreaker;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import util.LiftInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConsumerThread implements Runnable {
    private static final String QUEUE_NAME = "skier";
    private final Connection connection;
    public JedisPool pool;
    public EventCountCircuitBreaker breaker;
    private final GsonBuilder builder;
    private final Gson gson;

    public ConsumerThread(JedisPool pool, Connection connection) {
        this.pool = pool;
        this.connection = connection;
        this.builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();
    }

    @Override
    public void run() {
        Channel channel = null;
        try {
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicQos(1);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
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
            channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            e.printStackTrace();
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
            StringBuilder output = new StringBuilder();
            String liftIdInfo = concatInfo("liftId",liftId);
            String minuteInfo = concatInfo("minute",minute);
            String waitTimeInfo = concatInfo("waitTime",waitTime);
            String verticalInfo = concatInfo("vertical",strVertical);
            String dataTimeInfo = concatInfo("dateTime",strDateTime);
            output.append("/");
            output.append(liftIdInfo);
            output.append(minuteInfo);
            output.append(waitTimeInfo);
            output.append(verticalInfo);
            output.append(dataTimeInfo);
            output.append("/");
            jedis.append(skierId,output.toString());

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
    String concatInfo(String fieldName,String skierInfo){
        StringBuilder newValue = new StringBuilder();
        newValue.append(" ");
        newValue.append(fieldName);
        newValue.append(":");
        newValue.append(skierInfo);
        newValue.append(",");
        return newValue.toString();
    }

}
