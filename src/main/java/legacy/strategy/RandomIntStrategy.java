package legacy.strategy;

import legacy.schema.BaseEntity;

import java.io.Serializable;
import java.util.Random;

public class RandomIntStrategy implements Strategy {
    @Override
    public Serializable generate(BaseEntity entity) {
        Random random = new Random();
        return random.nextInt();
    }
}
