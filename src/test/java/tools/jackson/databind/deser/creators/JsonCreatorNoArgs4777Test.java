package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

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
        public ValueInstantiator modifyValueInstantiator(
                DeserializationConfig config,
                BeanDescription.Supplier beanDescRef,
                ValueInstantiator defaultInstantiator
        ) {
            if (beanDescRef.getBeanClass() == Foo4777.class) {
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
        public ValueInstantiator findValueInstantiator(DeserializationConfig config,
                BeanDescription.Supplier beanDescRef) {
            return null;
        }
    }

    // For [databind#4777]
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
