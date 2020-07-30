package org.gpcp.types;

public final class JsonSerializableTypeConverter<T> implements TypeConverter<T> {

    private final TypeId typeId;
    private final Class<T> targetClass;
    private final Class<T> primitiveTargetClass;

    public JsonSerializableTypeConverter(final TypeId typeId, final Class<T> targetClass) {
        this(typeId, targetClass, null);
    }

    public JsonSerializableTypeConverter(final TypeId typeId,
                                         final Class<T> targetClass,
                                         final Class<T> primitiveTargetClass) {
        this.typeId = typeId;
        this.targetClass = targetClass;
        this.primitiveTargetClass = primitiveTargetClass;
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
        return this.targetClass.equals(targetClass)
                || (primitiveTargetClass != null && primitiveTargetClass.equals(targetClass));
    }

    @Override
    public int typeId(Class<?> targetClass) {
        return typeId.getId();
    }
}
