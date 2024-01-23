package tools.jackson.databind.jsontype.ext;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;

// [databind#2588] / [databind#2610]
public class ExternalTypeId2588Test extends BaseMapTest
{
    // [databind#2588]
    interface Animal { }

    static class Cat implements Animal { }

    public static class Dog implements Animal { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Pet {
        final String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        @JsonTypeIdResolver(AnimalTypeIdResolver.class)
        private final Animal animal;

        @JsonCreator
        public Pet(@JsonProperty("type") String type,
                   @JsonProperty("animal") Animal animal) {
            this.type = type;
            this.animal = animal;
        }
    }

    static class AnimalTypeIdResolver extends TypeIdResolverBase {
        private static final long serialVersionUID = 1L;

        @Override
        public String idFromValue(DatabindContext context, Object value) {
            return idFromValueAndType(context, value, value.getClass());
        }

        @Override
        public String idFromValueAndType(DatabindContext context,
                Object value, Class<?> suggestedType) {
            if (suggestedType.isAssignableFrom(Cat.class)) {
                return "cat";
            } else if (suggestedType.isAssignableFrom(Dog.class)) {
                return "dog";
            }
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            if ("cat".equals(id)) {
                return context.constructType(Cat.class);
            } else if ("dog".equals(id)) {
                return context.constructType(Dog.class);
            }
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.NAME;
        }
    }

    // [databind#2588]
    public void testExternalTypeId2588() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        Pet pet;

        // works?

        pet = mapper.readValue(a2q(
"{\n" +
"  'type': 'cat',\n" +
"  'animal': { },\n" +
"  'ignoredObject\": {\n" +
"    'someField': 'someValue'\n" +
"  }"+
"}"
                ), Pet.class);
        assertNotNull(pet);

        // fails:
        pet = mapper.readValue(a2q(
"{\n" +
"  'animal\": { },\n" +
"  'ignoredObject': {\n" +
"    'someField': 'someValue'\n" +
"  },\n" +
"  'type': 'cat'\n" +
"}"
                ), Pet.class);
        assertNotNull(pet);
    }
}
