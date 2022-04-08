import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String rabbitHost = "34.219.103.201";
    private static final String userName = "guest";
    private static final String password = "guest";
    private static final String redisHost = "54.149.88.106";
    private static final Integer redisPort = 6379;
    private static JedisPool pool = null;
    static int numThread = 32;
    private static EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(1000, 1, TimeUnit.MINUTES, 800);

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setUsername(userName);
        factory.setPassword(password);
        pool = new JedisPool(redisHost, redisPort);

        if (args.length == 0) {
            throw new IllegalArgumentException("Some arguments are missing values");
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            params.put(args[i], args[i + 1]);
        }

        // validation
        if (params.containsKey("--numThread")) {
            int numThreadArg = Integer.parseInt(params.get("--numThread"));
            if (numThreadArg <= 0 | numThreadArg > 1024) {
                System.out.println("Invalid number of thread, default to 32");
            } else {
                numThread = numThreadArg;
            }
        } else {
            throw new IllegalArgumentException("Missing --numThread arguments");
        }

        try {
            Connection newConnection = factory.newConnection();
            ConsumerThread[] consumers = new ConsumerThread[numThread];
            for (int i = 0; i < numThread; i++) {
                consumers[i] = new ConsumerThread(pool, newConnection, breaker);
            }

            Thread[] threads = new Thread[numThread];
            for (int i = 0; i < numThread; i++) {
                threads[i] = new Thread(consumers[i]);
                threads[i].start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
