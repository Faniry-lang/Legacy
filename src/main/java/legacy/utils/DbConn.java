package legacy.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DbConn {
    
    public static Connection getConn() throws Exception {
        try {
            Properties properties = PropertyLoader.loadProperties("application.properties");
            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, username, password);

        } catch(Exception e) {
            throw new Exception("Une erreur s'est produite lors de la connection à la base de données: "+e.getMessage());
        }
    }

}
