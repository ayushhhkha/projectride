import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Benchmark {

    private static final double LAT_MIN = 52.00;
    private static final double LAT_MAX = 52.50;
    private static final double LON_MIN = 4.20;
    private static final double LON_MAX = 5.00;
    private static final double CELL_SIZE_KM = 2.0;

    private static final int[] DRIVER_COUNTS = {100, 1_000, 10_000, 100_000, 500_000};
    private static final int REQUESTS_PER_RUN = 500;

    public static void main(String[] args) {
        Random random = new Random(7);

        System.out.printf("%-12s %-22s %-22s %-10s%n",
                "Drivers", "Linear scan", "Spatial grid", "Speedup");
        System.out.println("-".repeat(70));

        for (int driverCount : DRIVER_COUNTS) {
            List<Driver> drivers = generateDrivers(driverCount, random);

            MatchingEngine linearEngine = new MatchingEngine();

            SpatialGrid grid = new SpatialGrid(CELL_SIZE_KM, (LAT_MIN + LAT_MAX) / 2);
            for (Driver driver : drivers) {
                grid.insert(driver);
            }

            List<RideRequest> requests = generateRequests(REQUESTS_PER_RUN, random);

            double linearAvgMicros = timeLinearScan(linearEngine, drivers, requests);
            double gridAvgMicros = timeGridSearch(grid, requests);

            System.out.printf("%-12d %-22.2f %-22.2f %-10.1fx%n",
                    driverCount, linearAvgMicros, gridAvgMicros,
                    linearAvgMicros / gridAvgMicros);
        }
    }

    private static double timeLinearScan(MatchingEngine engine, List<Driver> drivers,
                                          List<RideRequest> requests) {
        for (RideRequest r : requests) {
            engine.findNearestDriver(r, drivers);
        }

        long start = System.nanoTime();
        for (RideRequest r : requests) {
            engine.findNearestDriver(r, drivers);
        }
        long elapsedNanos = System.nanoTime() - start;

        return (elapsedNanos / 1000.0) / requests.size();
    }

    private static double timeGridSearch(SpatialGrid grid, List<RideRequest> requests) {
        for (RideRequest r : requests) {
            grid.findNearestDriver(r);
        }

        long start = System.nanoTime();
        for (RideRequest r : requests) {
            grid.findNearestDriver(r);
        }
        long elapsedNanos = System.nanoTime() - start;

        return (elapsedNanos / 1000.0) / requests.size();
    }

    private static List<Driver> generateDrivers(int count, Random random) {
        List<Driver> drivers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Location loc = randomLocation(random);
            Driver driver = new Driver(i, loc);
            driver.setAvailable(random.nextDouble() < 0.8);
            drivers.add(driver);
        }
        return drivers;
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