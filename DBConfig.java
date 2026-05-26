import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConfig {

    private static final String DB_USER = "root";
    private static final String DB_PASS = "";    
    private static final String URL = "jdbc:mysql://localhost:3306/Juris" + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static Connection connection = null;
    private DBConfig() {}

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB ERROR] MySQL JDBC driver not found. ");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Cannot connect: " + e.getMessage());
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    public static boolean testConnection() {
        Connection c = getConnection();
        
        if (c != null) {
            System.out.println("[DB] Connected successfully.");
            return true;
        }
        System.err.println("[DB ERROR] Connection failed.");
        return false;
    }
}
