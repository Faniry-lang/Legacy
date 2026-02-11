package legacy.annotations;

import legacy.strategy.GeneratedAfterPersistence;
import legacy.strategy.Strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Generated {
    Class<? extends Strategy> strategy() default GeneratedAfterPersistence.class;
    boolean overWrite() default true;
}
