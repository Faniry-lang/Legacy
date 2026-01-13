package legacy.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class EntityGenerator {

    public static void generateEntity(String tableName, String tableType, String outputFolderPath, String packageName) throws Exception {
        Connection conn = DbConn.getConn();
        try {
            generateEntityInternal(tableName, tableType, outputFolderPath, packageName, conn);
        } finally {
            conn.close();
        }
    }

    public static void generateAllEntities(String outputFolderPath, String packageName) throws Exception {
        Connection conn = DbConn.getConn();
        try {
            Map<String, String> tableNames = getAllTableNames(conn);
            for (Map.Entry<String, String> entry : tableNames.entrySet()) {
                generateEntityInternal(entry.getKey(), entry.getValue(), outputFolderPath, packageName, conn);
            }
            System.out.println("Generated " + tableNames.size() + " entity classes.");
        } finally {
            conn.close();
        }
    }

    private static Map<String, String> getAllTableNames(Connection conn) throws SQLException {
        Map<String, String> tableNames = new HashMap<>();
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet tablesResultSet = metaData.getTables(null, "public", "%", new String[]{ "TABLE" });
        ResultSet viewsResultSet = metaData.getTables(null, "public", "%", new String[] { "VIEW" });

        while (tablesResultSet.next()) {
            String tableName = tablesResultSet.getString("TABLE_NAME");
            tableNames.put(tableName, "TABLE");
        }

        while (viewsResultSet.next()) {
            String viewName = viewsResultSet.getString("TABLE_NAME");
            tableNames.put(viewName, "VIEW");
        }

        tablesResultSet.close();
        viewsResultSet.close();
        return tableNames;
    }

    private static void generateEntityInternal(String tableName, String tableType, String outputFolderPath, String packageName, Connection conn) throws Exception {
            DatabaseMetaData metaData = conn.getMetaData();
            
            ResultSet pkResultSet = metaData.getPrimaryKeys(null, null, tableName);
            Set<String> primaryKeyColumns = new HashSet<>();
            while (pkResultSet.next()) {
                primaryKeyColumns.add(pkResultSet.getString("COLUMN_NAME"));
            }
            pkResultSet.close();

            ResultSet columnsResultSet = metaData.getColumns(null, null, tableName, null);
            Map<String, ColumnInfo> columns = new LinkedHashMap<>();
            
            while (columnsResultSet.next()) {
                String columnName = columnsResultSet.getString("COLUMN_NAME");
                String columnType = columnsResultSet.getString("TYPE_NAME");
                int columnSize = columnsResultSet.getInt("COLUMN_SIZE");
                boolean isNullable = columnsResultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                boolean isPrimaryKey = primaryKeyColumns.contains(columnName);

                ColumnInfo info = new ColumnInfo(columnName, columnType, columnSize, isNullable, isPrimaryKey);
                columns.put(columnName, info);
            }
            columnsResultSet.close();

            String classCode = generateClassCode(tableName, tableType, packageName, columns);
            
            writeToFile(classCode, outputFolderPath, packageName, tableName);
            
            System.out.println("Entity class generated successfully: " + getClassName(tableName) + ".java");
        }

    private static String generateClassCode(String tableName, String tableType, String packageName, Map<String, ColumnInfo> columns) {
        StringBuilder code = new StringBuilder();
        String className = getClassName(tableName);
        String baseClass = tableType.equals("TABLE") ? "BaseEntity" : "BaseView";

        code.append("package ").append(packageName).append(";\n\n");

        code.append("import legacy.annotations.Column;\n");
        code.append("import legacy.annotations.Entity;\n");

        if(tableType.equals("TABLE")) {
            code.append("import legacy.annotations.Id;\n");
        }

        code.append("import legacy.schema.").append(baseClass).append(";\n\n");

        boolean hasLocalDate = false;
        boolean hasLocalDateTime = false;
        boolean hasBigDecimal = false;

        for (ColumnInfo col : columns.values()) {
            String javaType = mapSqlTypeToJava(col.sqlType);
            if (javaType.equals("LocalDate")) hasLocalDate = true;
            if (javaType.equals("LocalDateTime")) hasLocalDateTime = true;
            if (javaType.equals("BigDecimal")) hasBigDecimal = true;
        }

        if (hasLocalDate) code.append("import java.time.LocalDate;\n");
        if (hasLocalDateTime) code.append("import java.time.LocalDateTime;\n");
        if (hasBigDecimal) code.append("import java.math.BigDecimal;\n");

        code.append("\n");

        code.append("@Entity(tableName = \"").append(tableName).append("\")\n");
        code.append("public class ").append(className).append(" extends ").append(baseClass).append(" {\n");
        code.append("    public ").append(className).append("() {\n");
        code.append("        super();\n");
        code.append("    }\n\n");

        for (ColumnInfo col : columns.values()) {
            if (col.isPrimaryKey) {
                code.append("    @Id\n");
            }
            code.append("    @Column");
            if (!col.columnName.equals(getFieldName(col.columnName))) {
                code.append("(name = \"").append(col.columnName).append("\")");
            }
            code.append("\n");
            
            String javaType = mapSqlTypeToJava(col.sqlType);
            code.append("    private ").append(javaType).append(" ").append(getFieldName(col.columnName));
            if (!col.isNullable && !col.isPrimaryKey && (javaType.equals("String") || javaType.equals("Boolean") || javaType.equals("Integer") || javaType.equals("Long"))) {
                // Basic non-nullable defaults
            }
            code.append(";\n\n");
        }

        for (ColumnInfo col : columns.values()) {
            String fieldName = getFieldName(col.columnName);
            String javaType = mapSqlTypeToJava(col.sqlType);
            String methodName = capitalize(fieldName);

            code.append("    public ").append(javaType).append(" get").append(methodName).append("() {\n");
            code.append("        return ").append(fieldName).append(";\n");
            code.append("    }\n\n");

            code.append("    public void set").append(methodName).append("(").append(javaType).append(" ").append(fieldName).append(") {\n");
            code.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            code.append("    }\n\n");
        }

        code.append("}\n");

        return code.toString();
    }

    private static String mapSqlTypeToJava(String sqlType) {
        String upperType = sqlType.toUpperCase();

        if (upperType.contains("SERIAL") || upperType.contains("BIGINT")) {
            return "Long";
        }
        if (upperType.contains("INT")) {
            return "Integer";
        }
        if (upperType.contains("FLOAT") || upperType.contains("REAL")) {
            return "Float";
        }
        if (upperType.contains("DOUBLE")) {
            return "Double";
        }
        if (upperType.contains("DECIMAL") || upperType.contains("NUMERIC")) {
            return "BigDecimal";
        }
        if (upperType.contains("BOOLEAN")) {
            return "Boolean";
        }
        if (upperType.contains("DATE") && !upperType.contains("TIME")) {
            return "LocalDate";
        }
        if (upperType.contains("TIMESTAMP")) {
            return "LocalDateTime";
        }
        if (upperType.contains("TIME")) {
            return "LocalDateTime";
        }
        if (upperType.contains("VARCHAR") || upperType.contains("CHAR") || upperType.contains("TEXT")) {
            return "String";
        }
        
        return "String"; 
    }

    private static String getFieldName(String columnName) {
        if (columnName.contains("_")) {
            StringBuilder result = new StringBuilder();
            String[] parts = columnName.split("_");
            for (int i = 0; i < parts.length; i++) {
                if (i == 0) {
                    result.append(parts[i].toLowerCase());
                } else {
                    result.append(capitalize(parts[i]));
                }
            }
            return result.toString();
        }
        return columnName;
    }

    private static String getClassName(String tableName) {
        StringBuilder className = new StringBuilder();
        String[] parts = tableName.split("_");
        for (String part : parts) {
            className.append(capitalize(part));
        }
        return className.toString();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String toCamelCase(String text) {
        StringBuilder result = new StringBuilder();
        boolean toUpper = false;
        for (char c : text.toCharArray()) {
            if (c == '_') {
                toUpper = true;
            } else {
                if (toUpper) {
                    result.append(Character.toUpperCase(c));
                    toUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    private static void writeToFile(String classCode, String outputFolderPath, String packageName, String tableName) throws IOException {
        String className = getClassName(tableName);
        
        String folderPath = outputFolderPath + File.separator + packageName.replace(".", File.separator);
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, className + ".java");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(classCode);
        }
    }

    private static class ColumnInfo {
        String columnName;
        String sqlType;
        int size;
        boolean isNullable;
        boolean isPrimaryKey;

        ColumnInfo(String columnName, String sqlType, int size, boolean isNullable, boolean isPrimaryKey) {
            this.columnName = columnName;
            this.sqlType = sqlType;
            this.size = size;
            this.isNullable = isNullable;
            this.isPrimaryKey = isPrimaryKey;
        }
    }
}
