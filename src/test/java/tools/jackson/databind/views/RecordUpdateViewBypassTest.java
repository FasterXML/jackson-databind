package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5971] (record-update path): @JsonView bypass for creator properties
 * handled by {@code BeanDeserializer._deserializeRecordForUpdate()} (new in 3.1).
 *
 * The record-for-update path pre-populates the creator buffer from the existing
 * record and then overrides from JSON input; the creator-property branch did not
 * check {@code visibleInView}, so a {@code @JsonView(Admin)} component could be
 * overwritten from untrusted JSON while updating under a less-privileged view.
 */
public class RecordUpdateViewBypassTest extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    public record User(
            @JsonView(PublicView.class) String name,
            @JsonView(AdminView.class) String role) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Negative control: AdminView allows overriding the restricted component.
    @Test
    public void testAdminView_negativeControl() throws Exception {
        User existing = new User("alice", "user");
        User result = MAPPER.readerWithView(AdminView.class)
                .forType(User.class)
                .withValueToUpdate(existing)
                .readValue("{\"name\":\"alice2\",\"role\":\"admin\"}");
        assertEquals("alice2", result.name());
        assertEquals("admin", result.role());
    }

    // Formerly failing: under PublicView the @JsonView(AdminView) "role" component
    // must retain its pre-populated value, not be overwritten from JSON.
    @Test
    public void testCreatorPropertyHonorsViewOnRecordUpdate() throws Exception {
        User existing = new User("alice", "user");
        User result = MAPPER.readerWithView(PublicView.class)
                .forType(User.class)
                .withValueToUpdate(existing)
                .readValue("{\"name\":\"alice2\",\"role\":\"HACKED\"}");

        assertEquals("alice2", result.name());
        assertEquals("user", result.role(),
                "[databind#5971] @JsonView(AdminView) record component was overwritten in PublicView " +
                "via the record-update path. role = " + result.role());
    }
}
