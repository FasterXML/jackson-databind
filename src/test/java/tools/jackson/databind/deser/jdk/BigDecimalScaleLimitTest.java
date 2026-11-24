package tools.jackson.databind.deser.jdk;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#6488]
public class BigDecimalScaleLimitTest extends DatabindTestUtil
{
    private final static String HUGE_SCALE_VALUE = "1e2000000000";
    private final static String IN_LIMIT_SCALE_VALUE = "1e100000";

    static class BigDecimalCreatorWrapper {
        final BigDecimal value;

        @JsonCreator
        BigDecimalCreatorWrapper(@JsonProperty("value") BigDecimal value) {
            this.value = value;
        }
    }

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper LIMITED_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_BIG_DECIMAL_SCALE_OVERFLOW)
            .build();

    private final ObjectMapper LIMITED_UNTYPED_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_BIG_DECIMAL_SCALE_OVERFLOW)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    @Test
    public void testLimitDisabledByDefault() throws Exception
    {
        assertEquals(-2_000_000_000,
                DEFAULT_MAPPER.readValue(HUGE_SCALE_VALUE, BigDecimal.class).scale());
    }

    @Test
    public void testLimitForTypedBigDecimal() throws Exception
    {
        _verifyLimited(() -> LIMITED_MAPPER.readValue(HUGE_SCALE_VALUE, BigDecimal.class));
        _verifyLimited(() -> LIMITED_MAPPER.readValue('"' + HUGE_SCALE_VALUE + '"',
                BigDecimal.class));
    }

    @Test
    public void testLimitForUntypedBigDecimal() throws Exception
    {
        _verifyLimited(() -> LIMITED_UNTYPED_MAPPER.readValue(HUGE_SCALE_VALUE, Number.class));
        _verifyLimited(() -> LIMITED_UNTYPED_MAPPER.readValue(HUGE_SCALE_VALUE, Object.class));
        _verifyLimited(() -> LIMITED_UNTYPED_MAPPER.readTree(HUGE_SCALE_VALUE));
        _verifyLimited(() -> LIMITED_MAPPER.treeToValue(
                JsonNodeFactory.instance.numberNode(new BigDecimal(HUGE_SCALE_VALUE)),
                Number.class));
    }

    @Test
    public void testLimitForBigDecimalMapKey() throws Exception
    {
        _verifyLimited(() -> LIMITED_MAPPER.readValue(
                "{\"" + HUGE_SCALE_VALUE + "\":\"value\"}",
                new TypeReference<Map<BigDecimal, String>>() { }));
    }

    @Test
    public void testLimitForBigDecimalCreator() throws Exception
    {
        _verifyLimited(() -> LIMITED_MAPPER.readValue(
                "{\"value\":" + HUGE_SCALE_VALUE + "}",
                BigDecimalCreatorWrapper.class));
    }

    @Test
    public void testValueAtScaleLimitAccepted() throws Exception
    {
        assertEquals(-100_000,
                LIMITED_MAPPER.readValue(IN_LIMIT_SCALE_VALUE, BigDecimal.class).scale());
    }

    private interface ReadOperation {
        Object read() throws Exception;
    }

    private void _verifyLimited(ReadOperation operation) throws Exception
    {
        try {
            operation.read();
            fail("Should not accept a BigDecimal with excessive scale");
        } catch (StreamConstraintsException e) {
            verifyException(e, "BigDecimal scale", "magnitude exceeds the maximum allowed");
        }
    }
}
