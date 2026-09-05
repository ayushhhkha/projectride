public class Location {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final double latitude;
    private final double longitude;

    public Location(double latitude, double longitude) {

        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90 degrees, got: " + latitude);
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180 degrees, got: " + longitude);
        }

        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Calculates the great-circle distance between this location and
     * another, in kilometers, using the Haversine formula.
     *
     * This replaces the earlier Euclidean-style calculation
     * (sqrt(latDiff^2 + lonDiff^2)), which treated latitude/longitude
     * as flat Cartesian coordinates. That approach ignores the fact
     * that the Earth is a sphere and that a degree of longitude covers
     * less real-world distance as you move away from the equator, so
     * it becomes increasingly inaccurate over longer distances or at
     * higher latitudes. The Haversine formula corrects for this.
     */
    public double distanceTo(Location other) {

        double latitudeOneRadians =
                Math.toRadians(this.latitude);

        double latitudeTwoRadians =
                Math.toRadians(other.latitude);

        double latitudeDifferenceRadians =
                Math.toRadians(other.latitude - this.latitude);

        double longitudeDifferenceRadians =
                Math.toRadians(other.longitude - this.longitude);

        double a =
                Math.sin(latitudeDifferenceRadians / 2) * Math.sin(latitudeDifferenceRadians / 2)
                + Math.cos(latitudeOneRadians) * Math.cos(latitudeTwoRadians)
                * Math.sin(longitudeDifferenceRadians / 2) * Math.sin(longitudeDifferenceRadians / 2);

        double c =
                2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}