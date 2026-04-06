package package1;

import java.sql.*;

public class DatabaseHelper {
    private static final String URL = "jdbc:sqlite:repairs.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS repairs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp TEXT," +
                "customer_name TEXT," +
                "tel TEXT," +
                "brand TEXT," +
                "watch_model TEXT,"+
                "services TEXT," +
                "image_path TEXT" +
                ");";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Database initialized and table is ready.");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
