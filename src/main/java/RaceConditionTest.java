import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicInteger;

public class RaceConditionTest {

    private static final int NUM_DRIVERS = 50;
    private static final int NUM_REQUESTS = 300;
    private static final int THREAD_POOL_SIZE = 16;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Condition 1: naive check then act");
        runNaiveScenario();

        System.out.println();
        System.out.println("Condition 2a: ConcurrentMatchingEngine");
        runSafeLinearScenario();

        System.out.println();
        System.out.println("Condition 2b: SpatialGrid.matchAndAssign");
        runSafeGridScenario();
    }

    private static void runNaiveScenario() throws InterruptedException {
        List<Driver> drivers = clusteredDrivers();
        MatchingEngine naiveEngine = new MatchingEngine();

        List<Integer> assignedIds = runConcurrently(drivers, (request) -> {
            Driver found = naiveEngine.findNearestDriver(request, drivers);
            if (found == null) {
                return -1;
            }

            sleepMillis(1);

            found.setAvailable(false);
            return found.getId();
        });

        reportDuplicates("Naive engine", assignedIds, false);
    }

    private static void runSafeLinearScenario() throws InterruptedException {
        List<Driver> drivers = clusteredDrivers();
        ConcurrentMatchingEngine safeEngine = new ConcurrentMatchingEngine();

        List<Integer> assignedIds = runConcurrently(drivers, (request) -> {
            Driver assigned = safeEngine.matchAndAssign(request, drivers);
            return assigned == null ? -1 : assigned.getId();
        });

        reportDuplicates("ConcurrentMatchingEngine", assignedIds, true);
    }

    private static void runSafeGridScenario() throws InterruptedException {
        List<Driver> drivers = clusteredDrivers();
        SpatialGrid grid = new SpatialGrid(1.0, 52.07);
        drivers.forEach(grid::insert);

        List<Integer> assignedIds = runConcurrently(drivers, (request) -> {
            Driver assigned = grid.matchAndAssign(request);
            return assigned == null ? -1 : assigned.getId();
        });

        reportDuplicates("SpatialGrid.matchAndAssign", assignedIds, true);
    }

    private interface MatchFunction {
        int match(RideRequest request);
    }

    private static List<Integer> runConcurrently(List<Driver> drivers, MatchFunction matcher)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_REQUESTS; i++) {
            RideRequest request = new RideRequest(i,
                    new Location(52.0700, 4.3000),
                    new Location(52.0900, 4.3200));

            futures.add(executor.submit(() -> {
                startGate.await();
                return matcher.match(request);
            }));
        }

        startGate.countDown(); 

        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                throw new RuntimeException("Task failed", e);
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        return results;
    }

    private static void reportDuplicates(String label, List<Integer> assignedIds, boolean mustBeZero) {
        Map<Integer, Integer> counts = new HashMap<>();
        int successfulMatches = 0;

        for (int id : assignedIds) {
            if (id == -1) {
                continue;
            }
            successfulMatches++;
            counts.merge(id, 1, Integer::sum);
        }

        int duplicateDrivers = 0;
        int extraAssignments = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicateDrivers++;
                extraAssignments += entry.getValue() - 1;
                System.out.println("  Driver " + entry.getKey() + " was assigned "
                        + entry.getValue() + " times simultaneously");
            }
        }

        System.out.println(label + ": " + NUM_REQUESTS + " requests, " + NUM_DRIVERS
                + " drivers, " + successfulMatches + " successful matches, "
                + duplicateDrivers + " drivers double-booked ("
                + extraAssignments + " extra/incorrect assignments)");

        if (mustBeZero && duplicateDrivers > 0) {
            System.out.println("  Failed: expected 0 double booking with the atomic claim, "
                    + "but found " + duplicateDrivers);
            System.exit(1);
        } else if (mustBeZero) {
            System.out.println("  Passed: zero double booking, as guaranteed by tryAssign()");
        } else if (duplicateDrivers > 0) {
            System.out.println("  Race condition reproduced, as expected for the naive engine.");
        } else {
            System.out.println("  Note: no duplicates this run. The race is timing-dependent; "
                    + "re-run if you want to see it reproduce (checkthe readme.md).");
        }
    }

    private static List<Driver> clusteredDrivers() {
        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < NUM_DRIVERS; i++) {
            double jitter = i * 0.0005;
            drivers.add(new Driver(i, new Location(52.0700 + jitter, 4.3000 + jitter)));
        }
        return drivers;
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}