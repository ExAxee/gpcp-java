package org.gpcp.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonBuilder;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;

import org.gpcp.types.AggregateTypeConverter;
import org.gpcp.types.JsonSerializableTypeConverter;
import org.gpcp.types.TypeConverter;

import static org.gpcp.types.JsonSerializableTypeConverter.TypeId.*;

public abstract class BaseHandler {
    protected Map<String, CommandData> methodMap;
    protected TypeConverter<Object> typeConverter;
    protected BaseHandler extendingHandler;

    /**
     * Called when no declared command has a command trigger matching the received one
     * @param commandTrigger the received string that should have triggered a command
     * @param arguments the received json array with all arguments provided to the command
     * @return an object of a type convertible to json using one of the provided type converters
     */
    public abstract Object unknownCommand(final String commandTrigger,
                                          final JsonArray arguments);

    @Command
    public JsonArray requestCommands() {
        final JsonArray serializedCommands = new JsonArray();
        for (final Map.Entry<String, CommandData> commandData : methodMap.entrySet()) {
            serializedCommands.add(commandData.getValue()
                    .getJsonSerializedCommand(commandData.getKey(), typeConverter));
        }
        return serializedCommands;
    }


    final void setMethodMap(final Map<String, CommandData> functionMap) {
        this.methodMap = functionMap;
    }

    final void setTypeConverter(final TypeConverter<Object> typeConverter) {
        this.typeConverter = typeConverter;
    }

    final void setExtendingHandler(final BaseHandler extendingHandler) {
        this.extendingHandler = extendingHandler;
    }


    private static Class<?> typeToClass(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return typeToClass(((ParameterizedType) type).getRawType());
        } else {
            throw new IllegalArgumentException(
                    "Type " + type + " is neither a Class nor a ParameterizedType");
        }
    }


    public String handleData(final String data) {
        final int separatorIndex = data.indexOf("[");
        final String commandTrigger = data.substring(0, separatorIndex);

        Object result;
        final JsonArray arguments;
        try {
            arguments = JsonParser.array().from(data.substring(separatorIndex));

            final CommandData command = methodMap.get(commandTrigger);
            if (command == null) {
                result = unknownCommand(commandTrigger, arguments);
            } else {
                final Type[] methodArguments = command.method.getParameterTypes();
                final Object[] convertedArguments = new Object[methodArguments.length];
                for (int i = 0; i < methodArguments.length; i++) {
                    System.out.println(arguments.get(i) + " " + arguments.get(i).getClass());
                    convertedArguments[i] =
                            typeConverter.fromJson(arguments.get(i), typeToClass(methodArguments[i]));
                }

                try {
                    result = command.method.invoke(extendingHandler, convertedArguments);

                } catch (IllegalAccessException | InvocationTargetException e) {
                    // TODO proper error handling
                    e.printStackTrace();
                    result = e.getMessage();
                }
            }

        } catch (JsonParserException e) {
            // TODO proper error handling
            e.printStackTrace();
            result = e.getMessage();
        }

        return JsonWriter.string(typeConverter.toJson(result));
    }


    public static final class Factory<Handler extends BaseHandler> {
        private final Callable<Handler> handlerBuilder;
        private final Map<String, CommandData> methodMap;
        private final AggregateTypeConverter aggregateTypeConverter;

        public Factory(final Class<Handler> clazz, final Callable<Handler> handlerBuilder) {
            this.handlerBuilder = handlerBuilder;
            this.methodMap = new HashMap<>();
            this.aggregateTypeConverter = new AggregateTypeConverter(
                    new JsonSerializableTypeConverter<>(jsonObjectId, JsonObject.class),
                    new JsonSerializableTypeConverter<>(jsonObjectId, JsonArray.class),
                    new JsonSerializableTypeConverter<>(stringId, String.class),
                    new JsonSerializableTypeConverter<>(integerId, Boolean.class),
                    new JsonSerializableTypeConverter<>(integerId, Integer.class),
                    new JsonSerializableTypeConverter<>(integerId, Long.class),
                    new JsonSerializableTypeConverter<>(floatId, Float.class),
                    new JsonSerializableTypeConverter<>(floatId, Double.class),
                    new JsonSerializableTypeConverter<>(integerId, Number.class));

            for (final Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Command.class)) {
                    final Command command = method.getAnnotation(Command.class);

                    final String trigger;
                    if (command.trigger().equals("")) {
                        trigger = method.getName();
                    } else {
                        trigger = command.trigger();
                    }

                    method.setAccessible(true);
                    methodMap.put(trigger, new CommandData(method,
                            command.description(), command.argumentNames()));
                }
            }
        }

        /**
         * @param typeConverter use this type converter to convert types in the built handlers
         * @return {@code this}
         */
        public Factory<Handler> addTypeConverter(final TypeConverter<?> typeConverter) {
            aggregateTypeConverter.addTypeConverter(typeConverter);
            return this;
        }

        public Handler buildHandler() throws Exception {
            final Handler handler = handlerBuilder.call();
            handler.setMethodMap(methodMap);
            handler.setTypeConverter(aggregateTypeConverter);
            handler.setExtendingHandler(handler);
            return handler;
        }
    }

    private static final class CommandData {
        final Method method;
        final String description;
        final String[] argumentNames;

        CommandData(final Method method,
                    final String description,
                    final String[] argumentNames) {
            this.method = method;
            this.description = description;
            this.argumentNames = argumentNames;
        }

        JsonObject getJsonSerializedCommand(final String trigger, final TypeConverter<Object> typeConverter) {
            final JsonBuilder<JsonObject> arrayBuilder = JsonObject.builder()
                    .value("name", trigger)
                    .value("return_type",
                            typeConverter.typeId(typeToClass(method.getReturnType())))
                    .value("description", description.isEmpty() ? null : description)
                    .array("arguments");

            final Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                final String name;
                if (i < argumentNames.length) {
                    name = argumentNames[i];
                } else if (parameters[i].isNamePresent()) {
                    name = parameters[i].getName();
                } else {
                    name = String.valueOf(i);
                }

                arrayBuilder.object()
                        .value("type",
                                typeConverter.typeId(typeToClass(parameters[i].getType())))
                        .value("name", name)
                        .end();
            }

            return arrayBuilder.end().done();
        }
    }
}
