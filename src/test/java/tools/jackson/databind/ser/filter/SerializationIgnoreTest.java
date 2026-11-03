package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonIgnore} and {@link JsonIgnoreType} annotations
 * with bean serialization.
 */
public class SerializationIgnoreTest extends DatabindTestUtil
{
    // Class for testing enabled @JsonIgnore annotation
    static class SizeClassEnabledIgnore
    {
        @JsonIgnore public int getY() { return 9; }

        public int getX() { return 1; }

        @JsonIgnore public int getY2() { return 1; }
        @JsonIgnore public int getY3() { return 2; }
    }

    // Class for testing disabled @JsonIgnore annotation
    static class SizeClassDisabledIgnore
    {
        public int getX() { return 3; }
        @JsonIgnore(false) public int getY() { return 4; }
    }

    static class BaseClassIgnore
    {
        @JsonProperty("x")
        @JsonIgnore
        public int x() { return 1; }

        public int getY() { return 2; }
    }

    static class SubClassNonIgnore extends BaseClassIgnore
    {
        @Override
        @JsonIgnore(false)
        public int x() { return 3; }
    }

    @JsonIgnoreType
    static class IgnoredType { }

    @JsonIgnoreType(false)
    static class NonIgnoredType
    {
        public int value = 13;

        public IgnoredType ignored = new IgnoredType();
    }

    // [databind#3357]: Precedence of @JsonIgnore over @JsonProperty
    static class IgnoreAndProperty3357 {
        public int toInclude = 2;

        @JsonIgnore
        @JsonProperty
        int toIgnore = 3;

        public int getToIgnore() { return toIgnore; }
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSimpleIgnore() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassEnabledIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertNull(result.get("y"));
    }

    @Test
    public void testDisabledIgnore() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassDisabledIgnore());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
        assertEquals(Integer.valueOf(4), result.get("y"));
    }

    @Test
    public void testIgnoreOver() throws Exception
    {
        // should only see "y"
        Map<String,Object> result = writeAndMap(MAPPER, new BaseClassIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(2), result.get("y"));

        // Should see "x" and "y"
        result = writeAndMap(MAPPER, new SubClassNonIgnore());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));
    }

    @Test
    public void testIgnoreType() throws Exception
    {
        assertEquals("{\"value\":13}", MAPPER.writeValueAsString(new NonIgnoredType()));
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore vs @JsonProperty precedence [databind#3357]
    /**********************************************************************
     */

    // [databind#3357]
    @Test
    public void testPropertyVsIgnore3357() throws Exception
    {
        assertEquals("{\"toInclude\":2}", MAPPER.writeValueAsString(new IgnoreAndProperty3357()));
    }
}
