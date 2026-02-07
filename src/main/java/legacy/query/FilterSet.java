package legacy.query;

import java.util.ArrayList;
import java.util.List;

public class FilterSet {
    List<Filter> filters;

    public FilterSet() {
        this.filters = new ArrayList<>();
    }

    public void add(String fieldName, Comparator comparator, Object value) {
        this.filters.add(new Filter(fieldName, comparator, value));
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }
}
