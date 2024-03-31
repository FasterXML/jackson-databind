package tools.jackson.databind.deser.creators.jdk8;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

public class JsonCreatorTest extends DatabindTestUtil
{
    static class ClassWithJsonCreatorOnStaticMethod {
        final String first;
        final String second;

        ClassWithJsonCreatorOnStaticMethod(String first, String second) {
             this.first = first;
             this.second = second;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static ClassWithJsonCreatorOnStaticMethod factory(String first, String second) {
             return new ClassWithJsonCreatorOnStaticMethod(first, second);
        }
   }

    @Test
    public void testJsonCreatorOnStaticMethod() throws Exception {
        ObjectMapper objectMapper = newJsonMapper();

        String json = a2q("{'first':'1st','second':'2nd'}");
        ClassWithJsonCreatorOnStaticMethod actual = objectMapper.readValue(json, ClassWithJsonCreatorOnStaticMethod.class);

        then(actual).isEqualToComparingFieldByField(new ClassWithJsonCreatorOnStaticMethod("1st", "2nd"));
    }
}
