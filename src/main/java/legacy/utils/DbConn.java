package legacy.utils;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DbConn {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        try {
            Map<String, String> confMap = getConf();
            config.setJdbcUrl(confMap.get("url"));
            config.setUsername(confMap.get("username"));
            config.setPassword(confMap.get("password"));

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(3000);
            config.setIdleTimeout(600_000);

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConn() throws SQLException {
        return dataSource.getConnection();
    }

    private static Map<String, String> getConf() throws Exception {
        try {
            Class.forName("org.postgresql.Driver");
            Map<String, String> confMap = new HashMap<>();
            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null && !databaseUrl.isEmpty()) {
                getConnectionFromDatabaseUrl(confMap, databaseUrl);
            }

            String dbUrl = System.getenv("DB_URL");
            if (dbUrl != null && !dbUrl.isEmpty()) {
                getConnectionFromEnvVariables(confMap);
            }

            getConnectionFromProperties(confMap);

            return confMap;

        } catch(Exception e) {
            throw new Exception("Une erreur s'est produite lors de la connection à la base de données: " + e.getMessage());
        }
    }

    private static void getConnectionFromDatabaseUrl(Map<String, String> confMap, String databaseUrl) throws Exception {
        URI dbUri = new URI(databaseUrl);

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];

        int port = dbUri.getPort() != -1 ? dbUri.getPort() : 5432;

        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + dbUri.getPath();

        String sslMode = System.getenv("DB_SSL_MODE");
        if (sslMode != null) {
            dbUrl += "?sslmode=" + sslMode;
        } else if (System.getenv("HEROKU") != null || databaseUrl.contains("heroku")) {
            dbUrl += "?sslmode=require";
        }

        confMap.put("url", dbUrl);
        confMap.put("username", username);
        confMap.put("password", password);

    }

    private static void getConnectionFromEnvVariables(Map<String, String> confMap) throws Exception {
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        String sslMode = System.getenv("DB_SSL_MODE");
        if (sslMode != null) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=" + sslMode;
        } else if (System.getenv("RENDER") != null) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        confMap.put("url", url);
        confMap.put("username", username);
        confMap.put("password", password);
    }

    private static void getConnectionFromProperties(Map<String, String> confMap) throws Exception {
        Properties properties = PropertyLoader.loadProperties("application.properties");
        String url = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");

        confMap.put("url", url);
        confMap.put("username", username);
        confMap.put("password", password);
    }

}
