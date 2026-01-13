package legacy.query;

public class Filter {
    String fieldName;
    Comparator comparator;
    Object value;

    public Filter(String fieldName, Comparator comparator, Object value) {
        this.fieldName = fieldName;
        this.comparator = comparator;
        this.value = value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
