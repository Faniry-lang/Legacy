package legacy.query;

import legacy.utils.DbConn;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryManager {

    private static QueryManager _instance;

    public static QueryManager get_instance() {
        if(_instance == null) {
            _instance = new QueryManager();
        }
        return _instance;
    }

    public List<RawObject> executeSelect(String sql, Object... params) throws Exception {
        List<RawObject> resultList = new ArrayList<>();

        try (Connection conn = DbConn.getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    resultList.add(new RawObject(row));
                }
            }
        }

        return resultList;
    }

    public int executeUpdate(String sql, Object... params) throws Exception {
        try (Connection conn = DbConn.getConn();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    public long executeInsertReturnId(String sql, Object... params) throws Exception {
        try (Connection conn = DbConn.getConn();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(stmt, params);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new SQLException("Aucune clé générée");
                }
            }
        }
    }

    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }
}
