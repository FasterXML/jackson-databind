package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for "vanilla" processing of Builder-based deserialization when
 * deserializer is called with parser positioned at a property name
 * (or end-of-object), as happens for polymorphic types with type id
 * as the first property (see [databind#6219]).
 */
public class BuilderVanillaTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(@JsonSubTypes.Type(value = PointXY.class, name = "xy"))
    interface Base { }

    @JsonDeserialize(builder = PointXY.Builder.class)
    static class PointXY implements Base
    {
        final int x, y;

        PointXY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Immutable builder: verify value returned by "with" method is used
        static class Builder {
            final int x, y;

            public Builder() { this(0, 0); }

            Builder(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public Builder withX(int v) { return new Builder(v, y); }
            public Builder withY(int v) { return new Builder(x, v); }

            public PointXY build() { return new PointXY(x, y); }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void startAtPropertyName() throws Exception
    {
        PointXY p = (PointXY) MAPPER.readValue("""
                {"type":"xy","x":1,"y":2}
                """, Base.class);
        assertEquals(1, p.x);
        assertEquals(2, p.y);
    }

    @Test
    void startAtEndObject() throws Exception
    {
        PointXY p = (PointXY) MAPPER.readValue("""
                {"type":"xy"}
                """, Base.class);
        assertEquals(0, p.x);
        assertEquals(0, p.y);
    }

    @Test
    void unknownPropertyFirst() throws Exception
    {
        PointXY p = (PointXY) MAPPER.readerFor(Base.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue("""
                {"type":"xy","z":3,"x":1,"y":2}
                """);
        assertEquals(1, p.x);
        assertEquals(2, p.y);
    }

    @Test
    void failForFirstProperty() throws Exception
    {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> MAPPER.readValue("""
                {"type":"xy","x":"notAnInt","y":2}
                """, Base.class));
        assertEquals("x", e.getPath().get(e.getPath().size() - 1).getPropertyName());
    }
}
