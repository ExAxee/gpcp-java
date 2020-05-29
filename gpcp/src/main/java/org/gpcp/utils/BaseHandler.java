package org.gpcp.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class BaseHandler {
    protected Map<String, Method> functionMap;

    final void setFunctions(final Map<String, Method> functionMap) {
        this.functionMap = functionMap;
    }

    public String handleData(final String data) {

    }

    public static final class Factory<Handler extends BaseHandler> {
        private final Callable<Handler> handlerBuilder;
        private final Map<String, Method> functionMap;

        public Factory(final Class<Handler> clazz, final Callable<Handler> handlerBuilder) {
            this.handlerBuilder = handlerBuilder;
            this.functionMap = new HashMap<>();

            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Command.class)) {
                    functionMap.put(method.getAnnotation(Command.class).trigger(), method);
                }
            }
        }

        public Handler buildHandler() throws Exception {
            Handler handler = handlerBuilder.call();
            handler.setFunctions(functionMap);
            return handler;
        }
    }
}
