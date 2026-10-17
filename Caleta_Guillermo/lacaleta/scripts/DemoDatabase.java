import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

/** Ejecuta el SQL generado sin imprimir credenciales ni depender de psql. */
class DemoDatabase {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        Path env = Path.of(".env");
        if (Files.exists(env)) try (var reader = Files.newBufferedReader(env, StandardCharsets.UTF_8)) { config.load(reader); }
        String url = valor(config, "DATABASE_DIRECT_URL", false);
        if (url.isBlank()) url = valor(config, "DATABASE_URL", true);
        String user = valor(config, "DATABASE_USERNAME", true);
        String password = valor(config, "DATABASE_PASSWORD", true);
        String sql = Files.readString(Path.of("db/preparar-demo.sql"), StandardCharsets.UTF_8)
                .replaceAll("(?m)^BEGIN;\\s*$", "").replaceAll("(?m)^COMMIT;\\s*$", "");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
                connection.commit();
                try (ResultSet rs = stmt.executeQuery("select count(distinct producto_id) from receta_insumos")) {
                    rs.next(); System.out.println("Esquema actualizado. Productos con receta: " + rs.getInt(1));
                }
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
    }
    private static String valor(Properties config, String key, boolean requerido) {
        String valor = System.getenv().getOrDefault(key, config.getProperty(key, ""));
        if (requerido && valor.isBlank()) throw new IllegalArgumentException("Falta configurar " + key);
        return valor;
    }
}
