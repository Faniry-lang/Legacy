package legacy.schema;

import legacy.exceptions.ForeignKeyFieldNotFound;

import java.util.HashMap;
import java.util.Map;

public class ForeignKeysCollection {
    Map<String, Object> foreignKeys;

    public ForeignKeysCollection(Map<String, Object> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public ForeignKeysCollection() {
        this.foreignKeys = new HashMap<>();
    }

    public void put(String key, Object value) {
        foreignKeys.put(key, value);
    }

    public Object get(String key) throws ForeignKeyFieldNotFound {
        Object value = foreignKeys.get(key);
        if (value == null) {
            throw new ForeignKeyFieldNotFound("Foreign key field '" + key + "' not found in the collection.");
        }
        return value;
    }

    public Map<String, Object> getAll() {
        return foreignKeys;
    }

    public void clear() {
        foreignKeys.clear();
    }
}
