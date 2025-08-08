package tools.jackson.databind.ser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BasicSerializerFactory;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Set of serializers for JDK core types.
 *
 * @since 3.0
 */
public class JDKCoreSerializers
{
    /**
     * Since these are all JDK classes, we shouldn't have to worry
     * about ClassLoader used to load them. Rather, we can just
     * use the class name, and keep things simple and efficient.
     */
    protected final static HashMap<String, ValueSerializer<?>> _concrete;

    static {
        HashMap<String, ValueSerializer<?>> concrete = new HashMap<>();

        // String and string-like types (note: date types explicitly
        // not included -- can use either textual or numeric serialization)
        concrete.put(String.class.getName(), StringSerializer.instance);
        final ToStringSerializer sls = ToStringSerializer.instance;
        concrete.put(StringBuffer.class.getName(), sls);
        concrete.put(StringBuilder.class.getName(), sls);
        concrete.put(Character.class.getName(), sls);
        concrete.put(Character.TYPE.getName(), sls);

        // Primitives/wrappers for primitives (primitives needed for Beans)
        NumberSerializers.addAll(concrete);
        concrete.put(Boolean.TYPE.getName(), new BooleanSerializer(true));
        concrete.put(Boolean.class.getName(), new BooleanSerializer(false));

        // Other numbers, more complicated
        concrete.put(BigInteger.class.getName(), new NumberSerializer(BigInteger.class));
        concrete.put(BigDecimal.class.getName(), new NumberSerializer(BigDecimal.class));

        _concrete = concrete;
    }

    /**
     * Method called by {@link BasicSerializerFactory} to find one of serializers provided here.
     */
    public static final ValueSerializer<?> find(Class<?> raw)
    {
        return _concrete.get(raw.getName());
    }
}
