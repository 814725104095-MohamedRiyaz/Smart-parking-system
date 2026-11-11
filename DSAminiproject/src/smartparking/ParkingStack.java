package smartparking;

import java.util.ArrayDeque;
import java.util.Deque;

public class ParkingStack {
    private final Deque<ParkingRecord> records = new ArrayDeque<>();

    public synchronized void push(ParkingRecord record) { records.push(record); }
    public synchronized ParkingRecord pop() { return records.pollFirst(); }
    public synchronized ParkingRecord peek() { return records.peekFirst(); }
}
