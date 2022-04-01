import Part1.ClientPart1;
import Part2.ClientPart2;
import utils.ClientAbstract;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Default value
        String url = "http://localhost:8080/ResortServer_war_exploded/skiers/";
//      String url = "http://LB-1463899760.us-west-2.elb.amazonaws.com:8080/ResortServer_war/skiers/";
        int numThread = 32;
        int numSkiers = 20000;
        int numLifts = 40;
        int numRuns = 10;
        int part = 0;

        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Some arguments are missing values");
        } else if (args.length > 0) {
            ClientAbstract client = null;
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

            if (params.containsKey("--numSkiers")) {
                int numSkiersArg = Integer.parseInt(params.get("--numSkiers"));
                if (numSkiersArg > 100000) {
                    System.out.println("Invalid number of thread, default to 20000");
                } else {
                    numSkiers = numSkiersArg;
                }
            } else {
                throw new IllegalArgumentException("Missing --numSkiers arguments");
            }

            if (params.containsKey("--numLifts")) {
                int numLiftsArg = Integer.parseInt(params.get("--numLifts"));
                if (numLiftsArg < 5 | numLiftsArg > 60) {
                    System.out.println("Invalid number of thread, default to 40");
                } else {
                    numLifts = numLiftsArg;
                }
            } else {
                throw new IllegalArgumentException("Missing --numLifts arguments");
            }

            if (params.containsKey("--numRuns")) {
                int numRunsArg = Integer.parseInt(params.get("--numRuns"));
                if (numRunsArg < 0 | numRunsArg > 20) {
                    System.out.println("Invalid number of thread, default to 10");
                } else {
                    numRuns = numRunsArg;
                }
            } else {
                throw new IllegalArgumentException("Missing --numRuns arguments");
            }

            if (params.containsKey("--url")) {
                url = params.get("--url");
            } else {
                throw new IllegalArgumentException("Missing --url arguments");
            }

            if (params.containsKey("--part")) {
                int partArg = Integer.parseInt(params.get("--part"));
                if (partArg != 1 && partArg != 2) {
                    System.out.println("Invalid Client part to run, not included in assignment");
                } else {
                    if (partArg == 1) {
                        client = new ClientPart1(numThread, numSkiers, numLifts, numRuns, url);
                        part = 1;
                    } else {
                        client = new ClientPart2(numThread, numSkiers, numLifts, numRuns, url);
                        part = 2;
                    }
                }
            } else {
                throw new IllegalArgumentException("Missing --part arguments");
            }
            System.out.println("Connecting to :" + url);
            System.out.println("Running Client Part :" + part);
            System.out.printf("Number of threads running: %d\n", numThread);
            System.out.printf("Number of skiers running: %d\n", numSkiers);
            System.out.printf("Number of lifts running: %d\n", numLifts);
            System.out.printf("Number of runs: %d\n", numRuns);
            client.run();
        } else {
            System.out.println("No command line arguments found.");
            return;
        }
        System.exit(0);
    }

}
