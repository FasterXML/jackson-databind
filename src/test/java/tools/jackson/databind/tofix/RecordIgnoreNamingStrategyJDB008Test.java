package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [JDB-008]: Record `@JsonIgnore` bypass with naming strategy.
// `POJOPropertiesCollector._removeUnwantedIgnorals()` records the implicit
// component name in `_ignoredPropertyNames` BEFORE `_renameUsing()` applies the
// configured naming strategy, so the renamed JSON key (e.g. "internal_role")
// is not recognized as ignored and gets assigned to the constructor parameter.
class RecordIgnoreNamingStrategyJDB008Test extends DatabindTestUtil
{
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SensitiveRecord(
            String username,
            @JsonIgnore String internalRole
    ) {}

    private final ObjectMapper MAPPER = newJsonMapper();

    // Negative control: nothing tries to set the ignored component.
    @Test
    public void testNormalDeserialization() throws Exception {
        SensitiveRecord result = MAPPER.readValue(
                a2q("{'username':'alice'}"), SensitiveRecord.class);
        assertEquals("alice", result.username());
        assertNull(result.internalRole());
    }

    // Exploit path: the renamed snake_case key for the @JsonIgnore component
    // bypasses the ignore check and the value is bound to `internalRole`.
    @JacksonTestFailureExpected
    @Test
    public void testRenamedIgnoredRecordComponentBypass() throws Exception {
        SensitiveRecord result = MAPPER.readValue(
                a2q("{'username':'alice','internal_role':'ADMIN'}"),
                SensitiveRecord.class);

        assertEquals("alice", result.username());
        assertNotEquals("ADMIN", result.internalRole(),
                "JDB-008: @JsonIgnore on Record component bypassed after naming "
                        + "strategy rename; internalRole = " + result.internalRole());
    }
}
