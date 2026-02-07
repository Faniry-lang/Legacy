package legacy.query;

import legacy.annotations.Column;
import legacy.schema.BaseEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class RawObject {
    private final Map<String, Object> data;

    public RawObject(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public <T extends BaseEntity> T toEntity(Class<T> entityClass) throws Exception {
        Constructor<T> ctor = entityClass.getConstructor();
        T instance = ctor.newInstance();

        for (Field field : entityClass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column == null) {
                continue;
            }

            String columnName = column.name().isEmpty() ? field.getName() : column.name();
            if (!data.containsKey(columnName)) {
                continue;
            }

            Method setter = null;
            try {
                setter = entityClass.getMethod("set" + capitalize(field.getName()), field.getType());
            } catch(NoSuchMethodException e) {
                throw new Exception(
                        entityClass.getName()+" BaseEntity setter 'set"+capitalize(field.getName())+"' not found, please review your BaseEntity class to match the setter conventions of Legacy ORM");
            }
            Object convertedValue = convertValue(data.get(columnName), field.getType());
            setter.invoke(instance, convertedValue);
        }

        return instance;
    }

    public static <T extends BaseEntity> List<T> mapRowsToEntities(List<RawObject> rows, Class<T> entityClass) throws Exception {
        List<T> entities = new ArrayList<>();
        for (RawObject row : rows) {
            entities.add(row.toEntity(entityClass));
        }
        return entities;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType.asSubclass(Enum.class), value.toString());
        }

        if (Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) {
            if (value instanceof Number) {
                return convertNumber((Number) value, targetType);
            }
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }

        if (isDateLike(targetType)) {
            return convertTemporal(value, targetType);
        }

        if (targetType == String.class) {
            return value.toString();
        }

        return value;
    }

    private Object convertNumber(Number number, Class<?> targetType) {
        if (targetType == Long.class || targetType == long.class) {
            return number.longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return number.intValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return number.doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return number.floatValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return number.shortValue();
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return number.byteValue();
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(number.toString());
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(number.toString());
        }
        return number;
    }

    private boolean isDateLike(Class<?> targetType) {
        return targetType == Date.class
            || targetType == java.sql.Date.class
            || targetType == java.sql.Time.class
            || targetType == java.sql.Timestamp.class
            || targetType == LocalDate.class
            || targetType == LocalTime.class
            || targetType == LocalDateTime.class
            || targetType == Instant.class
            || targetType == ZonedDateTime.class;
    }

    private Object convertTemporal(Object value, Class<?> targetType) {
        Instant instant = null;

        if (value instanceof java.sql.Timestamp) {
            instant = ((java.sql.Timestamp) value).toInstant();
        } else if (value instanceof java.sql.Date) {
            instant = Instant.ofEpochMilli(((java.sql.Date) value).getTime());
        } else if (value instanceof java.sql.Time) {
            instant = Instant.ofEpochMilli(((java.sql.Time) value).getTime());
        } else if (value instanceof Date) {
            instant = Instant.ofEpochMilli(((Date) value).getTime());
        }

        if (value instanceof LocalDate && targetType == LocalDate.class) {
            return value;
        }
        if (value instanceof LocalDateTime && targetType == LocalDateTime.class) {
            return value;
        }
        if (value instanceof LocalTime && targetType == LocalTime.class) {
            return value;
        }
        if (value instanceof Instant && (targetType == Instant.class || targetType == Date.class || targetType == java.sql.Timestamp.class)) {
            instant = (Instant) value;
        }

        if (instant == null) {
            return value;
        }

        if (targetType == Instant.class) {
            return instant;
        }
        if (targetType == Date.class) {
            return Date.from(instant);
        }
        if (targetType == java.sql.Timestamp.class) {
            return java.sql.Timestamp.from(instant);
        }
        if (targetType == java.sql.Date.class) {
            return new java.sql.Date(instant.toEpochMilli());
        }
        if (targetType == java.sql.Time.class) {
            return new java.sql.Time(instant.toEpochMilli());
        }
        if (targetType == LocalDate.class) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (targetType == LocalTime.class) {
            return instant.atZone(ZoneId.systemDefault()).toLocalTime();
        }
        if (targetType == LocalDateTime.class) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (targetType == ZonedDateTime.class) {
            return instant.atZone(ZoneId.systemDefault());
        }

        return value;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
