import java.util.concurrent.atomic.AtomicBoolean;

public class Driver {

    private final int id;
    private volatile Location location;
    private final AtomicBoolean available;

    public Driver(int id, Location location) {
        this.id = id;
        this.location = location;
        this.available = new AtomicBoolean(true);
    }

    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isAvailable() {
        return available.get();
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public void setAvailable(boolean available) {
        this.available.set(available);
    }

    public boolean tryAssign() {
        return available.compareAndSet(true, false);
    }

    public void release() {
        available.set(true);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", location=" + location +
                ", available=" + available.get() +
                '}';
    }
}