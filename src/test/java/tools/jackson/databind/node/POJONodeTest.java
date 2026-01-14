package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.ser.std.StdSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class POJONodeTest extends NodeTestBase
{
    @JsonSerialize(using = CustomSer.class)
    public static class Data {
        public String aStr;
    }

    public static class CustomSer extends StdSerializer<Data> {
        public CustomSer() {
            super(Data.class);
        }

        @Override
        public void serialize(Data value, JsonGenerator gen, SerializationContext provider)
        {
            String attrStr = (String) provider.getAttribute("myAttr");
            gen.writeStartObject();
            gen.writeStringProperty("aStr", "The value is: " + (attrStr == null ? "NULL" : attrStr));
            gen.writeEndObject();
        }
    }

    final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPOJONodeCustomSer() throws Exception
    {
      Data data = new Data();
      data.aStr = "Hello";

      Map<String, Object> mapTest = new HashMap<>();
      mapTest.put("data", data);

      ObjectNode treeTest = MAPPER.createObjectNode();
      treeTest.putPOJO("data", data);

      final String EXP = "{\"data\":{\"aStr\":\"The value is: Hello!\"}}";

      String mapOut = MAPPER.writer().withAttribute("myAttr", "Hello!").writeValueAsString(mapTest);
      assertEquals(EXP, mapOut);

      String treeOut = MAPPER.writer().withAttribute("myAttr", "Hello!").writeValueAsString(treeTest);
      assertEquals(EXP, treeOut);
    }

    // [databind#3262]: The issue is that
    // `JsonNode.toString()` will use internal "default" ObjectMapper which
    // does not (and cannot) have modules for external datatypes, such as
    // Java 8 Date/Time types. So we'll catch IOException/RuntimeException for
    // POJONode, produce something like "[ERROR: (type) [msg]" StringNode for that case?
    @Test
    public void testAddJava8DateAsPojo() throws Exception
    {
        LocalDateTime dt = LocalDateTime.parse("2025-03-31T12:00");
        JsonNode node = MAPPER.createObjectNode().putPOJO("test", dt);
        String json = node.toString();
        assertNotNull(json);

        JsonNode result = MAPPER.readTree(json);
        String msg = result.path("test").asString();
        assertEquals(dt, LocalDateTime.parse(msg));
    }

    @Test
    public void testAsBoolean() {
        assertThat(new POJONode(null).asBoolean()).isFalse();
        assertThat(new POJONode(Boolean.TRUE).asBoolean()).isTrue();
        assertThat(new POJONode(Boolean.FALSE).asBoolean()).isFalse();
        assertThatThrownBy(() -> new POJONode(new Data()).asBoolean())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asBoolean()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `boolean`: value type not coercible");
    }

    @Test
    public void testAsBooleanDefaultValue() {
        assertThat(new POJONode(null).asBoolean(true)).isTrue();
        assertThat(new POJONode(Boolean.TRUE).asBoolean(false)).isTrue();
        assertThat(new POJONode(Boolean.FALSE).asBoolean(true)).isFalse();
        assertThat(new POJONode(new Data()).asBoolean(true)).isTrue();
    }

    @Test
    public void testAsBooleanOpt() {
        assertThat(new POJONode(null).asBooleanOpt()).isNotPresent();
        assertThat(new POJONode(Boolean.TRUE).asBooleanOpt()).hasValue(Boolean.TRUE);
        assertThat(new POJONode(Boolean.FALSE).asBooleanOpt()).hasValue(Boolean.FALSE);
        assertThat(new POJONode(new Data()).asBooleanOpt()).isNotPresent();
    }

    @Test
    public void testAsString() {
        assertThatThrownBy(() -> new POJONode(null).asString())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asString()` cannot coerce value"
                        + " {POJO of type [null]} to `java.lang.String`: value type not coercible");
        assertThat(new POJONode("test").asString()).isEqualTo("test");
        assertThatThrownBy(() -> new POJONode(new Data()).asString())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asString()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `java.lang.String`: value type not coercible");
    }

    @Test
    public void testAsStringDefaultValue() {
        assertThat(new POJONode(null).asString("fallback")).isEqualTo("fallback");
        assertThat(new POJONode("test").asString("fallback")).isEqualTo("test");
        assertThat(new POJONode(new Data()).asString("fallback")).isEqualTo("fallback");
    }

    @Test
    public void testAsStringOpt() {
        assertThat(new POJONode(null).asStringOpt()).isNotPresent();
        assertThat(new POJONode("test").asStringOpt()).hasValue("test");
        assertThat(new POJONode(new Data()).asStringOpt()).isNotPresent();
    }

    @Test
    public void testAsShort() {
        assertThat(new POJONode(null).asShort()).isEqualTo((short) 0);
        assertThat(new POJONode(99.99D).asShort()).isEqualTo((short) 99);
        assertThat(new POJONode(99L).asShort()).isEqualTo((short) 99);
        assertThat(new POJONode(99).asShort()).isEqualTo((short) 99);
        assertThat(new POJONode((short) 99).asShort()).isEqualTo((short) 99);
        assertThat(new POJONode((byte) 99).asShort()).isEqualTo((short) 99);
        assertThat(new POJONode(BigInteger.valueOf(99)).asShort()).isEqualTo((short) 99);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asShort()).isEqualTo((short) 99);
        assertThatThrownBy(() -> new POJONode(new Data()).asShort())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asShort()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `short`: value type not coercible");
    }

    @Test
    public void testAsShortDefaultValue() {
        assertThat(new POJONode(null).asShort((short) 10)).isEqualTo((short) 10);
        assertThat(new POJONode(99.99D).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode(99L).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode(99).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode((short) 99).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode((byte) 99).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode(BigInteger.valueOf(99)).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asShort((short) 10)).isEqualTo((short) 99);
        assertThat(new POJONode(new Data()).asShort((short) 10)).isEqualTo((short) 10);
    }

    @Test
    public void testAsShortOpt() {
        assertThat(new POJONode(null).asShortOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode(99L).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode(99).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode((short) 99).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode((byte) 99).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode(BigInteger.valueOf(99)).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asShortOpt()).hasValue((short) 99);
        assertThat(new POJONode(new Data()).asShortOpt()).isNotPresent();
    }

    @Test
    public void testAsInt() {
        assertThat(new POJONode(null).asInt()).isEqualTo(0);
        assertThat(new POJONode(99.99D).asInt()).isEqualTo(99);
        assertThat(new POJONode(99L).asInt()).isEqualTo(99);
        assertThat(new POJONode(99).asInt()).isEqualTo(99);
        assertThat(new POJONode((short) 99).asInt()).isEqualTo(99);
        assertThat(new POJONode((byte) 99).asInt()).isEqualTo(99);
        assertThat(new POJONode(BigInteger.valueOf(99)).asInt()).isEqualTo(99);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asInt()).isEqualTo(99);
        assertThatThrownBy(() -> new POJONode(new Data()).asInt())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asInt()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `int`: value type not coercible");
    }

    @Test
    public void testAsIntDefaultValue() {
        assertThat(new POJONode(null).asInt(10)).isEqualTo(10);
        assertThat(new POJONode(99.99D).asInt(10)).isEqualTo(99);
        assertThat(new POJONode(99L).asInt(10)).isEqualTo(99);
        assertThat(new POJONode(99).asInt(10)).isEqualTo(99);
        assertThat(new POJONode((short) 99).asInt(10)).isEqualTo(99);
        assertThat(new POJONode((byte) 99).asInt(10)).isEqualTo(99);
        assertThat(new POJONode(BigInteger.valueOf(99)).asInt(10)).isEqualTo(99);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asInt(10)).isEqualTo(99);
        assertThat(new POJONode(new Data()).asInt(10)).isEqualTo(10);
    }

    @Test
    public void testAsIntOpt() {
        assertThat(new POJONode(null).asIntOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asIntOpt()).hasValue(99);
        assertThat(new POJONode(99L).asIntOpt()).hasValue(99);
        assertThat(new POJONode(99).asIntOpt()).hasValue(99);
        assertThat(new POJONode((short) 99).asIntOpt()).hasValue(99);
        assertThat(new POJONode((byte) 99).asIntOpt()).hasValue(99);
        assertThat(new POJONode(BigInteger.valueOf(99)).asIntOpt()).hasValue(99);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asIntOpt()).hasValue(99);
        assertThat(new POJONode(new Data()).asIntOpt()).isNotPresent();
    }

    @Test
    public void testAsLong() {
        assertThat(new POJONode(null).asLong()).isEqualTo(0L);
        assertThat(new POJONode(99.99D).asLong()).isEqualTo(99L);
        assertThat(new POJONode(99L).asLong()).isEqualTo(99L);
        assertThat(new POJONode(99).asLong()).isEqualTo(99L);
        assertThat(new POJONode((short) 99).asLong()).isEqualTo(99L);
        assertThat(new POJONode((byte) 99).asLong()).isEqualTo(99L);
        assertThat(new POJONode(BigInteger.valueOf(99)).asLong()).isEqualTo(99L);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asLong()).isEqualTo(99L);
        assertThatThrownBy(() -> new POJONode(new Data()).asLong())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asLong()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `long`: value type not coercible");
    }

    @Test
    public void testAsLongDefaultValue() {
        assertThat(new POJONode(null).asLong(10)).isEqualTo(10L);
        assertThat(new POJONode(99.99D).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode(99L).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode(99).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode((short) 99).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode((byte) 99).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode(BigInteger.valueOf(99)).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asLong(10)).isEqualTo(99L);
        assertThat(new POJONode(new Data()).asLong(10)).isEqualTo(10L);
    }

    @Test
    public void testAsLongOpt() {
        assertThat(new POJONode(null).asLongOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asLongOpt()).hasValue(99L);
        assertThat(new POJONode(99L).asLongOpt()).hasValue(99L);
        assertThat(new POJONode(99).asLongOpt()).hasValue(99L);
        assertThat(new POJONode((short) 99).asLongOpt()).hasValue(99L);
        assertThat(new POJONode((byte) 99).asLongOpt()).hasValue(99L);
        assertThat(new POJONode(BigInteger.valueOf(99)).asLongOpt()).hasValue(99L);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asLongOpt()).hasValue(99L);
        assertThat(new POJONode(new Data()).asLongOpt()).isNotPresent();
    }

    @Test
    public void testAsBigInteger() {
        assertThat(new POJONode(null).asBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(new POJONode(99.99D).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(99L).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(99).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode((short) 99).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode((byte) 99).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(BigInteger.valueOf(99)).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asBigInteger()).isEqualTo(BigInteger.valueOf(99));
        assertThatThrownBy(() -> new POJONode(new Data()).asBigInteger())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asBigInteger()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `java.math.BigInteger`: value type not coercible");
    }

    @Test
    public void testAsBigIntegerDefaultValue() {
        assertThat(new POJONode(null).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.TEN);
        assertThat(new POJONode(99.99D).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(99L).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(99).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode((short) 99).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode((byte) 99).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(BigInteger.valueOf(99)).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.valueOf(99));
        assertThat(new POJONode(new Data()).asBigInteger(BigInteger.TEN)).isEqualTo(BigInteger.TEN);
    }

    @Test
    public void testAsBigIntegerOpt() {
        assertThat(new POJONode(null).asBigIntegerOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode(99L).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode(99).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode((short) 99).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode((byte) 99).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode(BigInteger.valueOf(99)).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asBigIntegerOpt()).hasValue(BigInteger.valueOf(99));
        assertThat(new POJONode(new Data()).asBigIntegerOpt()).isNotPresent();
    }

    @Test
    public void testAsFloat() {
        assertThat(new POJONode(null).asFloat()).isEqualTo(0.0f);
        assertThat(new POJONode(99.99D).asFloat()).isEqualTo(99.99f);
        assertThat(new POJONode(99L).asFloat()).isEqualTo(99f);
        assertThat(new POJONode(99).asFloat()).isEqualTo(99f);
        assertThat(new POJONode((short) 99).asFloat()).isEqualTo(99f);
        assertThat(new POJONode((byte) 99).asFloat()).isEqualTo(99f);
        assertThat(new POJONode(BigInteger.valueOf(99)).asFloat()).isEqualTo(99f);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asFloat()).isEqualTo(99.99f);
        assertThatThrownBy(() -> new POJONode(new Data()).asFloat())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asFloat()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `float`: value type not coercible");
    }

    @Test
    public void testAsFloatDefaultValue() {
        assertThat(new POJONode(null).asFloat(10.0f)).isEqualTo(10.0f);
        assertThat(new POJONode(99.99D).asFloat(10.0f)).isEqualTo(99.99f);
        assertThat(new POJONode(99L).asFloat(10.0f)).isEqualTo(99f);
        assertThat(new POJONode(99).asFloat(10.0f)).isEqualTo(99f);
        assertThat(new POJONode((short) 99).asFloat(10.0f)).isEqualTo(99f);
        assertThat(new POJONode((byte) 99).asFloat(10.0f)).isEqualTo(99f);
        assertThat(new POJONode(BigInteger.valueOf(99)).asFloat(10.0f)).isEqualTo(99f);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asFloat(10.0f)).isEqualTo(99.99f);
        assertThat(new POJONode(new Data()).asFloat(10.0f)).isEqualTo(10.0f);
    }

    @Test
    public void testAsFloatOpt() {
        assertThat(new POJONode(null).asFloatOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asFloatOpt()).hasValue(99.99f);
        assertThat(new POJONode(99L).asFloatOpt()).hasValue(99f);
        assertThat(new POJONode(99).asFloatOpt()).hasValue(99f);
        assertThat(new POJONode((short) 99).asFloatOpt()).hasValue(99f);
        assertThat(new POJONode((byte) 99).asFloatOpt()).hasValue(99f);
        assertThat(new POJONode(BigInteger.valueOf(99)).asFloatOpt()).hasValue(99f);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asFloatOpt()).hasValue(99.99f);
        assertThat(new POJONode(new Data()).asFloatOpt()).isNotPresent();
    }

    @Test
    public void testAsDouble() {
        assertThat(new POJONode(null).asDouble()).isEqualTo(0.0D);
        assertThat(new POJONode(99.99D).asDouble()).isEqualTo(99.99D);
        assertThat(new POJONode(99L).asDouble()).isEqualTo(99D);
        assertThat(new POJONode(99).asDouble()).isEqualTo(99D);
        assertThat(new POJONode((short) 99).asDouble()).isEqualTo(99D);
        assertThat(new POJONode((byte) 99).asDouble()).isEqualTo(99D);
        assertThat(new POJONode(BigInteger.valueOf(99)).asDouble()).isEqualTo(99D);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asDouble()).isEqualTo(99.99D);
        assertThatThrownBy(() -> new POJONode(new Data()).asDouble())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asDouble()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `double`: value type not coercible");
    }

    @Test
    public void testAsDoubleDefaultValue() {
        assertThat(new POJONode(null).asDouble(10.42D)).isEqualTo(10.42D);
        assertThat(new POJONode(99.99D).asDouble(10.42D)).isEqualTo(99.99D);
        assertThat(new POJONode(99L).asDouble(10.42D)).isEqualTo(99D);
        assertThat(new POJONode(99).asDouble(10.42D)).isEqualTo(99D);
        assertThat(new POJONode((short) 99).asDouble(10.42D)).isEqualTo(99D);
        assertThat(new POJONode((byte) 99).asDouble(10.42D)).isEqualTo(99D);
        assertThat(new POJONode(BigInteger.valueOf(99)).asDouble(10.42D)).isEqualTo(99D);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asDouble(10.42D)).isEqualTo(99.99D);
        assertThat(new POJONode(new Data()).asDouble(10.42D)).isEqualTo(10.42D);
    }

    @Test
    public void testAsDoubleOpt() {
        assertThat(new POJONode(null).asDoubleOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asDoubleOpt()).hasValue(99.99D);
        assertThat(new POJONode(99L).asDoubleOpt()).hasValue(99D);
        assertThat(new POJONode(99).asDoubleOpt()).hasValue(99D);
        assertThat(new POJONode((short) 99).asDoubleOpt()).hasValue(99D);
        assertThat(new POJONode((byte) 99).asDoubleOpt()).hasValue(99D);
        assertThat(new POJONode(BigInteger.valueOf(99)).asDoubleOpt()).hasValue(99D);
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asDoubleOpt()).hasValue(99.99D);
        assertThat(new POJONode(new Data()).asDoubleOpt()).isNotPresent();
    }

    @Test
    public void testAsDecimal() {
        assertThat(new POJONode(null).asDecimal()).isEqualTo(BigDecimal.ZERO);
        assertThat(new POJONode(99.99D).asDecimal()).isEqualTo(BigDecimal.valueOf(99.99));
        assertThat(new POJONode(99L).asDecimal()).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode(99).asDecimal()).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode((short) 99).asDecimal()).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode((byte) 99).asDecimal()).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode(BigInteger.valueOf(99)).asDecimal()).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asDecimal()).isEqualTo(BigDecimal.valueOf(99.99));
        assertThatThrownBy(() -> new POJONode(new Data()).asDecimal())
                .isInstanceOf(JsonNodeException.class)
                .hasMessage("'POJONode' method `asDecimal()` cannot coerce value"
                        + " {POJO of type `tools.jackson.databind.node.POJONodeTest$Data`} to `java.math.BigDecimal`: value type not coercible");
    }

    @Test
    public void testAsDecimalDefaultValue() {
        assertThat(new POJONode(null).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.TEN);
        assertThat(new POJONode(99.99D).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99.99));
        assertThat(new POJONode(99L).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode(99).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode((short) 99).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode((byte) 99).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode(BigInteger.valueOf(99)).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99));
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.valueOf(99.99));
        assertThat(new POJONode(new Data()).asDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.TEN);
    }

    @Test
    public void testAsDecimalOpt() {
        assertThat(new POJONode(null).asDecimalOpt()).isNotPresent();
        assertThat(new POJONode(99.99D).asDecimalOpt()).hasValue(BigDecimal.valueOf(99.99));
        assertThat(new POJONode(99L).asDecimalOpt()).hasValue(BigDecimal.valueOf(99));
        assertThat(new POJONode(99).asDecimalOpt()).hasValue(BigDecimal.valueOf(99));
        assertThat(new POJONode((short) 99).asDecimalOpt()).hasValue(BigDecimal.valueOf(99));
        assertThat(new POJONode((byte) 99).asDecimalOpt()).hasValue(BigDecimal.valueOf(99));
        assertThat(new POJONode(BigInteger.valueOf(99)).asDecimalOpt()).hasValue(BigDecimal.valueOf(99));
        assertThat(new POJONode(BigDecimal.valueOf(99.99)).asDecimalOpt()).hasValue(BigDecimal.valueOf(99.99));
        assertThat(new POJONode(new Data()).asDecimalOpt()).isNotPresent();
    }

}
