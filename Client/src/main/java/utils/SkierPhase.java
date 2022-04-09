package utils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main class to run different threads
 * and post multiple requests to server
 */
public class SkierPhase implements Runnable {
    private static final SecureRandom random = new SecureRandom();
    // Both phase 2 and 3 starts at 20%
    private static final double PERCENT_TO_START = 0.2;
    private static final int WAIT_TIME_MAX = 10;
    private final Integer numThreads;
    private final Integer numSkiers;
    private final Integer numLifts;
    private final Integer numRuns;
    private final Integer range;
    private final Integer numRequestToSend;
    private final Integer startTime;
    private final Integer endTime;
    private final String url;
    // fields created on init
    private final AtomicInteger successCount;
    private final CountDownLatch startNext;
    private final CountDownLatch isComplete;
    private final Integer totalCalls;
    private final List<LatencyStat> history = Collections.synchronizedList(new ArrayList<>());
    CloseableHttpClient client;
    private ClientPartEnum partChosen;
    private static EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(1000, 1, TimeUnit.SECONDS, 800);

    public SkierPhase(Integer numThreads, Integer numSkiers, Integer numLifts, Integer numRuns, Integer range, Integer numRequestToSend, Integer startTime, Integer endTime, String url, ClientPartEnum partChosen) {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.range = range;
        this.numRequestToSend = numRequestToSend;
        this.startTime = startTime;
        this.endTime = endTime;
        this.url = url;
        this.partChosen = partChosen;
        this.successCount = new AtomicInteger(0);
        this.startNext = new CountDownLatch((int) Math.ceil(numThreads * PERCENT_TO_START));
        this.totalCalls = this.numThreads * this.numRequestToSend;
        this.isComplete = new CountDownLatch(this.totalCalls);
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(50000);
        connManager.setDefaultMaxPerRoute(20000);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setServiceUnavailableRetryStrategy(new RetryStrategy())
                .build();
        this.client = httpClient;
    }

    public List<LatencyStat> getHistory() {
        return history;
    }

    public void setPartChosen(ClientPartEnum partChosen) {
        this.partChosen = partChosen;
    }

    public void incrementSuccess() {
        successCount.getAndIncrement();
    }

    public int getSuccessCount() {
        return successCount.get();
    }


    public Integer getTotalCalls() {
        return totalCalls;
    }

    /**
     * Method to make a post request with the defined specifications
     */
    @Override
    public void run() {
        System.out.println("Number of calls being made: " + this.getTotalCalls());
        for (int i = 0; i < this.numThreads; i++) {
            int rangeChunk = (int) Math.ceil(this.numSkiers / this.range);
            int startRange = i * rangeChunk;
            int skierID = random.nextInt(rangeChunk) + startRange + 1;
            int liftID = Math.abs(random.nextInt(this.numLifts));
            int time = random.nextInt(this.endTime - this.startTime + 1) + this.startTime;
            int waitTime = random.nextInt(WAIT_TIME_MAX);

            Runnable thread = () -> {
                // send a number of POST request
                for (int j = 0; j < this.numRequestToSend; j++) {
                    if (breaker.checkState()) {
                        try {
                            PostConnection newPost = new PostConnection(client, url, skierID, liftID, time, waitTime);
                            LatencyStat result = newPost.makeConnection(this.partChosen);
                            if (result.getResponseCode() == HttpStatus.SC_CREATED) {
                                this.incrementSuccess();
                            } else {
                                System.out.println("FAILURE");
                                breaker.incrementAndCheckState();
                            }

                            if (this.partChosen == ClientPartEnum.PART2) {
                                this.history.add(result);
                            }

                            this.isComplete.countDown();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        this.startNext.countDown();
                    }

                }
            };
            new Thread(thread).start();
        }

    }

    public void isNextReady() {
        try {
            this.startNext.await();
        } catch (InterruptedException e) {
            System.err.println("Next Phase Countdown Latch Exception");
            e.printStackTrace();
        }
    }

    public void completed() {
        try {
            this.isComplete.await();
        } catch (InterruptedException e) {
            System.err.println("Next Phase Countdown Latch Exception");
            e.printStackTrace();
        }
    }
}

