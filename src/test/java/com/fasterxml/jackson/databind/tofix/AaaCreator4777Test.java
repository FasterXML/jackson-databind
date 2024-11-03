package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

public class AaaCreator4777Test extends DatabindTestUtil
{
    static class Foo {
        String whoMadeWho;

        protected Foo() { whoMadeWho = "creator"; } 
        
        @JsonCreator
        static Foo create() {
            Foo foo = new Foo();
            foo.whoMadeWho = "factory";
            return foo;
        }
    }

    @SuppressWarnings("serial")
    static class Instantiator extends StdValueInstantiator {
        public Instantiator(StdValueInstantiator src) {
            super(src);
        }
    }

    static class Instantiators implements ValueInstantiators {
        Instantiator last = null;

        @Override
        public ValueInstantiator findValueInstantiator(
                DeserializationConfig config,
                BeanDescription beanDesc,
                ValueInstantiator defaultInstantiator
        ) {
            if (defaultInstantiator instanceof StdValueInstantiator) {
                Instantiator instantiator = new Instantiator((StdValueInstantiator) defaultInstantiator);
                last = instantiator;
                return instantiator;
            } else {
                return defaultInstantiator;
            }
        }
    }

    @Test
    public void test() throws Exception {
        Instantiators i = new Instantiators();

        SimpleModule sm = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addValueInstantiators(i);
            }
        };
        ObjectMapper mapper = JsonMapper.builder().addModule(sm).build();

        Foo result = mapper.readValue("{}", Foo.class);

        Assertions.assertEquals("factory", result.whoMadeWho);
        
        Assertions.assertNull(i.last.getWithArgsCreator());
    }
}
