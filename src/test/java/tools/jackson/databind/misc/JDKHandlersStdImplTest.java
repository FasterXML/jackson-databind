package tools.jackson.databind.misc;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.ClassUtil;

import static org.junit.jupiter.api.Assertions.fail;

// [databind#6109]: built-in JDK handlers must be recognized as standard implementations,
// so that optimization modules (like Blackbird) do not revert optimized properties to
// reflection when one of these handlers is resolved.
//
// Note: referenced by name since many of these are package-private and/or nested classes,
// spread over two packages.
public class JDKHandlersStdImplTest
    extends DatabindTestUtil
{
    private final static String[] HANDLERS = new String[] {
            // Serializers, tools.jackson.databind.ser.jdk
            "tools.jackson.databind.ser.jdk.AtomicReferenceSerializer",
            "tools.jackson.databind.ser.jdk.BooleanSerializer$AsNumber",
            "tools.jackson.databind.ser.jdk.ByteBufferSerializer",
            "tools.jackson.databind.ser.jdk.CollectionSerializer",
            "tools.jackson.databind.ser.jdk.EnumSetSerializer",
            "tools.jackson.databind.ser.jdk.InetAddressSerializer",
            "tools.jackson.databind.ser.jdk.InetSocketAddressSerializer",
            "tools.jackson.databind.ser.jdk.JDKKeySerializers$Default",
            "tools.jackson.databind.ser.jdk.JDKKeySerializers$Dynamic",
            "tools.jackson.databind.ser.jdk.JDKKeySerializers$EnumKeySerializer",
            "tools.jackson.databind.ser.jdk.JDKKeySerializers$StringKeySerializer",
            "tools.jackson.databind.ser.jdk.JDKMiscSerializers$AtomicBooleanSerializer",
            "tools.jackson.databind.ser.jdk.JDKMiscSerializers$AtomicIntegerSerializer",
            "tools.jackson.databind.ser.jdk.JDKMiscSerializers$AtomicLongSerializer",
            "tools.jackson.databind.ser.jdk.JDKMiscSerializers$ByteArrayOutputStreamSerializer",
            "tools.jackson.databind.ser.jdk.MapEntryAsPOJOSerializer",
            "tools.jackson.databind.ser.jdk.NumberSerializer$BigDecimalAsStringSerializer",
            "tools.jackson.databind.ser.jdk.TimeZoneSerializer",
            "tools.jackson.databind.ser.jdk.UUIDSerializer",

            // Deserializers, tools.jackson.databind.deser.jdk
            "tools.jackson.databind.deser.jdk.ArrayBlockingQueueDeserializer",
            "tools.jackson.databind.deser.jdk.AtomicBooleanDeserializer",
            "tools.jackson.databind.deser.jdk.AtomicIntegerDeserializer",
            "tools.jackson.databind.deser.jdk.AtomicLongDeserializer",
            "tools.jackson.databind.deser.jdk.AtomicReferenceDeserializer",
            "tools.jackson.databind.deser.jdk.ByteBufferDeserializer",
            "tools.jackson.databind.deser.jdk.EnumMapDeserializer",
            "tools.jackson.databind.deser.jdk.EnumSetDeserializer",
            "tools.jackson.databind.deser.jdk.FactoryBasedEnumDeserializer",
            "tools.jackson.databind.deser.jdk.JDKFromStringDeserializer",
            "tools.jackson.databind.deser.jdk.JDKFromStringDeserializer$StringBufferDeserializer",
            "tools.jackson.databind.deser.jdk.JDKFromStringDeserializer$StringBuilderDeserializer",
            "tools.jackson.databind.deser.jdk.JDKKeyDeserializer$DelegatingKD",
            "tools.jackson.databind.deser.jdk.JDKKeyDeserializer$StringCtorKeyDeserializer",
            "tools.jackson.databind.deser.jdk.JDKKeyDeserializer$StringFactoryKeyDeserializer",
            "tools.jackson.databind.deser.jdk.MapEntryDeserializer$POJOWrappedDeserializer",
            "tools.jackson.databind.deser.jdk.StackTraceElementDeserializer",
            "tools.jackson.databind.deser.jdk.ThreadGroupDeserializer",
            "tools.jackson.databind.deser.jdk.ThrowableDeserializer",
            "tools.jackson.databind.deser.jdk.UUIDDeserializer",
    };

    @Test
    public void jdkHandlersMarkedAsStdImpl() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String name : HANDLERS) {
            // fails the test (via ClassNotFoundException) if a handler gets renamed/removed
            Class<?> cls = Class.forName(name);
            if (!ClassUtil.isJacksonStdImpl(cls)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            fail("Missing @JacksonStdImpl on "+missing.size()+" JDK handler(s): "+missing);
        }
    }
}
