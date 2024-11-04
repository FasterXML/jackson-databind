package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonCreatorNoArgs4777Test extends DatabindTestUtil
{
    static class Foo {
        private Foo() { } 

        @JsonCreator
        static Foo create() {
            return new Foo();
        }
    }

    static class Instantiators implements ValueInstantiators {
        @Override
        public ValueInstantiator modifyValueInstantiator(
                DeserializationConfig config,
                BeanDescription beanDesc,
                ValueInstantiator defaultInstantiator
        ) {
            if (beanDesc.getBeanClass() == Foo.class) {
                AnnotatedWithParams dc = defaultInstantiator.getDefaultCreator();
                if (!(dc instanceof AnnotatedMethod)
                        || !dc.getName().equals("create")) {
                    throw new IllegalArgumentException("Wrong DefaultCreator: should be static-method 'create()', is: "
                            +dc);
                }
            }
            return defaultInstantiator;
        }

        @Override
        public ValueInstantiator findValueInstantiator(DeserializationConfig config, BeanDescription beanDesc) {
            return null;
        }
    }

    // For [databind#4777]
    @SuppressWarnings("serial")
    @Test
    @JacksonTestFailureExpected
    public void testCreatorDetection4777() throws Exception {
        SimpleModule sm = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addValueInstantiators(new Instantiators());
            }
        };
        ObjectMapper mapper = JsonMapper.builder().addModule(sm).build();

        Foo result = mapper.readValue("{}", Foo.class);
        assertNotNull(result);
    }
}
