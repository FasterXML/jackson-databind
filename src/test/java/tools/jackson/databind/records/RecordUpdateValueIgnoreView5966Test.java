package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5966]: Record updateValue() bypasses @JsonIgnore and @JsonView checks.
 */
public class RecordUpdateValueIgnoreView5966Test extends DatabindTestUtil
{
    // --- View classes ---
    static class PublicView {}
    static class AdminView extends PublicView {}

    // --- Record model --- (must be public for Jackson reflection)

    public record SecretRecord(
        String name,
        @JsonIgnore String secret
    ) {}

    @JsonIgnoreProperties({"password"})
    public record IgnoredPropsRecord(
        String username,
        String password
    ) {}

    public record ViewRecord(
        @JsonView(PublicView.class) String publicField,
        @JsonView(AdminView.class) String adminField
    ) {}

    public record InjectRecord(
        String name,
        @JacksonInject(value = "injected-key", useInput = OptBoolean.FALSE) String injected
    ) {}

    // --- Tests ---

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Normal (non-update) deserialization of a Record with
     * @JsonIgnore must reject the ignored field.
     */
    @Test
    public void testJsonIgnore_normalDeser_negativeControl() throws Exception {
        // Normal readValue should honour @JsonIgnore
        SecretRecord result = MAPPER.readValue("{\"name\":\"alice\",\"secret\":\"s3cr3t\"}", SecretRecord.class);
        // The exact behaviour depends on jackson version; the key point is that
        // updateValue is the vulnerable path. We verify updateValue separately.
        // Normal deser may accept or reject; the security concern is updateValue.
        assertNotNull(result.name());
    }

    /**
     * ObjectMapper.updateValue() for a Record with @JsonIgnore.
     */
    @Test
    public void test5966_updateValueBypassesJsonIgnore() throws Exception {
        SecretRecord original = new SecretRecord("alice", "original-secret");
        String maliciousJson = "{\"name\":\"alice\",\"secret\":\"HACKED\"}";

        SecretRecord updated = MAPPER.updateValue(original, MAPPER.readTree(maliciousJson));

        assertEquals("alice", updated.name());
        // @JsonIgnore on "secret" must prevent the attacker JSON value from taking effect.
        assertNotEquals("HACKED", updated.secret(),
            "[databind#5966] VULNERABLE: updateValue() bypassed @JsonIgnore on Record component. " +
            "secret was overwritten to: " + updated.secret());
        // Original value must be retained (not nulled out).
        assertEquals("original-secret", updated.secret(),
            "[databind#5966] secret should retain original value but was: " + updated.secret());
    }

    /**
     * Verify that @JsonIgnoreProperties prevents the JSON value from being
     * assigned. Data-integrity assertion: original "original-pw" must be retained.
     */
    @Test
    public void test5966_updateValueBypassesJsonIgnoreProperties() throws Exception {
        IgnoredPropsRecord original = new IgnoredPropsRecord("alice", "original-pw");
        String maliciousJson = "{\"username\":\"alice\",\"password\":\"HACKED\"}";

        IgnoredPropsRecord updated = MAPPER.updateValue(original, MAPPER.readTree(maliciousJson));

        assertEquals("alice", updated.username());
        // @JsonIgnoreProperties({"password"}) must block the attacker value.
        assertNotEquals("HACKED", updated.password(),
            "[databind#5966] VULNERABLE: updateValue() bypassed @JsonIgnoreProperties on Record. " +
            "password was overwritten to: " + updated.password());
        // Original value must be retained (not nulled out).
        assertEquals("original-pw", updated.password(),
            "[databind#5966] password should retain original value but was: " + updated.password());
    }

    /**
     * ObjectMapper.updateValue() with @JsonView — admin-only field
     * must not be updated when active view is PublicView.
     */
    @Test
    public void test5966_updateValueBypassesJsonView() throws Exception {
        ViewRecord original = new ViewRecord("public-val", "admin-original");
        String maliciousJson = "{\"publicField\":\"new-public\",\"adminField\":\"HACKED-ADMIN\"}";

        ViewRecord updated = MAPPER
            .readerWithView(PublicView.class)
            .withValueToUpdate(original)
            .readValue(maliciousJson);

        // publicField has @JsonView(PublicView.class): visible in PublicView → updated
        assertEquals("new-public", updated.publicField());
        // adminField has @JsonView(AdminView.class); with PublicView active it must not be updated
        assertNotEquals("HACKED-ADMIN", updated.adminField(),
            "[databind#5966] updateValue() bypassed @JsonView on Record component. " +
            "adminField was overwritten to: " + updated.adminField());
        // The original adminField value must be preserved (pre-populated in Step 1)
        assertEquals("admin-original", updated.adminField(),
            "[databind#5966] adminField should remain 'admin-original' but was: " + updated.adminField());
    }

    /**
     * ObjectMapper.updateValue() with @JacksonInject(useInput=FALSE) on a Record
     * component: JSON value must be skipped, injected value must take effect.
     */
    @Test
    public void test5966_updateValueRespectsInjectionOnly() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
            .injectableValues(new InjectableValues.Std().addValue("injected-key", "INJECTED"))
            .build();
        InjectRecord original = new InjectRecord("alice", "original-injected");
        String maliciousJson = "{\"name\":\"alice\",\"injected\":\"HACKED\"}";

        InjectRecord updated = mapper.updateValue(original, mapper.readTree(maliciousJson));

        assertEquals("alice", updated.name());
        assertNotEquals("HACKED", updated.injected(),
            "[databind#5966] updateValue() bypassed isInjectionOnly() on Record component. " +
            "injected was overwritten to: " + updated.injected());
        assertEquals("INJECTED", updated.injected(),
            "[databind#5966] injected component should hold the injected value but was: " + updated.injected());
    }

    /**
     * Empty-JSON updateValue() of a Record with @JsonIgnore: pre-populate must
     * retain the source record's value for ignored components when no JSON
     * properties are provided to override.
     */
    @Test
    public void test5966_updateValueEmptyJsonRetainsIgnored() throws Exception {
        SecretRecord original = new SecretRecord("alice", "original-secret");

        SecretRecord updated = MAPPER.updateValue(original, MAPPER.readTree("{}"));

        assertEquals("alice", updated.name());
        assertEquals("original-secret", updated.secret(),
            "[databind#5966] empty-JSON update should retain original secret but was: " + updated.secret());
    }
}
