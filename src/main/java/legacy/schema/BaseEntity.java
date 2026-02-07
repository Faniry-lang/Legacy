package legacy.schema;

import legacy.annotations.*;
import legacy.query.Filter;
import legacy.query.FilterSet;
import legacy.query.QueryManager;
import legacy.query.RawObject;
import legacy.strategy.GeneratedAfterPersistence;
import legacy.strategy.Strategy;

import java.lang.reflect.Field;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

public class BaseEntity {

    QueryManager queryManager;
    ForeignKeysCollection foreignKeysCollection;

    public BaseEntity() {
        this.queryManager = QueryManager.get_instance();
        this.foreignKeysCollection = new ForeignKeysCollection();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public LinkedHashMap<String, Object> getColumnsWithValue() throws InstantiationException {
        return getColumnsWithValue(true);
    }

    private LinkedHashMap<String, Object> getColumnsWithValue(boolean includeId) throws InstantiationException {
        LinkedHashMap<String, Object> columns = new LinkedHashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(Column.class)) {
                String colName = field.getName();
                Column columnAnnotation = field.getAnnotation(Column.class);
                if(!columnAnnotation.name().isEmpty()) {
                    colName = columnAnnotation.name();
                }
                if(!includeId && field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = null;
                    if(field.isAnnotationPresent(Generated.class)) {
                        if(field.getAnnotation(Generated.class).strategy().equals(GeneratedAfterPersistence.class)) {
                            continue;
                        }
                        value = getGeneratedValue(field);
                        columns.put(colName, value);
                    } else {
                        Method getter = this.getClass().getMethod("get" + capitalize(field.getName()));
                        value = getter.invoke(this);
                        if(value == null) {
                            if(!columnAnnotation.nullable()) {
                                throw new IllegalArgumentException("Field '"+field.getName()+"' is marked as non-nullable but is provided with null value");
                            }
                            continue;
                        }
                        columns.put(colName, value);
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    System.out.println("[LEGACY ERROR] Error while retrieving entity columns with value: "+e.getMessage());
                }
            }
        }
        return columns;
    }

    private Object getGeneratedValue(Field field) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Generated generatedValueAnnotation = field.getAnnotation(Generated.class);
        Class<? extends Strategy> strategyClass =  generatedValueAnnotation.strategy();
        Strategy strategy = strategyClass.getDeclaredConstructor().newInstance();
        // shouldn't happen
        if(strategy instanceof GeneratedAfterPersistence) {
            return null;
        }
        Object idValue = strategy.generate(this);
        return idValue;
    }

    public Method getIdGetter() {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(legacy.annotations.Id.class)) {
                try {
                    return this.getClass().getMethod("get" + capitalize(field.getName()));
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public Method getIdGetter(String idFieldName) throws Exception {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(legacy.annotations.Id.class)) {
                if(!field.isAnnotationPresent(Column.class)) {
                    throw new Exception("Found columnd @Id not annotated with @Column, please add @Column annotation to the id column");
                }
                Column columnAnnotation = field.getAnnotation(Column.class);
                if(!columnAnnotation.name().equalsIgnoreCase(idFieldName)) {
                    continue;
                }
                try {
                    return this.getClass().getMethod("get" + capitalize(field.getName()));
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public Method getIdSetter() {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(legacy.annotations.Id.class)) {
                try {
                    return this.getClass().getMethod("set" + capitalize(field.getName()), field.getType());
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public List<String> getIdFieldName() {
        Field[] fields = this.getClass().getDeclaredFields();
        return findIdFieldNameInFields(fields);
    }

    public static <T extends BaseEntity> List<String> getIdFieldNameFromClass(Class<T> entityClass) {
        if(!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("The entity class provided is not annotated with @Entity");
        }

        Field[] fields = entityClass.getDeclaredFields();
        return findIdFieldNameInFields(fields);
    }

    private static List<String> findIdFieldNameInFields(Field[] fields) {
        List<String> ids = new ArrayList<>();
        for(Field field : fields) {
            if(field.isAnnotationPresent(Id.class)) {
                String colName = field.getName();
                if(field.isAnnotationPresent(Column.class)) {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    if(!columnAnnotation.name().isEmpty()) {
                        colName = columnAnnotation.name();
                    }
                }
                ids.add(colName);
            }
        }
        return ids;
    }

    public String createInsertSql(LinkedHashMap<String, Object> columnsWithValue) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(getTableNameFromClass(this.getClass())).append(" (");
        for(String col : columnsWithValue.keySet()) {
            sql.append(col).append(", ");
        }
        sql.setLength(sql.length() - 2); 
        sql.append(") VALUES (");
        for(int i = 0; i < columnsWithValue.size(); i++) {
            sql.append("?, ");
        }
        sql.setLength(sql.length() - 2); 
        sql.append(")");
        return sql.toString();
    }

    public String createMultipleInsertSql(List<LinkedHashMap<String, Object>> listOfColumnsWithValue) {
        if(listOfColumnsWithValue.isEmpty()) {
            return "";
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(getTableNameFromClass(this.getClass())).append(" (");
        LinkedHashMap<String, Object> firstRow = listOfColumnsWithValue.get(0);
        for(String col : firstRow.keySet()) {
            sql.append(col).append(", ");
        }
        sql.setLength(sql.length() - 2); 
        sql.append(") VALUES ");

        for(int i = 0; i < listOfColumnsWithValue.size(); i++) {
            sql.append("(");
            for(int j = 0; j < firstRow.size(); j++) {
                sql.append("?, ");
            }
            sql.setLength(sql.length() - 2); 
            sql.append("), ");
        }
        sql.setLength(sql.length() - 2); 

        return sql.toString();
    }

    public String createUpdateSql(LinkedHashMap<String, Object> columnsWithValue) throws Exception {
        StringBuilder sql = new StringBuilder("UPDATE ");
        List<String> idFieldName = this.getIdFieldName();

        sql.append(getTableNameFromClass(this.getClass()));
        sql.append(" SET ");
        for(String colName : columnsWithValue.keySet()) {
            if(colName.equals(idFieldName)) {
                continue;
            }
            sql.append(colName);
            sql.append(" = ?");
            sql.append(", ");   
        }
        sql.setLength(sql.length() - 2); 
        sql.append(" WHERE ");

        for(int i = 0; i < idFieldName.size(); i++) {
            sql.append(idFieldName.get(i));
            sql.append( " = ?");
            if(i < idFieldName.size() - 1) {
                sql.append(" AND ");
            }
        }

        return sql.toString();
    }

    public BaseEntity save() throws Exception {
        LinkedHashMap<String, Object> columnsWithValue = getColumnsWithValue();
        String sqlStr = createInsertSql(columnsWithValue);
        System.out.println("[DEBUG Legacy Framework] (BaseEntity.save) Generated SQL: " + sqlStr);
        Object[] params = columnsWithValue.values().toArray();

        int returnedId = this.queryManager.executeInsertReturnId(sqlStr, params);

        List<String> ids = getIdFieldName();
        if(columnsWithValue.containsKey(ids.get(0))) {
            // retrieved columns have id so it means they are not generated
            return this;
        }

        // assuming auto generated id are single id
        Method idSetter = getIdSetter();
        if(idSetter != null) {
            try {
                idSetter.invoke(this, returnedId);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }

        return this;
    }
    
    public BaseEntity update() throws Exception {
        LinkedHashMap<String, Object> columnsWithValue = this.getColumnsWithValue(false);
        String sql = this.createUpdateSql(columnsWithValue);
        List<String> idFieldName = this.getIdFieldName();
        for(String idField : idFieldName) {
            Method idGetter = this.getIdGetter(idField);
            Object id = idGetter.invoke(this);
            columnsWithValue.put(idField, id);
        }
        Object[] params = columnsWithValue.values().toArray();

        this.queryManager.executeUpdate(sql, params);        
        return this;
    }

    public void delete() throws Exception {
        List<String> idFieldName = this.getIdFieldName();
        String sql = "DELETE FROM " + getTableNameFromClass(this.getClass()) + " WHERE ";
        String afterWhere = "";
        Object[] ids = new Object[idFieldName.size()];
        for(int i = 0; i < idFieldName.size(); i++) {
            Method idGetter = this.getIdGetter(idFieldName.get(i));
            if (idGetter == null) {
                throw new IllegalStateException("'"+idFieldName.get(i)+"' Id getter not found on entity " + this.getClass().getSimpleName());
            }

            Object idValue = idGetter.invoke(this);
            ids[i] = idValue;
            afterWhere += idFieldName.get(i) + " = ?";
            if(i < idFieldName.size() - 1) {
                afterWhere += " AND ";
            }
        }
        if(afterWhere.isEmpty() || afterWhere.equals("")) {
            throw new Exception("[LEGACY ERROR] Delete operation invalid due to unspecified id field in entity class: "+this.getClass().getSimpleName());
        }
        this.queryManager.executeUpdate(sql+afterWhere, ids);
    }

    public static <T extends BaseEntity> List<T> findAll(Class<T> entityClass) throws Exception {
        String tableName = getTableNameFromClass(entityClass);
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Entity class " + entityClass.getSimpleName() + " is not annotated with @Entity");
        }
        String sql = "SELECT * FROM " + tableName;
        QueryManager qm = QueryManager.get_instance();
        List<RawObject> rows = qm.executeSelect(sql);

        return RawObject.mapRowsToEntities(rows, entityClass);
    }

    public static <T extends BaseEntity> T findById(Object id, Class<T> entityClass) throws Exception {
        String tableName = getTableNameFromClass(entityClass);
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Entity class " + entityClass.getSimpleName() + " is not annotated with @Entity");
        }
        T instance = entityClass.getDeclaredConstructor().newInstance();
        String idFieldName = instance.getIdFieldName().get(0);
        
        String sql = "SELECT * FROM " + tableName + " WHERE " + idFieldName + " = ?";
        QueryManager qm = QueryManager.get_instance();
        List<RawObject> rows = qm.executeSelect(sql, id);
        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(0).toEntity(entityClass);
    }

    public static <T extends BaseEntity> T findById(Map<String, Object> ids, Class<T> entityClass) throws Exception {
        String tableName = getTableNameFromClass(entityClass);
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Entity class " + entityClass.getSimpleName() + " is not annotated with @Entity");
        }
        T instance = entityClass.getDeclaredConstructor().newInstance();
        List<String> idFieldName = instance.getIdFieldName();

        String sql = "SELECT * FROM " + tableName + " WHERE ";
        String afterWhere = "";
        Object[] idsParams = new Object[idFieldName.size()];
        for(int i = 0; i < idFieldName.size(); i++) {
            afterWhere += idFieldName.get(i) + " = ?";
            if(i < idFieldName.size() - 1) {
                afterWhere += " AND ";
            }
            idsParams[i] = ids.get(idFieldName.get(i));
        }

        if(afterWhere.isEmpty() || afterWhere.equals("")) {
            throw new Exception("[LEGACY ERROR] Error while fetching by ids, condition invalid (id annotated fields might be missing in entity declaration)");
        }

        QueryManager qm = QueryManager.get_instance();
        List<RawObject> rows = qm.executeSelect(sql+afterWhere, idsParams);
        if (rows.isEmpty()) {
            return null;
        }

        return rows.get(0).toEntity(entityClass);
    }

    private static String getTableNameFromClass(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Entity.class)) {
            Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
            if (!entityAnnotation.tableName().isEmpty()) {
                return entityAnnotation.tableName();
            }
        }
        throw new IllegalStateException("The targeted entityClass '"+entityClass.getName()+"' doesn't have an annotated tableName");
    }

    public static List<Field> getAllForeignKeysFields(Class<? extends BaseEntity> entityClass) {
        List<Field> fields = new ArrayList<>();
        for(Field field : entityClass.getDeclaredFields()) {
            if(field.isAnnotationPresent(ForeignKey.class)) {
                fields.add(field);
            }
        }
        return fields;
    }

    public static List<ColumnData> getColumnDatas(Class<? extends BaseEntity> entityClass) {
        List<ColumnData> columnDatas = new ArrayList<>();
        Field[] fields = entityClass.getDeclaredFields();
        for(Field field : fields) {
            if(field.isAnnotationPresent(Column.class)) {
                Column colAnnotation = field.getAnnotation(Column.class);
                String nameFromDb = field.getName();
                String nameFromEntity = field.getName();
                if(colAnnotation.name() != null && !colAnnotation.name().isEmpty()) {
                    nameFromDb = colAnnotation.name();
                }
                columnDatas.add(new ColumnData(nameFromDb, nameFromEntity));
            }
        }
        return columnDatas;
    }

    public static <T extends  BaseEntity> List<T> fetch(Class<T> entityClass, String sql, Object...params) throws Exception {
        QueryManager qm = QueryManager.get_instance();
        List<RawObject> rawObjects = qm.executeSelect(sql, params);
        List<T> baseEntityList = new ArrayList<>();
        for(RawObject rawObject : rawObjects){
            baseEntityList.add(rawObject.toEntity(entityClass));
        }
        return baseEntityList;
    }

    private void mount(String fieldName) throws Exception {
        if (this.foreignKeysCollection == null) {
            this.foreignKeysCollection = new ForeignKeysCollection();
        }

        Field field = null;
        String fName = null;
        List<Field> foreignKeysFields = BaseEntity.getAllForeignKeysFields(this.getClass());
        for(Field f : foreignKeysFields) {
            fName = f.getName();
            if(f.isAnnotationPresent(Column.class)) {
                Column colAnnotation = f.getAnnotation(Column.class);
                if(!colAnnotation.name().isEmpty()) {
                    fName = colAnnotation.name();
                }
            }
            if(fName.equals(fieldName)) {
                field = f;
                break;
            }
        }
        if(field == null) {
            throw new IllegalArgumentException("Foreign key field '"+fieldName+"' not found in entity '"+this.getClass().getSimpleName()+"'");
        }

        if(this.foreignKeysCollection.get(fName) != null) {
            return;
        }

        Class<? extends BaseEntity> entityClass = null;
        if(field.isAnnotationPresent(ForeignKey.class)) {
            ForeignKey fkAnnotation = field.getAnnotation(ForeignKey.class);
            if (fkAnnotation == null) {
                throw new IllegalArgumentException("Foreign key annotation not found on field '" + fieldName + "' in entity '" + this.getClass().getSimpleName() + "'");
            }
            entityClass = fkAnnotation.entity();
        }

        if(entityClass != null) {
            getForeignKeyEntity(fieldName, field, entityClass);
        }

    }

    private void getForeignKeyEntity(String fieldName, Field field, Class<? extends BaseEntity> entityClass) throws Exception {
        Method getterMethod = this.getClass().getMethod("get"+field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
        Object value = getterMethod.invoke(this);
        if (value != null) {
            BaseEntity entity = BaseEntity.findById(value, entityClass);
            if (entity != null) {
                this.foreignKeysCollection.put(fieldName, entity);
            }
        }
    }

    private void mountAll() throws Exception {
        if (this.foreignKeysCollection == null) {
            this.foreignKeysCollection = new ForeignKeysCollection();
        } else {
            this.foreignKeysCollection.clear();
        }
        List<Field> foreignKeysFields = BaseEntity.getAllForeignKeysFields(this.getClass());
        if(foreignKeysFields.size() == this.foreignKeysCollection.getAll().size()) {
            return;
        }
        for(Field field : foreignKeysFields) {
            String fieldName = field.getName();
            if(field.isAnnotationPresent(Column.class)) {
                Column colAnnotation = field.getAnnotation(Column.class);
                if(!colAnnotation.name().isEmpty()) {
                    fieldName = colAnnotation.name();
                }
            }

            Class<? extends BaseEntity> entityClass = null;
            if(field.isAnnotationPresent(ForeignKey.class)) {
                ForeignKey fkAnnotation = field.getAnnotation(ForeignKey.class);
                if(fkAnnotation == null) continue;
                entityClass = fkAnnotation.entity();
            }

            if(this.foreignKeysCollection.get(fieldName) == null) {
                getForeignKeyEntity(fieldName, field, entityClass);
            }

        }
    }

    public <T extends BaseEntity> T getForeignKey(String fieldName) throws Exception {
        this.mount(fieldName);
        return (T) foreignKeysCollection.get(fieldName);
    }

    public ForeignKeysCollection getForeignKeysCollection() throws Exception {
        this.mountAll();
        return foreignKeysCollection;
    }

    public static <T extends BaseEntity> List<T> filter(
            Class<T> entityClass,
            FilterSet filterSet) throws Exception {

        String tableName = getTableNameFromClass(entityClass);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1 = 1");

        List<Object> params = new ArrayList<>();

        List<Filter> filters = filterSet.getFilters();
        if (filters != null) {
            for (Filter filter : filters) {
                sql.append(" AND ").append(filter.getFieldName());

                switch (filter.getComparator()) {
                    case EQUALS -> sql.append(" = ?");
                    case NOT_EQUALS -> sql.append(" <> ?");
                    case GREATER_THAN -> sql.append(" > ?");
                    case LESS_THAN -> sql.append(" < ?");
                    case GREATER_THAN_OR_EQUALS -> sql.append(" >= ?");
                    case LESS_THAN_OR_EQUALS -> sql.append(" <= ?");
                    case LIKE -> sql.append(" LIKE ?");
                    case ILIKE -> sql.append(" ILIKE ?");
                    case IN -> {
                        Collection<?> values = (Collection<?>) filter.getValue();
                        if (values == null || values.isEmpty()) {
                            throw new IllegalArgumentException("IN comparator requires at least one value");
                        }
                        String placeholders = values.stream()
                                .map(v -> "?")
                                .collect(Collectors.joining(", "));
                        sql.append(" IN (").append(placeholders).append(")");
                        params.addAll(values);
                        continue;
                    }
                    default -> throw new IllegalArgumentException("Unsupported comparator: " + filter.getComparator());
                }

                params.add(filter.getValue());
            }
        }

        System.out.println("[DEBUG LEGACY FRAMEWORK] (BaseEntity.filter) Generated SQL: " + sql.toString());

        return fetch(entityClass, sql.toString(), params.toArray());
    }

    public static <T extends BaseEntity> List<T> findBy(String fieldName, Object value, Class<T> entityClass) throws Exception {
        String tableName = getTableNameFromClass(entityClass);
        String sql = "SELECT * FROM " + tableName + " WHERE " + fieldName + " = ?";
        return fetch(entityClass, sql, value);
    }

}
