package smartparking;

import java.time.LocalDateTime;

public class ParkingRecord {
    private final String vehicleNumber;
    private final String owner;
    private final String type;
    private final String slotNumber;
    private final LocalDateTime entryTime;
    private final LocalDateTime exitTime;
    private final double fee;

    public ParkingRecord(String vehicleNumber, String owner, String type, String slotNumber,
                         LocalDateTime entryTime, LocalDateTime exitTime, double fee) {
        this.vehicleNumber = vehicleNumber;
        this.owner = owner;
        this.type = type;
        this.slotNumber = slotNumber;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.fee = fee;
    }

    public String getVehicleNumber() { return vehicleNumber; }
    public String getOwner() { return owner; }
    public String getType() { return type; }
    public String getSlotNumber() { return slotNumber; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public double getFee() { return fee; }
}
