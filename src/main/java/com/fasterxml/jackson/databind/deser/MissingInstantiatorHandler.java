package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ValueDeserializer;
import com.fasterxml.jackson.databind.util.ReflectionUtil;

public class MissingInstantiatorHandler extends DeserializationProblemHandler {

    private static volatile MissingInstantiatorHandler INSTANCE;

    private MissingInstantiatorHandler() {
    }

    public static MissingInstantiatorHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (MissingInstantiatorHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MissingInstantiatorHandler();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, ValueInstantiator valueInsta,
                                            JsonParser jsonParser, String msg) throws JacksonException {
        Object instance = ReflectionUtil.newConstructorAndCreateInstance(instClass);
        if (instance == null) {
            return NOT_HANDLED;
        }

        ValueDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(ctxt.constructType(instClass));
        if (deserializer != null) {
            return deserializer.deserialize(jsonParser, ctxt, instance);
        }
        return NOT_HANDLED;
    }
}
