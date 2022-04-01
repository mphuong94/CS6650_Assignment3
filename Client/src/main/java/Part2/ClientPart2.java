package Part2;

import utils.ClientAbstract;
import utils.ClientPartEnum;
import utils.LatencyStat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Part 2: Get statistic of each individual call
 */
public class ClientPart2 extends ClientAbstract {

    public ClientPart2(int numThreads, int numSkiers, int numLifts, int numRuns, String url) {
        super(numThreads, numSkiers, numLifts, numRuns, url);
        this.getPhase1().setPartChosen(ClientPartEnum.PART2);
        this.getPhase2().setPartChosen(ClientPartEnum.PART2);
        this.getPhase3().setPartChosen(ClientPartEnum.PART2);
    }

    /**
     * Helper method to get percentile
     * using a sorted list and index
     *
     * @param numbers
     * @param percent
     * @return
     */
    private static long getPercentile(List<Long> numbers, double percent) {
        int index = (int) Math.ceil(percent / 100.0 * numbers.size());
        return numbers.get(index - 1);
    }

    /**
     * Helper method to write LatencyStats into CSV
     *
     * @param stats
     */
    private void writeCSV(List<LatencyStat> stats) {
        File file = new File(this.getNumThreads() + "_output.csv");
        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            bufferWriter.write("Start Time, Request Type, Response Code, Latency");
            bufferWriter.newLine();

            for (LatencyStat stat : stats) {
                StringJoiner singleRow = new StringJoiner(",");
                singleRow.add(stat.getStartTime().toString());
                singleRow.add(stat.getRequestType());
                singleRow.add(stat.getResponseCode().toString());
                singleRow.add(stat.getLatency().toString());
                String content = singleRow.toString();
                bufferWriter.write(content);
                bufferWriter.newLine();
            }
            bufferWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Collapse into 1 list
        List<LatencyStat> output = new ArrayList(this.getPhase1().getHistory());
        output.addAll(this.getPhase2().getHistory());
        output.addAll(this.getPhase3().getHistory());

        // Write CSV
        this.writeCSV(output);

        // Print statistic
        List<Long> total = output.stream()
                .map(LatencyStat::getLatency)
                .filter(x -> x > -1)
                .collect(Collectors.toList());
        double mean = total.stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
        Collections.sort(total);
        double median = getPercentile(total, 50.0);
        double ninetyNine = getPercentile(total, 99.0);
        double minLatency = total.get(0);
        double maxLatency = total.get(total.size() - 1);
        double MILLISECOND_TO_SECOND = 0.001;
        float throughput = (float) (this.getTotalCalls() / (this.getWallTime() * MILLISECOND_TO_SECOND));
        System.out.println("Wall time " + this.getWallTime());
        System.out.println("****LATENCY STATISTIC****");
        System.out.printf("Mean: %.2f\n", mean);
        System.out.printf("Median: %.2f\n", median);
        System.out.printf("Throughput: %.2f\n", throughput);
        System.out.printf("99th percentile: %.2f\n", ninetyNine);
        System.out.printf("Minimum: %.2f\n", minLatency);
        System.out.printf("Maximum: %.2f\n", maxLatency);
        return;
    }

}

