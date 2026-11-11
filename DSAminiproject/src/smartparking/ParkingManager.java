package smartparking;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ParkingManager {
    private static final int HASH_ITERATIONS = 120_000;
    private static final int HASH_BITS = 256;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,80}$");
    private final Map<String, Vehicle> vehicleIndex = Collections.synchronizedMap(new HashMap<>());
    private final VehicleQueue waitingVehicles = new VehicleQueue();
    private final ParkingStack recentExits = new ParkingStack();
    private final LinkedList<ParkingRecord> historyRecords = new LinkedList<>();

    public void bootstrapAdmin() throws Exception {
        try (Connection connection = DBConnection.getConnection()) {
            ensureAccountSchema(connection);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM admin")) {
            result.next();
            if (result.getLong(1) > 0) {
                return;
            }
            }
            String username = System.getenv("PARKING_ADMIN_USERNAME");
            String password = System.getenv("PARKING_ADMIN_PASSWORD");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO admin (full_name, email, username, password_salt, password_hash) "
                            + "VALUES (?, NULL, ?, '', ?)")) {
                statement.setString(1, username.trim());
                statement.setString(2, username.trim());
                statement.setString(3, BCrypt.hashpw(password, BCrypt.gensalt(12)));
                statement.executeUpdate();
            }
        }
    }

    public boolean authenticate(String identifier, char[] password) throws Exception {
        String sql = "SELECT admin_id, password_salt, password_hash FROM admin "
                + "WHERE username = ? OR email = ? LIMIT 2";
        try (Connection connection = DBConnection.getConnection()) {
            ensureAccountSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                String login = identifier == null ? "" : identifier.trim();
                statement.setString(1, login);
                statement.setString(2, login);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return false;
                    long accountId = result.getLong("admin_id");
                    String saltText = result.getString("password_salt");
                    String passwordHash = result.getString("password_hash");
                    if (result.next()) return false;

                    boolean valid;
                    if (isBcryptHash(passwordHash)) {
                        valid = BCrypt.checkpw(new String(password), passwordHash);
                    } else {
                        byte[] salt = fromHex(saltText);
                        byte[] expected = fromHex(passwordHash);
                        byte[] actual = fromHex(hashPassword(password, salt));
                        valid = MessageDigest.isEqual(expected, actual);
                        if (valid) {
                            try (PreparedStatement update = connection.prepareStatement(
                                    "UPDATE admin SET password_salt = '', password_hash = ? WHERE admin_id = ?")) {
                                update.setString(1, BCrypt.hashpw(new String(password), BCrypt.gensalt(12)));
                                update.setLong(2, accountId);
                                update.executeUpdate();
                            }
                        }
                    }
                    return valid;
                }
            }
        }
    }

    public void registerAccount(String fullName, String email, String username, char[] password)
            throws SQLException {
        String name = fullName == null ? "" : fullName.trim();
        String address = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String login = username == null ? "" : username.trim();
        if (name.isEmpty() || name.length() > 120) {
            throw new IllegalArgumentException("Enter your full name (up to 120 characters).");
        }
        if (address.length() > 254 || !EMAIL_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (!USERNAME_PATTERN.matcher(login).matches()) {
            throw new IllegalArgumentException("Username must be 3–80 characters using letters, numbers, '.', '_' or '-'.");
        }
        int passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 8 || passwordBytes > 72) {
            throw new IllegalArgumentException("Password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            ensureAccountSchema(connection);
            try (PreparedStatement check = connection.prepareStatement(
                    "SELECT 1 FROM admin WHERE username = ? OR email = ? OR username = ? OR email = ? LIMIT 1")) {
                check.setString(1, login);
                check.setString(2, address);
                check.setString(3, address);
                check.setString(4, login);
                try (ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        throw new IllegalArgumentException("That username or email address is already registered.");
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO admin (full_name, email, username, password_salt, password_hash) "
                            + "VALUES (?, ?, ?, '', ?)")) {
                insert.setString(1, name);
                insert.setString(2, address);
                insert.setString(3, login);
                insert.setString(4, BCrypt.hashpw(new String(password), BCrypt.gensalt(12)));
                insert.executeUpdate();
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("That username or email address is already registered.", ex);
                }
                throw ex;
            }
        }
    }

    public String getAccountName(String identifier) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureAccountSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT full_name, username FROM admin WHERE username = ? OR email = ? LIMIT 1")) {
                String login = identifier == null ? "" : identifier.trim();
                statement.setString(1, login);
                statement.setString(2, login);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return login;
                    String name = result.getString("full_name");
                    return name == null || name.isBlank() ? result.getString("username") : name;
                }
            }
        }
    }

    private static boolean isBcryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private static void ensureAccountSchema(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, "admin", "full_name")) {
            if (!columns.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE admin ADD COLUMN full_name VARCHAR(120) NOT NULL DEFAULT ''");
                }
            }
        }
        try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, "admin", "email")) {
            if (!columns.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE admin ADD COLUMN email VARCHAR(254) NULL");
                }
            }
        }
        boolean emailIndexExists = false;
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(catalog, null, "admin", false, false)) {
            while (indexes.next()) {
                if ("uk_admin_email".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    emailIndexExists = true;
                    break;
                }
            }
        }
        if (!emailIndexExists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE UNIQUE INDEX uk_admin_email ON admin (email)");
            }
        }
    }

    public DashboardStats getDashboardStats() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM parking_slots) total, "
                + "(SELECT COUNT(*) FROM parking_slots WHERE status='AVAILABLE') available, "
                + "(SELECT COUNT(*) FROM parking_slots WHERE status='OCCUPIED') occupied, "
                + "(SELECT COUNT(*) FROM waiting_queue) waiting";
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return new DashboardStats(result.getInt("total"), result.getInt("available"),
                    result.getInt("occupied"), result.getInt("waiting"));
        }
    }

    public List<String[]> getSlots() throws SQLException {
        List<String[]> slots = new ArrayList<>();
        String sql = "SELECT s.slot_number, s.status, v.vehicle_number FROM parking_slots s "
                + "LEFT JOIN parking_records r ON r.slot_id=s.slot_id AND r.exit_time IS NULL "
                + "LEFT JOIN vehicles v ON v.vehicle_id=r.vehicle_id ORDER BY s.slot_number";
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                slots.add(new String[]{result.getString(1), result.getString(2), result.getString(3)});
            }
        }
        return slots;
    }

    public synchronized EntryResult enterVehicle(Vehicle vehicle) throws SQLException {
        validateVehicle(vehicle);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (isVehicleActive(connection, vehicle.getNumber())
                        || isVehicleWaiting(connection, vehicle.getNumber())) {
                    throw new SQLException("This vehicle is already parked or waiting.");
                }
                long slotId = findAvailableSlot(connection);
                if (slotId < 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO waiting_queue (vehicle_number, owner_name, vehicle_type) VALUES (?, ?, ?)")) {
                        statement.setString(1, vehicle.getNumber());
                        statement.setString(2, vehicle.getOwner());
                        statement.setString(3, vehicle.getType());
                        statement.executeUpdate();
                    }
                    connection.commit();
                    waitingVehicles.add(vehicle);
                    return new EntryResult(true, null);
                }
                long vehicleId = saveVehicle(connection, vehicle);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE parking_slots SET status='OCCUPIED' WHERE slot_id=?")) {
                    statement.setLong(1, slotId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO parking_records (vehicle_id, slot_id, entry_time) VALUES (?, ?, NOW())")) {
                    statement.setLong(1, vehicleId);
                    statement.setLong(2, slotId);
                    statement.executeUpdate();
                }
                String slot = slotNumber(connection, slotId);
                connection.commit();
                vehicleIndex.put(vehicle.getNumber(), vehicle);
                return new EntryResult(false, slot);
            } catch (SQLException ex) {
                rollback(connection, ex);
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public synchronized ExitResult exitVehicle(String vehicleNumber) throws SQLException {
        String number = normalizeNumber(vehicleNumber);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String sql = "SELECT r.record_id, r.vehicle_id, r.slot_id, r.entry_time, "
                        + "v.vehicle_number, v.owner_name, v.type, s.slot_number "
                        + "FROM parking_records r JOIN vehicles v ON v.vehicle_id=r.vehicle_id "
                        + "JOIN parking_slots s ON s.slot_id=r.slot_id "
                        + "WHERE v.vehicle_number=? AND r.exit_time IS NULL FOR UPDATE";
                long recordId;
                long slotId;
                LocalDateTime entryTime;
                Vehicle vehicle;
                String slot;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, number);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("No active parking record for " + number + ".");
                        recordId = result.getLong("record_id");
                        slotId = result.getLong("slot_id");
                        entryTime = result.getTimestamp("entry_time").toLocalDateTime();
                        vehicle = new Vehicle(result.getString("vehicle_number"),
                                result.getString("owner_name"), result.getString("type"));
                        slot = result.getString("slot_number");
                    }
                }
                LocalDateTime exitTime = LocalDateTime.now();
                double fee = calculateFee(entryTime, exitTime, vehicle.getType());
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE parking_records SET exit_time=?, fee=? WHERE record_id=?")) {
                    statement.setTimestamp(1, java.sql.Timestamp.valueOf(exitTime));
                    statement.setDouble(2, fee);
                    statement.setLong(3, recordId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE parking_slots SET status='AVAILABLE' WHERE slot_id=?")) {
                    statement.setLong(1, slotId);
                    statement.executeUpdate();
                }
                ParkingRecord exited = new ParkingRecord(vehicle.getNumber(), vehicle.getOwner(),
                        vehicle.getType(), slot, entryTime, exitTime, fee);
                String assigned = assignNextWaitingVehicle(connection, slotId);
                connection.commit();
                recentExits.push(exited);
                vehicleIndex.remove(vehicle.getNumber());
                if (assigned != null) waitingVehicles.remove();
                return new ExitResult(exited, assigned);
            } catch (SQLException ex) {
                rollback(connection, ex);
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public synchronized List<String[]> getWaitingVehicles() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        waitingVehicles.clear();
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT vehicle_number, owner_name, vehicle_type, queued_at FROM waiting_queue ORDER BY queue_id")) {
            int position = 1;
            while (result.next()) {
                waitingVehicles.add(new Vehicle(result.getString(1), result.getString(2), result.getString(3)));
                rows.add(new String[]{String.valueOf(position++), result.getString(1),
                        result.getString(2), result.getString(3), result.getString(4)});
            }
        }
        return rows;
    }

    public synchronized List<ParkingRecord> getHistory(String search) throws SQLException {
        LinkedList<ParkingRecord> rows = new LinkedList<>();
        String sql = "SELECT v.vehicle_number, v.owner_name, v.type, s.slot_number, r.entry_time, "
                + "r.exit_time, COALESCE(r.fee,0) fee FROM parking_records r "
                + "JOIN vehicles v ON v.vehicle_id=r.vehicle_id JOIN parking_slots s ON s.slot_id=r.slot_id "
                + "WHERE ?='' OR v.vehicle_number LIKE ? OR v.owner_name LIKE ? "
                + "ORDER BY r.entry_time DESC";
        String query = search == null ? "" : search.trim();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, query);
            statement.setString(2, "%" + query + "%");
            statement.setString(3, "%" + query + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ParkingRecord record = new ParkingRecord(result.getString("vehicle_number"),
                            result.getString("owner_name"), result.getString("type"),
                            result.getString("slot_number"), result.getTimestamp("entry_time").toLocalDateTime(),
                            result.getTimestamp("exit_time") == null ? null
                                    : result.getTimestamp("exit_time").toLocalDateTime(),
                            result.getDouble("fee"));
                    rows.add(record);
                }
            }
        }
        historyRecords.clear();
        historyRecords.addAll(rows);
        return rows;
    }

    public Vehicle findActiveVehicle(String vehicleNumber) throws SQLException {
        String number = normalizeNumber(vehicleNumber);
        Vehicle cached = vehicleIndex.get(number);
        if (cached != null) return cached;
        String sql = "SELECT v.vehicle_number, v.owner_name, v.type FROM vehicles v "
                + "JOIN parking_records r ON r.vehicle_id=v.vehicle_id "
                + "WHERE v.vehicle_number=? AND r.exit_time IS NULL";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                Vehicle vehicle = new Vehicle(result.getString(1), result.getString(2), result.getString(3));
                vehicleIndex.put(number, vehicle);
                return vehicle;
            }
        }
    }

    public ParkingRecord getMostRecentExit() {
        return recentExits.peek();
    }

    public ReportStats getReportStats() throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM parking_records WHERE DATE(entry_time)=CURRENT_DATE) today, "
                + "(SELECT COALESCE(SUM(fee),0) FROM parking_records WHERE DATE(exit_time)=CURRENT_DATE) revenue, "
                + "(SELECT COUNT(*) FROM parking_slots WHERE status='AVAILABLE') available, "
                + "(SELECT COUNT(*) FROM parking_slots WHERE status='OCCUPIED') occupied, "
                + "(SELECT COUNT(*) FROM parking_slots) total";
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return new ReportStats(result.getInt("today"), result.getDouble("revenue"),
                    result.getInt("available"), result.getInt("occupied"), result.getInt("total"));
        }
    }

    private String assignNextWaitingVehicle(Connection connection, long slotId) throws SQLException {
        String number;
        String owner;
        String type;
        long queueId;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT queue_id, vehicle_number, owner_name, vehicle_type FROM waiting_queue "
                             + "ORDER BY queue_id LIMIT 1 FOR UPDATE")) {
            if (!result.next()) return null;
            queueId = result.getLong("queue_id");
            number = result.getString("vehicle_number");
            owner = result.getString("owner_name");
            type = result.getString("vehicle_type");
        }
        Vehicle vehicle = new Vehicle(number, owner, type);
        long vehicleId = saveVehicle(connection, vehicle);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO parking_records (vehicle_id, slot_id, entry_time) VALUES (?, ?, NOW())")) {
            statement.setLong(1, vehicleId);
            statement.setLong(2, slotId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE parking_slots SET status='OCCUPIED' WHERE slot_id=?")) {
            statement.setLong(1, slotId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM waiting_queue WHERE queue_id=?")) {
            statement.setLong(1, queueId);
            statement.executeUpdate();
        }
        return number;
    }

    private long findAvailableSlot(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT slot_id FROM parking_slots WHERE status='AVAILABLE' "
                             + "ORDER BY slot_number LIMIT 1 FOR UPDATE")) {
            return result.next() ? result.getLong(1) : -1;
        }
    }

    private long saveVehicle(Connection connection, Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (vehicle_number, owner_name, type) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE owner_name=VALUES(owner_name), type=VALUES(type)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vehicle.getNumber());
            statement.setString(2, vehicle.getOwner());
            statement.setString(3, vehicle.getType());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT vehicle_id FROM vehicles WHERE vehicle_number=?")) {
            statement.setString(1, vehicle.getNumber());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Could not save vehicle.");
                return result.getLong(1);
            }
        }
    }

    private boolean isVehicleActive(Connection connection, String number) throws SQLException {
        return exists(connection, "SELECT 1 FROM parking_records r JOIN vehicles v "
                + "ON v.vehicle_id=r.vehicle_id WHERE v.vehicle_number=? AND r.exit_time IS NULL", number);
    }

    private boolean isVehicleWaiting(Connection connection, String number) throws SQLException {
        return exists(connection, "SELECT 1 FROM waiting_queue WHERE vehicle_number=?", number);
    }

    private boolean exists(Connection connection, String sql, String number) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private String slotNumber(Connection connection, long slotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT slot_number FROM parking_slots WHERE slot_id=?")) {
            statement.setLong(1, slotId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Allocated slot could not be found.");
                return result.getString(1);
            }
        }
    }

    private static double calculateFee(LocalDateTime entry, LocalDateTime exit, String type) {
        long seconds = Math.max(1, Duration.between(entry, exit).getSeconds());
        long hours = Math.max(1, (seconds + 3599) / 3600);
        double hourlyRate = switch (type) {
            case "BIKE" -> 20.0;
            case "CAR" -> 50.0;
            default -> 40.0;
        };
        return hours * hourlyRate;
    }

    private static void validateVehicle(Vehicle vehicle) throws SQLException {
        if (vehicle.getNumber().isBlank() || vehicle.getOwner().isBlank()) {
            throw new SQLException("Vehicle number and owner name are required.");
        }
    }

    private static String normalizeNumber(String number) {
        return number == null ? "" : number.trim().toUpperCase(Locale.ROOT);
    }

    private static String hashPassword(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_BITS);
        try {
            return toHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded());
        } finally {
            spec.clearPassword();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) text.append(String.format("%02x", value));
        return text.toString();
    }

    private static byte[] fromHex(String text) {
        byte[] bytes = new byte[text.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static void rollback(Connection connection, SQLException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    public static class DashboardStats {
        public final int total, available, occupied, waiting;
        DashboardStats(int total, int available, int occupied, int waiting) {
            this.total = total; this.available = available; this.occupied = occupied; this.waiting = waiting;
        }
    }

    public static class EntryResult {
        public final boolean queued;
        public final String slot;
        EntryResult(boolean queued, String slot) { this.queued = queued; this.slot = slot; }
    }

    public static class ExitResult {
        public final ParkingRecord record;
        public final String assignedVehicle;
        ExitResult(ParkingRecord record, String assignedVehicle) {
            this.record = record; this.assignedVehicle = assignedVehicle;
        }
    }

    public static class ReportStats {
        public final int today, available, occupied, total;
        public final double revenue;
        ReportStats(int today, double revenue, int available, int occupied, int total) {
            this.today = today; this.revenue = revenue; this.available = available;
            this.occupied = occupied; this.total = total;
        }
    }
}
