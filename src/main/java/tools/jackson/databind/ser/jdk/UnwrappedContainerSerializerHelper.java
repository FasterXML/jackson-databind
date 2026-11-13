package tools.jackson.databind.ser.jdk;

import java.util.function.BooleanSupplier;

import tools.jackson.core.JsonGenerator;

/**
 * Shared handling for a physical singleton whose array wrapper would be omitted.
 */
final class UnwrappedContainerSerializerHelper
{
    private UnwrappedContainerSerializerHelper() { }

    /**
     * A suppressed singleton still needs to write a JSON value. Check lazily so
     * disabled content filtering does not inspect the element or its serializer.
     */
    static boolean writeEmptyArrayIfSuppressed(Object value, JsonGenerator g,
            boolean filtering, BooleanSupplier isEmpty)
    {
        if (filtering && isEmpty.getAsBoolean()) {
            g.writeStartArray(value, 0);
            g.writeEndArray();
            return true;
        }
        return false;
    }
}
