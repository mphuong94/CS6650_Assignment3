package utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class that has common work between 2 parts
 */
public abstract class ClientAbstract {
    private static final Integer POOL_SIZE = 3;
    // maximum number of threads to run (numThreads - max 1024)
    private int numThreads;
    // number of skier to generate lift rides for (numSkiers - max 100000), This is effectively the skier’s ID (skierID)
    private int numSkiers;
    // number of ski lifts (numLifts - range 5-60, default 40)
    private int numLifts = 40;
    // mean numbers of ski lifts each skier rides each day (numRuns - default 10, max 20)
    private int numRuns = 10;
    private String url;
    private SkierPhase phase1;
    private SkierPhase phase2;
    private SkierPhase phase3;
    private Integer totalSuccess;
    private Integer totalFailure;
    private Integer totalCalls;
    private Long wallTime;

    public ClientAbstract(int numThreads, int numSkiers, int numLifts, int numRuns, String url) {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.url = url;
        this.phase1 = new SkierPhase(this.numThreads / 4,
                this.numSkiers, this.numLifts, this.numRuns,
                this.numSkiers / (this.numThreads / 4),
                (int) ((this.numRuns * 0.2) * (this.numSkiers / (this.numThreads / 4))),
                1, 90,
                this.url, ClientPartEnum.DEFAULT
        );
        this.phase2 = new SkierPhase(this.numThreads,
                this.numSkiers, this.numLifts, this.numRuns,
                this.numSkiers / this.numThreads,
                (int) ((this.numRuns * 0.6) * (numSkiers / numThreads)),
                91, 360,
                this.url, ClientPartEnum.DEFAULT
        );
        this.phase3 = new SkierPhase((int) (this.numThreads * 0.1),
                this.numSkiers, this.numLifts, this.numRuns,
                this.numSkiers / (this.numThreads / 4),
                (int) (0.1 * this.numRuns),
                361, 420,
                this.url, ClientPartEnum.DEFAULT
        );
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getNumSkiers() {
        return numSkiers;
    }

    public void setNumSkiers(int numSkiers) {
        this.numSkiers = numSkiers;
    }

    public int getNumLifts() {
        return numLifts;
    }

    public void setNumLifts(int numLifts) {
        this.numLifts = numLifts;
    }

    public int getNumRuns() {
        return numRuns;
    }

    public void setNumRuns(int numRuns) {
        this.numRuns = numRuns;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SkierPhase getPhase1() {
        return phase1;
    }

    public void setPhase1(SkierPhase phase1) {
        this.phase1 = phase1;
    }

    public SkierPhase getPhase2() {
        return phase2;
    }

    public void setPhase2(SkierPhase phase2) {
        this.phase2 = phase2;
    }

    public SkierPhase getPhase3() {
        return phase3;
    }

    public void setPhase3(SkierPhase phase3) {
        this.phase3 = phase3;
    }

    public Integer getTotalSuccess() {
        return totalSuccess;
    }

    public Integer getTotalFailure() {
        return totalFailure;
    }

    public Integer getTotalCalls() {
        return totalCalls;
    }

    public Long getWallTime() {
        return wallTime;
    }

    /**
     * Main multithreaded function to coordinate the phases
     *
     * @throws InterruptedException
     */
    public void run() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("Phase 1 started");
        this.phase1.run();
        this.phase1.isNextReady();
        System.out.println("Phase 2 started");
        this.phase2.run();
        this.phase2.isNextReady();
        System.out.println("Phase 3 started");
        this.phase3.run();
        // wait for all threads to finish
        this.phase1.completed();
        this.phase2.completed();
        this.phase3.completed();

        long endTime = System.currentTimeMillis();
        this.wallTime = endTime - startTime;
        System.out.println("All phases done");
        this.totalSuccess = this.getPhase1().getSuccessCount() +
                this.getPhase2().getSuccessCount() + this.getPhase3().getSuccessCount();
        this.totalCalls = this.getPhase1().getTotalCalls() +
                this.getPhase2().getTotalCalls() + this.getPhase3().getTotalCalls();
        this.totalFailure = this.totalCalls - this.totalSuccess;
    }
}
