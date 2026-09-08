import java.util.List;

public class ConcurrentMatchingEngine {

    private static final int MAX_RETRIES = 100;

    public Driver matchAndAssign(RideRequest request, List<Driver> drivers) {
        return matchAndAssign(request, drivers, Double.MAX_VALUE);
    }

    public Driver matchAndAssign(RideRequest request, List<Driver> drivers, double maxDistanceKm) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Driver candidate = findNearestAvailable(request, drivers, maxDistanceKm);

            if (candidate == null) {
                return null; 
            }

            if (candidate.tryAssign()) {
                return candidate; 
            }

        }

        throw new IllegalStateException(
                "Could not assign a driver after " + MAX_RETRIES
                        + " attempts. This suggests extreme contention over "
                        + "a very small pool of available drivers.");
    }

    private Driver findNearestAvailable(RideRequest request, List<Driver> drivers, double maxDistanceKm) {
        Driver nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Driver driver : drivers) {
            if (!driver.isAvailable()) {
                continue;
            }

            double distance = driver.getLocation().distanceTo(request.getPickup());

            if (distance > maxDistanceKm) {
                continue;
            }

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = driver;
            }
        }

        return nearest;
    }
}