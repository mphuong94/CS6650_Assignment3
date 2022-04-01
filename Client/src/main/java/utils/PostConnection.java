package utils;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that takes care of making HTTP request
 * to server
 */
public class PostConnection {
    CloseableHttpClient client;
    private String url;
    private Integer skierID;
    private Integer liftID;
    private Integer minuteRange;
    private Integer waitTime;

    public PostConnection(CloseableHttpClient client, String url, Integer skierID, Integer liftID, Integer minuteRange, Integer waitTime) {
        this.client = client;
        this.url = url;
        this.skierID = skierID;
        this.liftID = liftID;
        this.minuteRange = minuteRange;
        this.waitTime = waitTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSkierID() {
        return skierID;
    }

    public void setSkierID(Integer skierID) {
        this.skierID = skierID;
    }

    public Integer getLiftID() {
        return liftID;
    }

    public void setLiftID(Integer liftID) {
        this.liftID = liftID;
    }

    public Integer getMinuteRange() {
        return minuteRange;
    }

    public void setMinuteRange(Integer minuteRange) {
        this.minuteRange = minuteRange;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

    /**
     * Method to call server and get a response, record latency
     *
     * @param part
     * @return LatencyStat
     * @throws IOException
     */
    public LatencyStat makeConnection(ClientPartEnum part) throws IOException {
        LatencyStat result = new LatencyStat();
        long start = 0;
        long end = 0;
        long latency = 0;
        // Try to use just one client to remove bottleneck
        // at the outside of the for loop

        HttpPost request = new HttpPost(this.url);
        List<BasicNameValuePair> urlParameters = new ArrayList<>();

        urlParameters.add(new BasicNameValuePair("skier_id", this.getSkierID().toString()));
        urlParameters.add(new BasicNameValuePair("lift_id", this.getLiftID().toString()));
        urlParameters.add(new BasicNameValuePair("minute", this.getMinuteRange().toString()));
        urlParameters.add(new BasicNameValuePair("wait", this.getWaitTime().toString()));

        HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
        request.setEntity(postParams);

        try {
            if (part == ClientPartEnum.PART2) {
                start = System.currentTimeMillis();
            }
            CloseableHttpResponse response = this.client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            String requestType = "POST";
            // Execute the method.
            if (status != HttpStatus.SC_CREATED) {
                System.err.println("Method failed: " + status);
            }
            if (part == ClientPartEnum.PART2) {
                end = System.currentTimeMillis();
                latency = end - start;
            }
            response.close();
            result = new LatencyStat(start, requestType, status, latency);
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            return result;
        }
    }
}
