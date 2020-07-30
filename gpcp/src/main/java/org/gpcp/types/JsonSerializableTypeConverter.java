package org.gpcp.types;

public final class JsonSerializableTypeConverter<T> implements TypeConverter<T> {
    private final Class<T> targetClass;

    public JsonSerializableTypeConverter(final Class<T> targetClass) {
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
}
