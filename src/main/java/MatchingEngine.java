import java.util.List;

public class MatchingEngine {

    public Driver findNearestDriver(
            RideRequest request,
            List<Driver> drivers) {

        Driver nearestDriver = null;
        double shortestDistance = Double.MAX_VALUE;

        for (Driver driver : drivers) {

            if (!driver.isAvailable()) {
                continue;
            }

            double distance = calculateDistance(
                    request.getPickup(),
                    driver.getLocation()
            );

            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearestDriver = driver;
            }
        }

        return nearestDriver;
    }

    private double calculateDistance(
            Location a,
            Location b) {

        double latitudeDifference =
                a.getLatitude() - b.getLatitude();

        double longitudeDifference =
                a.getLongitude() - b.getLongitude();

        return Math.sqrt(
                latitudeDifference * latitudeDifference
                + longitudeDifference * longitudeDifference
        );
    }
}