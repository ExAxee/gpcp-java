package org.gpcp.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AggregateTypeConverter implements TypeConverter<Object> {
    private final List<TypeConverter<?>> typeConverters;

    public AggregateTypeConverter(final TypeConverter<?>... typeConverters) {
        this.typeConverters = new ArrayList<>(Arrays.asList(typeConverters));
    }

    public void addTypeConverter(final TypeConverter<?> typeConverter) {
        typeConverters.add(typeConverter);
    }


    @Override
    public final Object fromJson(final Object json, final Class<?> expectedClass) {
        for (final TypeConverter<?> typeConverter : typeConverters) {
            if (typeConverter.accepts(expectedClass)) {
                return typeConverter.fromJson(json, expectedClass);
            }
        }
        throw classCastException(expectedClass);
    }

    @Override
    public final Object toJson(final Object object) {
        for (final TypeConverter<?> typeConverter : typeConverters) {
            if (typeConverter.accepts(object.getClass())) {
                return typeConverter.toJson(object);
            }
        }
        throw classCastException(object.getClass());
    }

    @Override
    public boolean accepts(final Class<?> targetClass) {
        for (final TypeConverter<?> typeConverter : typeConverters) {
            if (typeConverter.accepts(targetClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer typeId(final Class<?> targetClass) {
        for (final TypeConverter<?> typeConverter : typeConverters) {
            if (typeConverter.accepts(targetClass)) {
                return typeConverter.typeId(targetClass);
            }
        }
        throw classCastException(targetClass);
    }

    private ClassCastException classCastException(final Class<?> clazz) {
        return new ClassCastException("No type converter available for class " + clazz.getName());
    }
}
