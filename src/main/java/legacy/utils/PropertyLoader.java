package legacy.utils;

import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {
    
    public static Properties loadProperties(String propertyFileName) throws Exception {
        Properties properties = null;
        try(InputStream input = PropertyLoader.class.getClassLoader().getResourceAsStream(propertyFileName)) {

            if(input == null) {
                throw new RuntimeException("Impossible de trouver le fichier "+propertyFileName);
            }

            properties = new Properties();
            properties.load(input);

        } catch(Exception e) {
            throw new Exception("Une erreur s'est produite lors du chargement du fichier de propriétés: "+e.getMessage());
        }
        return properties;
    }

}
