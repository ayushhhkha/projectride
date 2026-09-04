public class RideRequest {

    private final int id;
    private final Location pickup;
    private final Location destination;

    public RideRequest(
            int id,
            Location pickup,
            Location destination) {

        this.id = id;
        this.pickup = pickup;
        this.destination = destination;
    }

    public int getId() {
        return id;
    }

    public Location getPickup() {
        return pickup;
    }

    public Location getDestination() {
        return destination;
    }
}