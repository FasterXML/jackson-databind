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
 * Commit e40aaee8 (Fix #2692) changed BeanDeserializer._deserializeUsingPropertyBased()
 * from checking {@code prop instanceof MergingSettableBeanProperty} to {@code prop.isMerging()},
 * and made SetterlessProperty.isMerging() return true. This allows collection-typed setterless
 * properties to be populated via the merging buffer path.
 *
 * Security regression: the {@code isMerging()} branch in the regular-property buffering section
 * has no {@code prop.visibleInView(activeView)} check. For a class with an @JsonCreator
 * constructor and a setterless collection property annotated @JsonView(Admin.class),
 * an attacker using a Public view can still populate the Admin-only collection.
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

    // Negative control: Admin view should populate both name and roles.
    @Test
    public void testAdminView_negativeControl() throws Exception {
        CreatorBean result = MAPPER
                .readerWithView(AdminView.class)
                .forType(CreatorBean.class)
                .readValue("{\"name\":\"alice\",\"roles\":[\"admin\"]}");
        assertEquals("alice", result.getName());
        assertEquals(List.of("admin"), result.getRoles());
    }

    // Exploit path: roles appears BEFORE the creator property "name", forcing the
    // regular-property buffering path. Under PublicView, roles must remain empty.
    @Test
    public void testSetterlessViewBypassInCreatorDeser() throws Exception {
        CreatorBean result = MAPPER
                .readerWithView(PublicView.class)
                .forType(CreatorBean.class)
                .readValue("{\"roles\":[\"admin\"],\"name\":\"alice\"}");

        assertEquals("alice", result.getName());
        assertTrue(result.getRoles().isEmpty(),
                "[databind#5969] VULNERABLE: setterless property @JsonView(AdminView) was " +
                "populated in PublicView via isMerging() buffer path. roles = " + result.getRoles());
    }

    // Exploit path: roles appears AFTER the creator property.
    @Test
    public void testSetterlessViewBypass_rolesAfterCreator() throws Exception {
        CreatorBean result = MAPPER
                .readerWithView(PublicView.class)
                .forType(CreatorBean.class)
                .readValue("{\"name\":\"alice\",\"roles\":[\"admin\"]}");

        assertEquals("alice", result.getName());
        assertTrue(result.getRoles().isEmpty(),
                "[databind#5969] VULNERABLE: roles populated in PublicView. roles = " + result.getRoles());
    }
}
