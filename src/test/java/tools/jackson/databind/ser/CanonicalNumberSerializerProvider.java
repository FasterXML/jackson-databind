package tools.jackson.databind.ser;

import java.math.BigDecimal;

import tools.jackson.databind.ser.std.StdSerializer;

// TODO I need to implement an interface and ValueSerializer which is an abstract class. This seems to be the only solution.
public interface CanonicalNumberSerializerProvider {
    StdSerializer<BigDecimal> getNumberSerializer();
    ValueToString<BigDecimal> getValueToString();
}
