import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbChecker {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== CATEGORIAS ===");
            ResultSet rs = stmt.executeQuery("SELECT * FROM categorias");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getString("codigo") + " | " + rs.getString("nombre"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
