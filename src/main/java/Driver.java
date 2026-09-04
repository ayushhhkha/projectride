public class Driver {

    private final int id;
    private Location location;
    private boolean available;

    public Driver(int id, Location location) {
        this.id = id;
        this.location = location;
        this.available = true;
    }

    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isAvailable() {
        return available;
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}