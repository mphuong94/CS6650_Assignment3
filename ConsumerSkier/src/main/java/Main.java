import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.concurrent

public class Main {
    private static final String rabbitHost = "54.190.81.230";
    private static final String userName = "guest";
    private static final String password = "guest";
    private static final String redisHost = "54.213.231.1";
    private static final Integer redisPort = 6379;
    private static JedisPool pool = null;
    static int numThread = 32;
    EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(1000, 1, TimeUnit.MINUTE, 800);
    public void handleRequest(Request request) {
        if (breaker.incrementAndCheckState()) {
            // actually handle this request
        } else {
            // do something else, e.g. send an error code
        }
    }

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
                consumers[i] = new ConsumerThread(pool, newConnection);
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
