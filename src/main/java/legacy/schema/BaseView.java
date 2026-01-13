package legacy.schema;

import legacy.query.QueryManager;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;

public class BaseView extends BaseEntity {
    public BaseView() {
        super();
    }

    @Override
    @Deprecated
    public Method getIdGetter() {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public Method getIdSetter() {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public String getIdFieldName() {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public String createInsertSql(LinkedHashMap<String, Object> columnsWithValue) {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public String createMultipleInsertSql(List<LinkedHashMap<String, Object>> listOfColumnsWithValue) {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public String createUpdateSql(LinkedHashMap<String, Object> columnsWithValue) throws Exception {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public BaseEntity save() {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public BaseEntity update() throws Exception {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }

    @Override
    @Deprecated
    public void delete() throws Exception {
        throw new UnsupportedOperationException("Operation is not supported for views.");
    }
}
