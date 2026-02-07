package legacy.strategy;

import legacy.schema.BaseEntity;

import java.io.Serializable;

public interface Strategy<T extends BaseEntity> {
    public Serializable generate(T entity);
}
