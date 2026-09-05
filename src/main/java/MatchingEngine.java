import java.util.List;

public class MatchingEngine {

    public Driver findNearestDriver(
            RideRequest request,
            List<Driver> drivers) {

        return findNearestDriver(request, drivers, Double.MAX_VALUE);
    }

    /**
     
     * @param maxDistanceKm maximum acceptable distance in kilometers pass Double.MAX_VALUE for no limit
     */
    public Driver findNearestDriver(
            RideRequest request,
            List<Driver> drivers,
            double maxDistanceKm) {

        Driver nearestDriver = null;
        double shortestDistance = Double.MAX_VALUE;

        for (Driver driver : drivers) {

            if (!driver.isAvailable()) {
                continue;
            }

            double distance =
                    driver.getLocation().distanceTo(request.getPickup());

            if (distance > maxDistanceKm) {
                continue;
            }

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestDriver = driver;
            }
        }

        return nearestDriver;
    }
}