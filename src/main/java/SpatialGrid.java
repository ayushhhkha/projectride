import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialGrid {

    private static final double KM_PER_DEGREE_LATITUDE = 111.0;
    private static final int MAX_RING_SEARCH = 1000;
    private static final int MAX_RETRIES = 100;

    private final double cellSizeKm;
    private final double latDegreesPerCell;
    private final double lonDegreesPerCell;

    private final Map<CellKey, Set<Driver>> cells = new ConcurrentHashMap<>();
    private final Map<Integer, CellKey> driverCells = new ConcurrentHashMap<>();

    public SpatialGrid(double cellSizeKm, double referenceLatitude) {
        if (cellSizeKm <= 0) {
            throw new IllegalArgumentException("cellSizeKm must be positive");
        }

        this.cellSizeKm = cellSizeKm;
        this.latDegreesPerCell = cellSizeKm / KM_PER_DEGREE_LATITUDE;

        double cosLat = Math.max(Math.cos(Math.toRadians(referenceLatitude)), 0.01);
        this.lonDegreesPerCell = cellSizeKm / (KM_PER_DEGREE_LATITUDE * cosLat);
    }

    private CellKey cellFor(Location location) {
        int row = (int) Math.floor(location.getLatitude() / latDegreesPerCell);
        int col = (int) Math.floor(location.getLongitude() / lonDegreesPerCell);
        return new CellKey(row, col);
    }

    public void insert(Driver driver) {
        synchronized (driver) {
            CellKey key = cellFor(driver.getLocation());
            cells.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(driver);
            driverCells.put(driver.getId(), key);
        }
    }

    public void remove(Driver driver) {
        synchronized (driver) {
            CellKey key = driverCells.remove(driver.getId());
            if (key != null) {
                Set<Driver> cellDrivers = cells.get(key);
                if (cellDrivers != null) {
                    cellDrivers.remove(driver);
                }
            }
        }
    }

    public void updateLocation(Driver driver, Location newLocation) {
        synchronized (driver) {
            remove(driver);
            driver.updateLocation(newLocation);
            insert(driver);
        }
    }

    public Driver findNearestDriver(RideRequest request) {
        return findNearestDriver(request, Double.MAX_VALUE);
    }


    public Driver findNearestDriver(RideRequest request, double maxDistanceKm) {
        Location pickup = request.getPickup();

        Driver bestDriver = null;
        double bestDistance = Double.MAX_VALUE;

        int ringCap = (maxDistanceKm == Double.MAX_VALUE)
                ? MAX_RING_SEARCH
                : (int) Math.ceil(maxDistanceKm / cellSizeKm) + 1;
        ringCap = Math.min(ringCap, MAX_RING_SEARCH);

        int totalDriversInGrid = size();

        for (int ring = 0; ring <= ringCap; ring++) {
            List<Driver> candidates = driversInRing(pickup, ring);

            for (Driver driver : candidates) {
                if (!driver.isAvailable()) {
                    continue;
                }
                double distance = driver.getLocation().distanceTo(pickup);
                if (distance > maxDistanceKm) {
                    continue;
                }
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestDriver = driver;
                }
            }

            double guaranteedLowerBoundForNextRing = Math.max(0, ring) * cellSizeKm;

            if (bestDriver != null && bestDistance <= guaranteedLowerBoundForNextRing) {
                break;
            }

            if (candidates.size() >= totalDriversInGrid) {
                break;
            }
        }

        return bestDriver;
    }

    public Driver matchAndAssign(RideRequest request) {
        return matchAndAssign(request, Double.MAX_VALUE);
    }

    public Driver matchAndAssign(RideRequest request, double maxDistanceKm) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Driver candidate = findNearestDriver(request, maxDistanceKm);

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

    private List<Driver> driversInRing(Location pickup, int ringRadius) {
        CellKey center = cellFor(pickup);
        List<Driver> result = new ArrayList<>();

        for (int dRow = -ringRadius; dRow <= ringRadius; dRow++) {
            for (int dCol = -ringRadius; dCol <= ringRadius; dCol++) {
                CellKey key = new CellKey(center.row + dRow, center.col + dCol);
                Set<Driver> cellDrivers = cells.get(key);
                if (cellDrivers != null) {
                    result.addAll(cellDrivers);
                }
            }
        }
        return result;
    }

    public double getCellSizeKm() {
        return cellSizeKm;
    }

    public int size() {
        return driverCells.size();
    }

    private static final class CellKey {
        final int row;
        final int col;

        CellKey(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CellKey)) return false;
            CellKey cellKey = (CellKey) o;
            return row == cellKey.row && col == cellKey.col;
        }

        @Override
        public int hashCode() {
            return 31 * row + col;
        }
    }
}