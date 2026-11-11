package smartparking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/smart_parking?serverTimezone=UTC";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("PARKING_DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("PARKING_DB_USER", "root");
        String password = System.getenv().getOrDefault("PARKING_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
