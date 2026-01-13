package legacy.schema;

import legacy.annotations.Column;
import legacy.annotations.Entity;
import legacy.annotations.ForeignKey;
import legacy.annotations.Id;
import legacy.query.Comparator;
import legacy.query.Filter;
import legacy.query.QueryManager;
import legacy.query.RawObject;
import legacy.exceptions.UnmountedEntityException;

import java.lang.reflect.Field;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

public class BaseEntity {

    QueryManager queryManager;
    ForeignKeysCollection foreignKeysCollection;
    private boolean mounted;

    public BaseEntity() {
        this.queryManager = QueryManager.get_instance();
        this.foreignKeysCollection = new ForeignKeysCollection();
        this.mounted = false;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public LinkedHashMap<String, Object> getColumnsWithValue() {
        return getColumnsWithValue(true);
    }

    private LinkedHashMap<String, Object> getColumnsWithValue(boolean includeId) {
        LinkedHashMap<String, Object> columns = new LinkedHashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(Column.class)) {
                if (!includeId && field.isAnnotationPresent(legacy.annotations.Id.class)) {
                    continue;
                }
                String colName = field.getName();
                Column columnAnnotation = field.getAnnotation(Column.class);
                if(!columnAnnotation.name().isEmpty()) {
                    colName = columnAnnotation.name();
                }   
                field.setAccessible(true);
                try {
                    Method getter = this.getClass().getMethod("get" + capitalize(field.getName()));
                    columns.put(colName, getter.invoke(this));
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return columns;
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

    public String getIdFieldName() {
        Field[] fields = this.getClass().getDeclaredFields();
        return findIdFieldNameInFields(fields);
    }

    public static <T extends BaseEntity> String getIdFieldNameFromClass(Class<T> entityClass) {
        if(!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("The entity class provided is not annotated with @Entity");
        }

        Field[] fields = entityClass.getDeclaredFields();
        return findIdFieldNameInFields(fields);
    }

    private static String findIdFieldNameInFields(Field[] fields) {
        for(Field field : fields) {
            if(field.isAnnotationPresent(Id.class)) {
                String colName = field.getName();
                if(field.isAnnotationPresent(Column.class)) {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    if(!columnAnnotation.name().isEmpty()) {
                        colName = columnAnnotation.name();
                    }
                }
                return colName;
            }
        }
        return null;
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
        String idFieldName = this.getIdFieldName();

        sql.append(getTableNameFromClass(this.getClass()));
        sql.append(" SET ");
        for(String colName : columnsWithValue.keySet()) {
            sql.append(colName);
            sql.append(" = ?");
            sql.append(", ");   
        }
        sql.setLength(sql.length() - 2); 
        sql.append(" WHERE ");
        sql.append(idFieldName);
        sql.append( " = ?");

        return sql.toString();
    }

    public BaseEntity save() throws Exception {
        LinkedHashMap<String, Object> columnsWithValue = getColumnsWithValue(false);
        String sqlStr = createInsertSql(columnsWithValue);
        Object[] params = columnsWithValue.values().toArray();
        long returnedId = this.queryManager.executeInsertReturnId(sqlStr, params);

        Method idSetter = getIdSetter();
        if(idSetter != null) {
            try {
                idSetter.invoke(this, returnedId);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return this;
    }
    
    public BaseEntity update() throws Exception {
        LinkedHashMap<String, Object> columnsWithValue = this.getColumnsWithValue(false);
        String sql = this.createUpdateSql(columnsWithValue);
        String idFieldName = this.getIdFieldName();
        Method idGetter = this.getIdGetter();
        Object id = idGetter.invoke(this);
        columnsWithValue.put(idFieldName, id);
        Object[] params = columnsWithValue.values().toArray();

        this.queryManager.executeUpdate(sql, params);        
        return this;
    }

    public void delete() throws Exception {
        Method idGetter = this.getIdGetter();
        String idFieldName = this.getIdFieldName();
        if (idGetter == null || idFieldName == null) {
            throw new IllegalStateException("Id field not defined on entity " + this.getClass().getSimpleName());
        }

        Object idValue = idGetter.invoke(this);
        String sql = "DELETE FROM " + getTableNameFromClass(this.getClass()) + " WHERE " + idFieldName + " = ?";
        this.queryManager.executeUpdate(sql, idValue);
    }

    public static <T extends BaseEntity> List<T> findAll(Class<T> entityClass, QueryManager queryManager) throws Exception {
        String tableName = getTableNameFromClass(entityClass);
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Entity class " + entityClass.getSimpleName() + " is not annotated with @Entity");
        }
        String sql = "SELECT * FROM " + tableName;
        List<RawObject> rows = queryManager.executeSelect(sql);

        return RawObject.mapRowsToEntities(rows, entityClass);
    }

    public static <T extends BaseEntity> T findById(Object id, Class<T> entityClass, QueryManager queryManager) throws Exception {
        String tableName = getTableNameFromClass(entityClass);
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Entity class " + entityClass.getSimpleName() + " is not annotated with @Entity");
        }
        T instance = entityClass.getDeclaredConstructor().newInstance();
        String idFieldName = instance.getIdFieldName();
        
        String sql = "SELECT * FROM " + tableName + " WHERE " + idFieldName + " = ?";
        List<RawObject> rows = queryManager.executeSelect(sql, id);
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

    public static <T extends  BaseEntity> List<T> fetch(Class<T> entityClass, QueryManager queryManager, String sql, Object...params) throws Exception {
        List<RawObject> rawObjects = queryManager.executeSelect(sql, params);
        List<T> baseEntityList = new ArrayList<>();
        for(RawObject rawObject : rawObjects){
            baseEntityList.add(rawObject.toEntity(entityClass));
        }
        return baseEntityList;
    }

    public static <T extends  BaseEntity> List<T> findAllByIds(Class<T> entityClass, QueryManager queryManager, List<Object> ids) throws Exception {
        StringBuilder sb = new StringBuilder("SELECT * FROM "+getTableNameFromClass(entityClass)+" WHERE "+getIdFieldNameFromClass(entityClass)+" IN (");
        String idsString = "";
        for(int i = 0 ; i < ids.size(); i++) {
            idsString += "?, ";
        }
        idsString = idsString.substring(0, idsString.length() - 2);
        sb.append(idsString).append(")");

        return fetch(entityClass, queryManager, sb.toString(), ids.toArray());
    }

    public void mount() throws Exception {
        if (this.foreignKeysCollection == null) {
            this.foreignKeysCollection = new ForeignKeysCollection();
        } else {
            this.foreignKeysCollection.clear();
        }
        List<Field> foreignKeysFields = BaseEntity.getAllForeignKeysFields(this.getClass());
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

            Method getterMethod = this.getClass().getMethod("get"+field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
            Object value = getterMethod.invoke(this);
            if (value != null) {
                BaseEntity entity = BaseEntity.findById(value, entityClass, queryManager);
                if (entity != null) {
                    this.foreignKeysCollection.put(fieldName, entity);
                }
            }

        }

        this.mounted = true;
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }

    public ForeignKeysCollection getForeignKeysCollection() throws UnmountedEntityException {
        if(!this.mounted) {
            throw new UnmountedEntityException("The entity "+this.getClass().getSimpleName()+" is not mounted. Please call the mount() method before accessing mounted foreign keys.");
        }
        return foreignKeysCollection;
    }

    public static <T extends BaseEntity> List<T> filter(
            Class<T> entityClass,
            QueryManager queryManager,
            Filter... filters) throws Exception {

        String tableName = getTableNameFromClass(entityClass);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1 = 1");

        List<Object> params = new ArrayList<>();

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

        return fetch(entityClass, queryManager, sql.toString(), params.toArray());
    }

}
