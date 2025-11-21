package tools.jackson.databind.ser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Test for [databind#5414]
@SuppressWarnings("serial")
public class ValueSerializerModifier5414Test extends DatabindTestUtil
{
    // HiddenFieldModule should prevent the output of the password field.
    record User(String name, @Hidden String password) {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface Hidden {}

    static class HiddenFieldModule extends SimpleModule {
        @Override
        public void setupModule(SetupContext context) {
          super.setupModule(context);
          context.addSerializerModifier(new HiddenFieldRemover());
        }
    }

    static class HiddenFieldRemover extends ValueSerializerModifier {
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
            return beanProperties.stream()
                    .filter(writer -> !isHidden(writer.getMember()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private boolean isHidden(AnnotatedMember member) {
            if (member.annotations() == null) {
                return false;
            }
            return member
                    .annotations()
                    .anyMatch(annotation -> annotation.annotationType().equals(Hidden.class));
        }
    }

    // [databind#5414]
    @Test
    public void annotationsAccessIssue5414()
    {
        var mapper = JsonMapper.builder()
                .addModule(new HiddenFieldModule())
                .build();
        User user = new User("John", "123456");
        String userJson = mapper.writeValueAsString(user);
        assertEquals("{\"name\":\"John\"}", userJson);
    }
}
