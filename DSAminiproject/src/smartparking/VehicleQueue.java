package smartparking;

import java.util.ArrayDeque;
import java.util.Queue;

public class VehicleQueue {
    private final Queue<Vehicle> vehicles = new ArrayDeque<>();

    public void add(Vehicle vehicle) { vehicles.offer(vehicle); }
    public Vehicle remove() { return vehicles.poll(); }
    public int size() { return vehicles.size(); }
    public void clear() { vehicles.clear(); }
}
