package org.gpcp.types;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

public interface TypeConverter<T> {
    /**
     * @param json the parsed json to be converted to an instance of {@link T}. Could be of type
     *             {@link JsonObject}, {@link JsonArray}, {@link String}, {@link Number},
     *             {@code boolean}, {@code int}, {@code long}, {@code float}, {@code double}
     * @return a newly built instance of {@link T}
     */
    T fromJson(Object json, Class<?> expectedClass);

    /**
     * @param object the object to convert to json (instance of {@link T})
     * @return an instance of a json type or a json-serializable type, that is: {@link JsonObject},
     *         {@link JsonArray}, {@link String}, {@link Number}, {@code boolean}, {@code int},
     *         {@code long}, {@code float}, {@code double}
     */
    Object toJson(Object object);

    /**
     * @param targetClass the target class
     * @return whether this type converter is able to convert to and from the provided target class
     */
    boolean accepts(Class<?> targetClass);

    /**
     * @param targetClass the target class
     * @return an unique integer id that identifies the type
     */
    Integer typeId(Class<?> targetClass);
}
