package org.gpcp.types;

public enum TypeId {
    jsonObjectId(0),
    jsonArrayId(1),
    stringId(2),
    booleanId(3),
    integerId(4),
    floatId(5),
    bytesId(6);

    private final int id;
    TypeId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
