package Part1;

import utils.ClientAbstract;
import utils.ClientPartEnum;

/**
 * Part 1: Get wall time and throughput
 */
public class ClientPart1 extends ClientAbstract {
    public ClientPart1(int numThreads, int numSkiers, int numLifts, int numRuns, String url) {
        super(numThreads, numSkiers, numLifts, numRuns, url);
        this.getPhase1().setPartChosen(ClientPartEnum.PART1);
        this.getPhase2().setPartChosen(ClientPartEnum.PART1);
        this.getPhase3().setPartChosen(ClientPartEnum.PART1);
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        double MILLISECOND_TO_SECOND = 0.001;
        float throughput = (float) (this.getTotalCalls() / (this.getWallTime() * MILLISECOND_TO_SECOND));
        System.out.printf("Success calls: %d\n", this.getTotalSuccess());
        System.out.printf("Failure calls: %d\n", this.getTotalFailure());
        System.out.printf("Wall time: %d\n", this.getWallTime());
        System.out.printf("Throughput: %.2f\n", throughput);
        return;
    }

}
