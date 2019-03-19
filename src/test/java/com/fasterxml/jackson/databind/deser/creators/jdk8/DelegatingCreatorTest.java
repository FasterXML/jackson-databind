package com.fasterxml.jackson.databind.deser.creators.jdk8;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;

import org.junit.*;

import java.util.*;

import static org.assertj.core.api.BDDAssertions.*;

public class DelegatingCreatorTest
{
    static class ClassWithDelegatingCreator {
        private final String value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        ClassWithDelegatingCreator(Map<String, String> props) {
             this.value = props.get("value");
        }

        String getValue() {
             return value;
        }
   }

   public static class IntWrapper
   {
        private final int value;

        @JsonCreator(mode=JsonCreator.Mode.PROPERTIES)
        public IntWrapper(int value) {
             this.value = value;
        }

        public int getValue() {
             return value;
        }
   }

   public static class GenericWrapper<T>
   {
        private final T value;

        @JsonCreator(mode=JsonCreator.Mode.PROPERTIES)
        public GenericWrapper(T value) {
             this.value = value;
        }

        public T getValue() {
             return value;
        }
   }


   /*
	@Test
	public void shouldNotOverrideJsonCreatorAnnotationWithSpecifiedMode() throws IOException {

		// given
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

		// when
		ClassWithDelegatingCreator actual = objectMapper.readValue("{\"value\":\"aValue\"}",
		        ClassWithDelegatingCreator.class);

		// then
		Map<String, String> props = new HashMap<>();
		props.put("value", "aValue");
		ClassWithDelegatingCreator expected = new ClassWithDelegatingCreator(props);
		then(actual).isEqualToComparingFieldByField(expected);
	}
	*/

   @Test
   public void shouldDeserializeIntWrapper() throws Exception {
       ObjectMapper mapper = new ObjectMapper();
       IntWrapper actual = mapper.readValue
               ("{\"value\":13}", IntWrapper.class);
       then(actual).isEqualToComparingFieldByField(new IntWrapper(13));
   }

   @Test
   public void shouldDeserializeGenericWrapper() throws Exception {
       ObjectMapper mapper = new ObjectMapper();
       GenericWrapper<String> actual = mapper.readValue
               ("{\"value\":\"aValue\"}", new TypeReference<GenericWrapper<String>>() { });
       then(actual).isEqualToComparingFieldByField(new GenericWrapper<>("aValue"));
   }
}
