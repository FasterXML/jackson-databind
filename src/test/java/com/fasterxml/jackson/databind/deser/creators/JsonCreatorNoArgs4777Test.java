package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonCreatorNoArgs4777Test extends DatabindTestUtil
{
    static class Foo4777 {
        Foo4777() { }

        @JsonCreator
        static Foo4777 create() {
            return new Foo4777();
        }
    }

    static class Instantiators4777 implements ValueInstantiators {
        @Override
        public ValueInstantiator findValueInstantiator(
                DeserializationConfig config,
                BeanDescription beanDesc,
                ValueInstantiator defaultInstantiator
        ) {
            if (beanDesc.getBeanClass() == Foo4777.class) {
                AnnotatedWithParams dc = defaultInstantiator.getDefaultCreator();
                if (!(dc instanceof AnnotatedMethod)
                        || !dc.getName().equals("create")) {
                    throw new IllegalArgumentException("Wrong DefaultCreator: should be static-method 'create()', is: "
                            +dc);
                }
            }
            return defaultInstantiator;
        }
    }

    // For [databind#4777]
    @SuppressWarnings("serial")
    @Test
    public void testCreatorDetection4777() throws Exception {
        SimpleModule sm = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addValueInstantiators(new Instantiators4777());
            }
        };
        ObjectMapper mapper = JsonMapper.builder().addModule(sm).build();

        Foo4777 result = mapper.readValue("{}", Foo4777.class);
        assertNotNull(result);
    }
}
