package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#5974]: Record `@JsonIgnore` bypass with naming strategy.
// `POJOPropertiesCollector._removeUnwantedIgnorals()` records the implicit
// component name in `_ignoredPropertyNames` before `_renameUsing()` applies the
// configured naming strategy; without the rename-aware ignore propagation the
// renamed JSON key (e.g. "internal_role") slipped past the ignore check and was
// bound to the constructor parameter.
class RecordIgnoreNamingStrategy5974Test extends DatabindTestUtil
{
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SensitiveRecord(
            String username,
            @JsonIgnore String internalRole
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SensitivePojo {
        public String username;
        @JsonIgnore
        public String internalRole;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testNormalDeserialization() throws Exception {
        SensitiveRecord result = MAPPER.readValue(
                a2q("{'username':'alice'}"), SensitiveRecord.class);
        assertEquals("alice", result.username());
        assertNull(result.internalRole());
    }

    @Test
    public void testRenamedIgnoredRecordComponentBypass() throws Exception {
        SensitiveRecord result = MAPPER.readValue(
                a2q("{'username':'alice','internal_role':'ADMIN'}"),
                SensitiveRecord.class);

        assertEquals("alice", result.username());
        assertNull(result.internalRole());
    }

    // POJO analog of the Record case. Non-Record path drops the @JsonIgnore
    // property before _renameUsing() runs so this never had the same exploit,
    // but the assertion locks in the broader contract that an ignored field
    // cannot be populated via the naming-strategy-renamed JSON key.
    @Test
    public void testRenamedIgnoredPojoFieldBypass() throws Exception {
        SensitivePojo result = MAPPER.readValue(
                a2q("{'username':'alice','internal_role':'ADMIN'}"),
                SensitivePojo.class);

        assertEquals("alice", result.username);
        assertNull(result.internalRole);
    }
}
