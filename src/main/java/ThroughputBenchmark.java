import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThroughputBenchmark {

    private static final int DRIVER_COUNT = 50_000;
    private static final int REQUESTS_PER_TRIAL = 5_000;
    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 16};

    private static final double LAT_MIN = 52.00;
    private static final double LAT_MAX = 52.50;
    private static final double LON_MIN = 4.20;
    private static final double LON_MAX = 5.00;

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random(11);

        List<Driver> drivers = new ArrayList<>();
        SpatialGrid grid = new SpatialGrid(2.0, (LAT_MIN + LAT_MAX) / 2);
        for (int i = 0; i < DRIVER_COUNT; i++) {
            Driver driver = new Driver(i, randomLocation(random));
            drivers.add(driver);
            grid.insert(driver);
        }

        System.out.printf("%-10s %-18s %-20s%n", "Threads", "Total time (ms)", "Throughput (matches/sec)");
        System.out.println("-".repeat(55));

        for (int threadCount : THREAD_COUNTS) {
            drivers.forEach(Driver::release);

            List<RideRequest> requests = generateRequests(REQUESTS_PER_TRIAL, random);

            double throughput = runTrial(grid, requests, threadCount);
            long totalMs = Math.round(REQUESTS_PER_TRIAL / throughput * 1000);

            System.out.printf("%-10d %-18d %-20.1f%n", threadCount, totalMs, throughput);
        }
    }

    private static double runTrial(SpatialGrid grid, List<RideRequest> requests, int threadCount)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Driver>> futures = new ArrayList<>();

        long start = System.nanoTime();
        for (RideRequest request : requests) {
            futures.add(executor.submit(() -> grid.matchAndAssign(request)));
        }

        for (Future<Driver> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Match task failed", e);
            }
        }
        long elapsedNanos = System.nanoTime() - start;

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        return requests.size() / elapsedSeconds;
    }

    private static List<RideRequest> generateRequests(int count, Random random) {
        List<RideRequest> requests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Location pickup = randomLocation(random);
            requests.add(new RideRequest(i, pickup, pickup));
        }
        return requests;
    }

    private static Location randomLocation(Random random) {
        double lat = LAT_MIN + random.nextDouble() * (LAT_MAX - LAT_MIN);
        double lon = LON_MIN + random.nextDouble() * (LON_MAX - LON_MIN);
        return new Location(lat, lon);
    }
}