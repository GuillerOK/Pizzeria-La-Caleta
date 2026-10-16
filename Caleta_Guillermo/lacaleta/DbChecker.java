import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbChecker {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-wandering-cell-ay8ox05o-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String password = "npg_ULk4dAE5TuBH";

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
