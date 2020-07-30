package org.gpcp.utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

import org.gpcp.types.AggregateTypeConverter;
import org.gpcp.types.JsonSerializableTypeConverter;
import org.gpcp.types.TypeConverter;

public abstract class BaseHandler {
    protected Map<String, CommandData> functionMap;
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


    final void setFunctions(final Map<String, CommandData> functionMap) {
        this.functionMap = functionMap;
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
        final JsonArray arguments = JsonArray.from(data.substring(separatorIndex));

        final CommandData command = functionMap.get(commandTrigger);
        Object result;
        if (command == null) {
            result = unknownCommand(commandTrigger, arguments);
        } else {
            final Type[] functionArguments = command.function.getParameterTypes();
            final Object[] convertedArguments = new Object[functionArguments.length];
            for (int i = 0; i < functionArguments.length; i++) {
                convertedArguments[i] =
                        typeConverter.fromJson(arguments.get(i), typeToClass(functionArguments[i]));
            }

            try {
                result = command.function.invoke(extendingHandler, convertedArguments);
                
            } catch (IllegalAccessException | InvocationTargetException e) {
                // TODO proper error handling
                e.printStackTrace();
                result = e.getMessage();
            }
        }

        return JsonWriter.string(typeConverter.toJson(result));
    }


    public static final class Factory<Handler extends BaseHandler> {
        private final Callable<Handler> handlerBuilder;
        private final Map<String, CommandData> functionMap;
        private final AggregateTypeConverter aggregateTypeConverter;

        public Factory(final Class<Handler> clazz, final Callable<Handler> handlerBuilder) {
            this.handlerBuilder = handlerBuilder;
            this.functionMap = new HashMap<>();
            this.aggregateTypeConverter = new AggregateTypeConverter(
                    new JsonSerializableTypeConverter<>(JsonObject.class),
                    new JsonSerializableTypeConverter<>(JsonArray.class),
                    new JsonSerializableTypeConverter<>(String.class),
                    new JsonSerializableTypeConverter<>(Boolean.class),
                    new JsonSerializableTypeConverter<>(Integer.class),
                    new JsonSerializableTypeConverter<>(Long.class),
                    new JsonSerializableTypeConverter<>(Float.class),
                    new JsonSerializableTypeConverter<>(Double.class),
                    new JsonSerializableTypeConverter<>(Number.class));

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
                    functionMap.put(trigger, new CommandData(method, command.description()));
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
            handler.setFunctions(functionMap);
            handler.setTypeConverter(aggregateTypeConverter);
            handler.setExtendingHandler(handler);
            return handler;
        }
    }

    private static final class CommandData {
        final Method function;
        final String description;

        CommandData(final Method function, final String description) {
            this.function = function;
            this.description = description;
        }
    }
}
