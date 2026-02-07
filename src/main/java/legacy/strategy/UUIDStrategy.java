package legacy.strategy;

import legacy.schema.BaseEntity;

import java.io.Serializable;
import java.util.UUID;

public class UUIDStrategy implements Strategy {
    @Override
    public Serializable generate(BaseEntity entity) {
        return UUID.randomUUID();
    }
}
