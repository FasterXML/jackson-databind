package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3547]: When combining `As.EXTERNAL_PROPERTY` with `@JsonValue`, the serializer
// used to omit the external type id field when the `@JsonValue` return value was null.
public class ExternalPropertyWithJsonValueNull3547Test extends DatabindTestUtil
{
    public interface GenericType {
        String getValue();
        void setValue(String value);
    }

    public static abstract class AbstractGenericType implements GenericType {
        protected String value;
        public AbstractGenericType() { }
        public AbstractGenericType(String value) { this.value = value; }
        @Override @JsonValue public String getValue() { return value; }
        @Override public void setValue(String value) { this.value = value; }
    }

    public static class FooType extends AbstractGenericType {
        public FooType() { super(); }
        @JsonCreator public FooType(String value) { super(value); }
    }

    public static class Container {
        @JsonTypeInfo(use = Id.CLASS, include = As.EXTERNAL_PROPERTY, property = "type", visible = true)
        @JsonSubTypes(value = { @Type(value = FooType.class) })
        protected GenericType value;

        public GenericType getValue() { return value; }
        public void setValue(GenericType value) { this.value = value; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void serializeWithNonNullJsonValue() throws Exception {
        Container container = new Container();
        container.setValue(new FooType("foobar"));
        String json = MAPPER.writeValueAsString(container);
        assertEquals(
            "{\"value\":\"foobar\",\"type\":\"" + FooType.class.getName() + "\"}",
            json);
    }

    // The type id must still be emitted even when the `@JsonValue` accessor returns null,
    // as long as the containing bean itself is non-null.
    @Test
    public void serializeWithNullJsonValueShouldStillEmitTypeId() throws Exception {
        Container container = new Container();
        container.setValue(new FooType()); // value bean present, but @JsonValue getter returns null
        String json = MAPPER.writeValueAsString(container);
        assertEquals(
            "{\"value\":null,\"type\":\"" + FooType.class.getName() + "\"}",
            json);
    }

    // Sanity check: if the bean itself is null, no type id should be emitted.
    @Test
    public void serializeWithNullBean() throws Exception {
        Container container = new Container();
        String json = MAPPER.writeValueAsString(container);
        assertEquals("{\"value\":null}", json);
    }

    @Test
    public void deserializeWithNonNullJsonValue() throws Exception {
        String json = "{\"value\":\"foobar\",\"type\":\"" + FooType.class.getName() + "\"}";
        Container container = MAPPER.readValue(json, Container.class);
        assertNotNull(container.value);
        assertInstanceOf(FooType.class, container.value);
        assertEquals("foobar", container.value.getValue());
    }

    @Test
    public void roundtripWithNonNullJsonValue() throws Exception {
        Container in = new Container();
        in.setValue(new FooType("foobar"));
        String json = MAPPER.writeValueAsString(in);
        Container out = MAPPER.readValue(json, Container.class);
        assertInstanceOf(FooType.class, out.value);
        assertEquals("foobar", out.value.getValue());
    }

    // 18-Apr-2026, tatu: Note: no deser test for surrogate `null` value as
    //   it is not quite clear what should happen (or rather, how to make
    //   round-trip handling work as expected)
}
