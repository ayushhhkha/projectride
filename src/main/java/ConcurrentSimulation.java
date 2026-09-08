import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentSimulation {

    private static final int NUM_DRIVERS = 200;
    private static final int NUM_LOCATION_UPDATES = 2_000;
    private static final int NUM_RIDE_REQUESTS = 1_000;
    private static final int THREAD_POOL_SIZE = 16;

    private static final double LAT_MIN = 52.00;
    private static final double LAT_MAX = 52.20;
    private static final double LON_MIN = 4.20;
    private static final double LON_MAX = 4.50;

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random(1);

        List<Driver> drivers = new ArrayList<>();
        SpatialGrid grid = new SpatialGrid(1.0, (LAT_MIN + LAT_MAX) / 2);

        for (int i = 0; i < NUM_DRIVERS; i++) {
            Driver driver = new Driver(i, randomLocation(random));
            drivers.add(driver);
            grid.insert(driver);
        }

        AtomicInteger locationUpdatesCompleted = new AtomicInteger(0);
        AtomicInteger matchesSucceeded = new AtomicInteger(0);
        AtomicInteger matchesFailed = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        for (int i = 0; i < NUM_LOCATION_UPDATES; i++) {
            executor.submit(() -> {
                Driver driver = drivers.get(ThreadLocalRandomLite.nextInt(drivers.size()));
                Location newLocation = jitter(driver.getLocation());
                grid.updateLocation(driver, newLocation);
                locationUpdatesCompleted.incrementAndGet();
            });
        }

        for (int i = 0; i < NUM_RIDE_REQUESTS; i++) {
            final int requestId = i;
            executor.submit(() -> {
                Location pickup = randomLocation(ThreadLocalRandomLite.random());
                RideRequest request = new RideRequest(requestId, pickup, pickup);

                Driver assigned = grid.matchAndAssign(request);
                if (assigned == null) {
                    matchesFailed.incrementAndGet();
                    return;
                }

                matchesSucceeded.incrementAndGet();

                sleepMillis(2);
                assigned.release();
            });
        }

        long start = System.nanoTime();
        executor.shutdown();
        boolean finishedCleanly = executor.awaitTermination(30, TimeUnit.SECONDS);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        System.out.println("=== Concurrent Simulation Results ===");
        System.out.println("Drivers:                  " + NUM_DRIVERS);
        System.out.println("Thread pool size:         " + THREAD_POOL_SIZE);
        System.out.println("Location updates run:     " + locationUpdatesCompleted.get()
                + " / " + NUM_LOCATION_UPDATES);
        System.out.println("Ride requests matched:    " + matchesSucceeded.get());
        System.out.println("Ride requests unmatched:  " + matchesFailed.get()
                + "  (no available driver at that instant)");
        System.out.println("Total wall-clock time:    " + elapsedMillis + " ms");
        System.out.println("Executor shut down cleanly within timeout: " + finishedCleanly);

        if (!finishedCleanly) {
            System.out.println("WARNING: executor did not terminate within the timeout. "
                    + "This would indicate a deadlock or livelock and needs investigation.");
            System.exit(1);
        }

        long availableAtEnd = drivers.stream().filter(Driver::isAvailable).count();
        System.out.println("Drivers available at end: " + availableAtEnd + " / " + NUM_DRIVERS);
    }

    private static Location randomLocation(Random random) {
        double lat = LAT_MIN + random.nextDouble() * (LAT_MAX - LAT_MIN);
        double lon = LON_MIN + random.nextDouble() * (LON_MAX - LON_MIN);
        return new Location(lat, lon);
    }

    private static Location jitter(Location location) {
        double newLat = clamp(location.getLatitude() + (Math.random() - 0.5) * 0.01, LAT_MIN, LAT_MAX);
        double newLon = clamp(location.getLongitude() + (Math.random() - 0.5) * 0.01, LON_MIN, LON_MAX);
        return new Location(newLat, newLon);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ThreadLocalRandomLite {
        static Random random() {
            return java.util.concurrent.ThreadLocalRandom.current();
        }

        static int nextInt(int bound) {
            return java.util.concurrent.ThreadLocalRandom.current().nextInt(bound);
        }
    }
}