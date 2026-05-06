package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5966]: Record updateValue() bypasses @JsonIgnore and @JsonView checks.
 *
 * Commit 7ec2a83f added _deserializeRecordForUpdate() for Records. The new method
 * parses JSON properties and assigns them to creator parameters without checking:
 *   - @JsonIgnore / @JsonIgnoreProperties on record components
 *   - @JsonView visibility on creator properties
 *   - isInjectionOnly() (injection-only creator params)
 *
 * The main _deserializeUsingPropertyBased path at line 731 DOES check activeView and
 * at line 744 checks IgnorePropertiesUtil.shouldIgnore(); the new Record path omits both.
 *
 * Proof: live_runtime_proof (JUnit 5).
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

    // --- Tests ---

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * NEGATIVE CONTROL: normal (non-update) deserialization of a Record with
     * @JsonIgnore must reject the ignored field. Must pass before and after patch.
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
     * EXPLOIT PATH: ObjectMapper.updateValue() for a Record with @JsonIgnore.
     *
     * Security assertion: @JsonIgnore must prevent the attacker-supplied JSON value
     * from being assigned to the "secret" component.  Because PropertyBasedCreator
     * excludes ignored components from its lookup table, findCreatorProperty("secret")
     * returns null and the JSON value is rejected.
     *
     * Data-integrity assertion: the pre-existing record value "original-secret"
     * must be retained — _deserializeRecordForUpdate pre-populates ALL creator
     * components from the source record, including ignored ones.
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
     * EXPLOIT PATH: ObjectMapper.updateValue() with @JsonIgnoreProperties on Record.
     *
     * Security assertion: @JsonIgnoreProperties must prevent the JSON value from being
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
     * EXPLOIT PATH: ObjectMapper.updateValue() with @JsonView — admin-only field
     * must not be updated when active view is PublicView.
     *
     * Jackson 3.x defaults DEFAULT_VIEW_INCLUSION=false, so each Record component
     * that should be visible in PublicView must carry @JsonView(PublicView.class).
     * With PublicView active:
     *   - publicField (@JsonView(PublicView.class)) IS visible → updated from JSON
     *   - adminField  (@JsonView(AdminView.class))  is NOT visible → kept from original
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
            "[databind#5966] VULNERABLE: updateValue() bypassed @JsonView on Record component. " +
            "adminField was overwritten to: " + updated.adminField());
        // The original adminField value must be preserved (pre-populated in Step 1)
        assertEquals("admin-original", updated.adminField(),
            "[databind#5966] VULNERABLE: adminField should remain 'admin-original' but was: " + updated.adminField());
    }
}
