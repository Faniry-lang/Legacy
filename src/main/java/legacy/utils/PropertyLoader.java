package legacy.utils;

import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {
    
    /**
     * Charge les propriétés depuis un fichier ou depuis les variables d'environnement.
     * Priorité: fichier de propriétés > variables d'environnement
     *
     * Pour Heroku, définissez les variables d'environnement correspondantes :
     * - DB_URL ou DATABASE_URL
     * - DB_USERNAME
     * - DB_PASSWORD
     */
    public static Properties loadProperties(String propertyFileName) throws Exception {
        Properties properties = new Properties();

        try (InputStream input = PropertyLoader.class.getClassLoader().getResourceAsStream(propertyFileName)) {

            if (input != null) {
                // Mode fichier de propriétés (local)
                properties.load(input);
            } else {
                // Mode variables d'environnement (Heroku/Cloud)
                properties = loadFromEnvironment();
            }

        } catch (Exception e) {
            // Si le fichier n'existe pas, essayer les variables d'environnement
            properties = loadFromEnvironment();
            if (properties.isEmpty()) {
                throw new Exception("Aucune configuration trouvée. Fournissez un fichier "
                    + propertyFileName + " ou définissez les variables d'environnement appropriées.");
            }
        }

        return properties;
    }

    /**
     * Charge les propriétés depuis les variables d'environnement.
     */
    private static Properties loadFromEnvironment() {
        Properties properties = new Properties();

        // Support DATABASE_URL (format Heroku) ou DB_URL
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null) {
            properties.setProperty("db.url", dbUrl);
        }

        String dbUsername = System.getenv("DB_USERNAME");
        if (dbUsername != null) {
            properties.setProperty("db.username", dbUsername);
        }

        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword != null) {
            properties.setProperty("db.password", dbPassword);
        }

        return properties;
    }

}
