import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GridVsLinearScanTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Random random = new Random(42);

        runScenario("This is for the scenario dense placement so this has 500 drivers x 300 requests",
                500, 300, 0.9, random, 52.00, 52.20, 4.20, 4.50);

        runScenario("This is for the scenario sparse placement so this has 30 drivers x 300 requests",
                30, 300, 0.7, random, 52.00, 52.50, 4.00, 5.00);

        runScenario("This is for the scenario very sparse placement so this has 8 drivers x 300 requests",
                8, 300, 0.5, random, 51.80, 52.60, 3.80, 5.20);

        runScenario("This is for the scenario many unavailable drivers so this has 400 drivers x 200 requests",
                400, 200, 0.15, random, 52.00, 52.20, 4.20, 4.50);

        runMaxDistanceScenario();
        runBoundaryCase();
        runNoDriversAvailableCase();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runScenario(String label, int driverCount, int requestCount,
                                     double availabilityRate, Random random,
                                     double latMin, double latMax,
                                     double lonMin, double lonMax) {
        List<Driver> drivers = new ArrayList<>();
        
        for (int i = 0; i < driverCount; i++) {
            Location loc = randomLocation(random, latMin, latMax, lonMin, lonMax);
            Driver driver = new Driver(i, loc);
            driver.setAvailable(random.nextDouble() < availabilityRate);
            drivers.add(driver);
        }

        MatchingEngine linear = new MatchingEngine();

        SpatialGrid grid = new SpatialGrid(1.0, (latMin + latMax) / 2);
        drivers.forEach(grid::insert);

        int mismatches = 0;
        for (int i = 0; i < requestCount; i++) {
            Location pickup = randomLocation(random, latMin, latMax, lonMin, lonMax);
            RideRequest request = new RideRequest(i, pickup, pickup);

            Driver expected = linear.findNearestDriver(request, drivers);
            Driver actual = grid.findNearestDriver(request);

            if (!sameOutcome(expected, actual, pickup)) {
                mismatches++;
                if (mismatches <= 3) {
                    System.out.println("  mismatch: expected="
                            + (expected == null ? "none" : expected.getId())
                            + " actual=" + (actual == null ? "none" : actual.getId())
                            + " pickup=" + pickup);
                }
            }
        }

        check(label, mismatches == 0, mismatches + " mismatches out of " + requestCount + " requests");
    }

    private static void runMaxDistanceScenario() {
        List<Driver> drivers = List.of(
                new Driver(1, new Location(52.0700, 4.3000)),
                new Driver(2, new Location(53.5000, 6.0000)) 
        );

        RideRequest request = new RideRequest(1,
                new Location(52.0710, 4.3010),
                new Location(52.0900, 4.3200));

        MatchingEngine linear = new MatchingEngine();
        SpatialGrid grid = new SpatialGrid(1.0, 52.5);
        drivers.forEach(grid::insert);

        Driver expected = linear.findNearestDriver(request, drivers, 10.0);
        Driver actual = grid.findNearestDriver(request, 10.0);

        check("Max-distance cutoff excludes far driver (grid matches linear)",
                expected != null && actual != null && expected.getId() == actual.getId(),
                "expected=" + (expected == null ? "none" : expected.getId())
                        + " actual=" + (actual == null ? "none" : actual.getId()));

        Driver expectedNone = linear.findNearestDriver(request, drivers, 0.001);
        Driver actualNone = grid.findNearestDriver(request, 0.001);

        check("Max-distance cutoff excludes ALL drivers when too strict",
                expectedNone == null && actualNone == null,
                "expected=" + expectedNone + " actual=" + actualNone);
    }

    
    private static void runBoundaryCase() {
        SpatialGrid grid = new SpatialGrid(1.0, 52.0); // ~1km cells

        Driver farInSameCell = new Driver(1, new Location(52.0089, 4.0089));
        Driver nearAcrossBoundary = new Driver(2, new Location(52.0091, 4.0091));

        List<Driver> drivers = List.of(farInSameCell, nearAcrossBoundary);
        grid.insert(farInSameCell);
        grid.insert(nearAcrossBoundary);

        Location pickup = new Location(52.0090, 4.0090);
        RideRequest request = new RideRequest(1, pickup, pickup);

        MatchingEngine linear = new MatchingEngine();
        Driver expected = linear.findNearestDriver(request, drivers);
        Driver actual = grid.findNearestDriver(request);

        check("Cell-boundary case picks the true nearest driver",
                expected != null && actual != null && expected.getId() == actual.getId(),
                "expected=" + (expected == null ? "none" : expected.getId())
                        + " actual=" + (actual == null ? "none" : actual.getId()));
    }

    private static void runNoDriversAvailableCase() {
        Driver unavailable = new Driver(1, new Location(52.0700, 4.3000));
        unavailable.setAvailable(false);
        List<Driver> drivers = List.of(unavailable);

        RideRequest request = new RideRequest(1,
                new Location(52.0700, 4.3000),
                new Location(52.0900, 4.3200));

        MatchingEngine linear = new MatchingEngine();
        SpatialGrid grid = new SpatialGrid(1.0, 52.07);
        drivers.forEach(grid::insert);

        Driver expected = linear.findNearestDriver(request, drivers);
        Driver actual = grid.findNearestDriver(request);

        check("No available drivers returns null on both engines",
                expected == null && actual == null,
                "expected=" + expected + " actual=" + actual);
    }

    private static boolean sameOutcome(Driver expected, Driver actual, Location pickup) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.getId() == actual.getId()) {
            return true;
        }
        double expectedDist = expected.getLocation().distanceTo(pickup);
        double actualDist = actual.getLocation().distanceTo(pickup);
        return Math.abs(expectedDist - actualDist) < 1e-9;
    }

    private static Location randomLocation(Random random, double latMin, double latMax,
                                            double lonMin, double lonMax) {
        double lat = latMin + random.nextDouble() * (latMax - latMin);
        double lon = lonMin + random.nextDouble() * (lonMax - lonMin);
        return new Location(lat, lon);
    }

    private static void check(String label, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("Pass  " + label + (detail.isEmpty() ? "" : "  (" + detail + ")"));
        } else {
            failed++;
            System.out.println("Failll  " + label + "  " + detail);
        }
    }
}