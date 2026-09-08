import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CellSizeTuningBenchmark {

    private static final int DRIVER_COUNT = 300_000;
    private static final int REQUESTS_PER_TRIAL = 500;
    private static final double[] CELL_SIZES_KM = {0.5, 1.0, 2.0, 4.0, 8.0, 16.0};

    private static final double LAT_MIN = 52.00;
    private static final double LAT_MAX = 52.50;
    private static final double LON_MIN = 4.20;
    private static final double LON_MAX = 5.00;

    public static void main(String[] args) {
        Random random = new Random(23);

        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < DRIVER_COUNT; i++) {
            drivers.add(new Driver(i, randomLocation(random)));
        }

        System.out.println("Driver count: " + DRIVER_COUNT
                + " over a ~" + Math.round((LAT_MAX - LAT_MIN) * 111) + "km x "
                + Math.round((LON_MAX - LON_MIN) * 70) + "km area");
        System.out.printf("%-14s %-16s %-22s%n", "Cell size(km)", "Avg cell pop.", "Avg latency (us)");
        System.out.println("-".repeat(55));

        for (double cellSize : CELL_SIZES_KM) {
            SpatialGrid grid = new SpatialGrid(cellSize, (LAT_MIN + LAT_MAX) / 2);
            drivers.forEach(driver -> {
                driver.release();
                grid.insert(driver);
            });

            List<RideRequest> requests = generateRequests(REQUESTS_PER_TRIAL, random);

            for (RideRequest r : requests) {
                grid.findNearestDriver(r);
            }

            long start = System.nanoTime();
            for (RideRequest r : requests) {
                grid.findNearestDriver(r);
            }
            long elapsedNanos = System.nanoTime() - start;
            double avgMicros = (elapsedNanos / 1000.0) / requests.size();

            double areaKm2 = ((LAT_MAX - LAT_MIN) * 111.0) * ((LON_MAX - LON_MIN) * 70.0);
            double driversPerKm2 = DRIVER_COUNT / areaKm2;
            double avgDriversPerCell = driversPerKm2 * cellSize * cellSize;

            System.out.printf("%-14.1f %-16.1f %-22.2f%n", cellSize, avgDriversPerCell, avgMicros);
        }
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