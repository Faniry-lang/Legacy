package legacy.schema;

public class ColumnData {
    String nameFromDb;
    String nameFromEntity;

    public ColumnData(String nameFromDb, String nameFromEntity) {
        this.nameFromDb = nameFromDb;
        this.nameFromEntity = nameFromEntity;
    }

    public ColumnData() {
    }

    public String getNameFromDb() {
        return nameFromDb;
    }

    public void setNameFromDb(String nameFromDb) {
        this.nameFromDb = nameFromDb;
    }

    public String getNameFromEntity() {
        return nameFromEntity;
    }

    public void setNameFromEntity(String nameFromEntity) {
        this.nameFromEntity = nameFromEntity;
    }
}
