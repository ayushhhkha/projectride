import java.util.List;

public class Main {

    public static void main(String[] args) {

        Driver driver1 = new Driver(
                1,
                new Location(52.0700, 4.3000)
        );

        Driver driver2 = new Driver(
                2,
                new Location(52.0800, 4.3100)
        );

        Driver driver3 = new Driver(
                3,
                new Location(52.1000, 4.3300)
        );

        List<Driver> drivers =
                List.of(driver1, driver2, driver3);

        RideRequest request = new RideRequest(
                1,
                new Location(52.0710, 4.3010),
                new Location(52.0900, 4.3200)
        );

        // Baseline: linear scan over every driver
        MatchingEngine engine = new MatchingEngine();
        Driver matched = engine.findNearestDriver(request, drivers);

        System.out.println("[Linear scan]  Matched driver: " + matched.getId());

        // Phase 3: spatial grid, only checks drivers near the pickup point
        SpatialGrid grid = new SpatialGrid(1.0, 52.07);

        for (Driver driver : drivers) {
            grid.insert(driver);
        }

        Driver gridMatched = grid.findNearestDriver(request);

        System.out.println("[Spatial grid] Matched driver: " + gridMatched.getId());
    }
}