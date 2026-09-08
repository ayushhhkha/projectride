import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Loadtest {

    private static final int DRIVER_COUNT = 100;
    private static final int THREAD_POOL_SIZE = 300;
    private static final long TEST_DURATION_MILLIS = 5_000;
    private static final long SIMULATED_RIDE_DURATION_MILLIS = 50;
    private static final int MAX_IN_FLIGHT_REQUESTS = 1000;

    private static final double LAT_MIN = 52.00;
    private static final double LAT_MAX = 52.30;
    private static final double LON_MIN = 4.20;
    private static final double LON_MAX = 4.60;

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random(3);

        List<Driver> drivers = new ArrayList<>();
        SpatialGrid grid = new SpatialGrid(1.0, (LAT_MIN + LAT_MAX) / 2);
        for (int i = 0; i < DRIVER_COUNT; i++) {
            Driver driver = new Driver(i, randomLocation(random));
            drivers.add(driver);
            grid.insert(driver);
        }

        AtomicInteger requestsSubmitted = new AtomicInteger(0);
        AtomicInteger matchesSucceeded = new AtomicInteger(0);
        AtomicInteger matchesFailed = new AtomicInteger(0);
        AtomicLong totalMatchLatencyNanos = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        java.util.concurrent.Semaphore inFlight = new java.util.concurrent.Semaphore(MAX_IN_FLIGHT_REQUESTS);

        System.out.println("Running sustained load for " + (TEST_DURATION_MILLIS / 1000) + " seconds...");
        long testStart = System.currentTimeMillis();
        long testEnd = testStart + TEST_DURATION_MILLIS;

        while (System.currentTimeMillis() < testEnd) {
            inFlight.acquire();
            int requestId = requestsSubmitted.incrementAndGet();
            Location pickup = randomLocation(ThreadLocalRandomHolder.random());
            RideRequest request = new RideRequest(requestId, pickup, pickup);

            executor.submit(() -> {
                try {
                    long matchStart = System.nanoTime();
                    Driver assigned = grid.matchAndAssign(request);
                    long matchElapsed = System.nanoTime() - matchStart;

                    if (assigned == null) {
                        matchesFailed.incrementAndGet();
                        return;
                    }

                    matchesSucceeded.incrementAndGet();
                    totalMatchLatencyNanos.addAndGet(matchElapsed);

                    sleepMillis(SIMULATED_RIDE_DURATION_MILLIS);
                    assigned.release();
                } finally {
                    inFlight.release();
                }
            });
        }

        executor.shutdown();
        boolean finishedCleanly = executor.awaitTermination(30, TimeUnit.SECONDS);
        long actualDurationMillis = System.currentTimeMillis() - testStart;

        int totalCompleted = matchesSucceeded.get() + matchesFailed.get();
        double avgLatencyMicros = totalCompleted == 0 ? 0
                : (totalMatchLatencyNanos.get() / 1000.0) / matchesSucceeded.get();
        double throughputPerSecond = totalCompleted / (actualDurationMillis / 1000.0);

        System.out.println();
        System.out.println("=== Load Test Results ===");
        System.out.println("Driver pool size:" + DRIVER_COUNT);
        System.out.println("Thread pool size:" + THREAD_POOL_SIZE);
        System.out.println("Actual test duration: " + actualDurationMillis + " ms");
        System.out.println("Requests submitted: " + requestsSubmitted.get());
        System.out.println("Requests completed: " + totalCompleted);
        System.out.println("Successful matches: " + matchesSucceeded.get());
        System.out.println("Failed (no driver available): " + matchesFailed.get()
                + "  (" + String.format("%.1f", 100.0 * matchesFailed.get() / Math.max(1, totalCompleted)) + "%)");
        System.out.println("Average successful-match latency: " + String.format("%.2f", avgLatencyMicros) + " us");
        System.out.println("Sustained throughput: " + String.format("%.1f", throughputPerSecond) + " requests/sec");
        System.out.println("Executor shut down cleanly: " + finishedCleanly);

        if (!finishedCleanly) {
            System.out.println("error error possible deadlock/livelock.");
            System.exit(1);
        }
    }

    private static Location randomLocation(Random random) {
        double lat = LAT_MIN + random.nextDouble() * (LAT_MAX - LAT_MIN);
        double lon = LON_MIN + random.nextDouble() * (LON_MAX - LON_MIN);
        return new Location(lat, lon);
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ThreadLocalRandomHolder {
        static Random random() {
            return java.util.concurrent.ThreadLocalRandom.current();
        }
    }
}