package tools.jackson.databind.views;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5969]: @JsonView bypass for setterless creator properties.
 *
 * Regression introduced when setterless collection properties were enrolled
 * in the merging buffer path of {@code BeanDeserializer._deserializeUsingPropertyBased()}:
 * the regular-property buffering branch had no {@code prop.visibleInView(activeView)} check,
 * so a setterless collection annotated {@code @JsonView(Admin.class)} could still be populated
 * when deserializing under a non-Admin view via a class with a {@code @JsonCreator} constructor.
 */
public class SetterlessViewBypass5969Test extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    public static class CreatorBean {
        @JsonView(PublicView.class)
        private String name;

        // Setterless collection: no setter -> SetterlessProperty.
        // @JsonView(AdminView) means it must be invisible in PublicView.
        @JsonView(AdminView.class)
        private final List<String> roles = new ArrayList<>();

        @JsonCreator
        public CreatorBean(@JsonProperty("name") @JsonView(PublicView.class) String name) {
            this.name = name;
        }

        @JsonView(PublicView.class)
        public String getName() { return name; }

        @JsonView(AdminView.class)
        public List<String> getRoles() { return roles; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Regular case: Admin view should populate both name and roles.
    @Test
    public void testAdminView_negativeControl() throws Exception {
        CreatorBean result = MAPPER
                .readerWithView(AdminView.class)
                .forType(CreatorBean.class)
                .readValue("{\"name\":\"alice\",\"roles\":[\"admin\"]}");
        assertEquals("alice", result.getName());
        assertEquals(List.of("admin"), result.getRoles());
    }

    // Formerly failing case: roles appears BEFORE the creator property "name", forcing the
    // regular-property buffering path. Under PublicView, roles must remain empty.
    @Test
    public void testSetterlessViewBypassInCreatorDeser() throws Exception {
        CreatorBean result = MAPPER
                .readerWithView(PublicView.class)
                .forType(CreatorBean.class)
                .readValue("{\"roles\":[\"admin\"],\"name\":\"alice\"}");

        assertEquals("alice", result.getName());
        assertTrue(result.getRoles().isEmpty(),
                "[databind#5969] setterless property @JsonView(AdminView) was " +
                "populated in PublicView via isMerging() buffer path. roles = " + result.getRoles());
    }

    // Formerly failing case: roles appears AFTER the creator property.
    @Test
    public void testSetterlessViewBypass_rolesAfterCreator() throws Exception {
        CreatorBean result = MAPPER
                .readerWithView(PublicView.class)
                .forType(CreatorBean.class)
                .readValue("{\"name\":\"alice\",\"roles\":[\"admin\"]}");

        assertEquals("alice", result.getName());
        assertTrue(result.getRoles().isEmpty(),
                "[databind#5969] roles populated in PublicView. roles = " + result.getRoles());
    }
}
