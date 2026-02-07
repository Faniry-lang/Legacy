package legacy.strategy;

import legacy.schema.BaseEntity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

public class TimestampStrategy implements Strategy {
    @Override
    public Serializable generate(BaseEntity entity) {
        return Timestamp.from(Instant.now());
    }
}
