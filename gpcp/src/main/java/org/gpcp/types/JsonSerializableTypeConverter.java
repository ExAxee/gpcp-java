package org.gpcp.types;

public final class JsonSerializableTypeConverter<T> implements TypeConverter<T> {

    public enum TypeId {
        stringId(1),
        integerId(2),
        floatId(3),
        jsonObjectId(4),
        ;

        private final int id;
        TypeId(int id) {
            this.id = id;
        }
    }


    private final TypeId typeId;
    private final Class<T> targetClass;

    public JsonSerializableTypeConverter(final TypeId typeId, final Class<T> targetClass) {
        this.typeId = typeId;
        this.targetClass = targetClass;
    }

    @Override
    public T fromJson(final Object json, final Class<?> expectedClass) {
        return targetClass.cast(json);
    }

    @Override
    public Object toJson(final Object object) {
        return object;
    }

    @Override
    public boolean accepts(final Class<?> targetClass) {
        return this.targetClass.equals(targetClass);
    }

    @Override
    public Integer typeId(Class<?> targetClass) {
        return typeId.id;
    }
}
