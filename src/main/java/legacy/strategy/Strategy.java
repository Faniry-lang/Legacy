package legacy.strategy;

import legacy.schema.BaseEntity;

import java.io.Serializable;

public interface Strategy {
    public Serializable generate(BaseEntity entity);
}
