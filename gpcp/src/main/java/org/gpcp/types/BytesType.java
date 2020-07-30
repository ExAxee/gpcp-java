package org.gpcp.types;

import java.nio.charset.StandardCharsets;

public class BytesType implements TypeConverter<byte[]> {
    @Override
    public byte[] fromJson(final Object json, final Class<?> expectedClass) {
        if (json instanceof String) {
            return ((String) json).getBytes(StandardCharsets.US_ASCII);
        } else {
            throw TypeConverter.getClassCastException(String.class, json.getClass());
        }
    }

    @Override
    public Object toJson(final Object object) {
        if (object instanceof byte[]) {
            return new String((byte[]) object, StandardCharsets.US_ASCII);
        } else {
            throw TypeConverter.getClassCastException(byte[].class, object.getClass());
        }
    }

    @Override
    public boolean accepts(final Class<?> targetClass) {
        return targetClass.equals(byte[].class);
    }

    @Override
    public int typeId(final Class<?> targetClass) {
        return TypeId.bytesId.getId();
    }
}
