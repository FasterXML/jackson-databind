package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javax.xml.bind.JAXBElement;

public class JAXBElementSerializer extends StdSerializer<JAXBElement<?>> {

    protected JAXBElementSerializer(Class<?> t) {
        super(t);
    }

    final static JAXBElementSerializer instance = new JAXBElementSerializer(JAXBElement.class);

    @Override
    public void serialize(JAXBElement<?> value, JsonGenerator gen, SerializerProvider provider) throws JacksonException {
        if (value.isNil()) {
            gen.writeNull();
        } else {
            gen.writePOJO(value.getValue());
        }
    }
}
