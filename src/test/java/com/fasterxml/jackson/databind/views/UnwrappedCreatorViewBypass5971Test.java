package com.fasterxml.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5971]: @JsonView bypass for unwrapped creator parameters.
//
// UnwrappedPropertyHandler.processUnwrappedCreatorProperties() iterated over
// _creatorProperties and called prop.deserialize() without checking
// visibleInView(activeView). When an application uses JsonView as a write-side
// access-control boundary, an @JsonView(Admin) constructor parameter annotated
// with @JsonUnwrapped could still be populated from untrusted JSON while reading
// with a different (less-privileged) active view.
public class UnwrappedCreatorViewBypass5971Test extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    public static class Address {
        @JsonView(AdminView.class)
        public String street;

        @JsonView(PublicView.class)
        public String city;
    }

    public static class UserBean {
        @JsonView(PublicView.class)
        public final String name;

        @JsonView(AdminView.class)
        public final Address address;

        @JsonCreator
        public UserBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonView(AdminView.class) @JsonUnwrapped Address address) {
            this.name = name;
            this.address = address;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Negative control: AdminView populates both creator params.
    @Test
    public void testAdminView_negativeControl() throws Exception {
        UserBean result = MAPPER
                .readerWithView(AdminView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"street\":\"1 Main St\",\"city\":\"Springfield\"}");
        assertEquals("alice", result.name);
        assertNotNull(result.address);
        assertEquals("1 Main St", result.address.street);
        assertEquals("Springfield", result.address.city);
    }

    // Formerly failing case: under PublicView, the @JsonView(AdminView)
    // @JsonUnwrapped creator parameter must not be populated.
    @Test
    public void testUnwrappedCreatorParamHonorsJsonView() throws Exception {
        UserBean result = MAPPER
                .readerWithView(PublicView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"street\":\"1 Main St\",\"city\":\"Springfield\"}");

        assertEquals("alice", result.name);
        assertNull(result.address,
                "[databind#5971] @JsonView(AdminView) @JsonUnwrapped creator parameter " +
                "was populated in PublicView. address = " + result.address);
    }
}
