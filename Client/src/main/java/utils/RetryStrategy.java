package utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Retry strategy for 5 times if response code is 4XX or 5XX
 */
public class RetryStrategy implements ServiceUnavailableRetryStrategy {
    private final static Integer NUM_RETRIES = 5;
    private final static Integer LOWER_BOUND = 3;
    private final static Integer UPPER_BOUND = 6;

    @Override
    public boolean retryRequest(
            final HttpResponse response, final int executionCount, final HttpContext context) {
        int statusCode = response.getStatusLine().getStatusCode();
        int firstNumber = Integer.parseInt(Integer.toString(statusCode).substring(0, 1));
        // check if code is 4xx or 5xx
        return LOWER_BOUND < firstNumber && firstNumber < UPPER_BOUND && executionCount <= NUM_RETRIES;
    }

    @Override
    public long getRetryInterval() {
        return 0;
    }

}