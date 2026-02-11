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
