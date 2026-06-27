package com.fasterxml.jackson.databind.views;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#6060]: `@JsonView` by-passed for `@JsonUnwrapped` Field/Setter properties
public class UnwrappedViewBypass6060Test extends DatabindTestUtil {
    public static class PublicView {}
    public static class AdminView extends PublicView {}

    public static class AccountFlags {       // admin-only block
        public String role;
        public boolean approved;
        public long creditBalance;
    }
    public static class Registration {       // bound at the public registration endpoint
        @JsonView(PublicView.class) public String email;
        @JsonView(PublicView.class) public String password;
        @JsonView(AdminView.class) @JsonUnwrapped public AccountFlags flags;   // admin-only, unwrapped
    }
    public static class Control {            // identical block, NOT unwrapped
        @JsonView(PublicView.class) public String email;
        @JsonView(AdminView.class)  public AccountFlags flags;
    }

    public static class RegistrationWithSetters {   // same as Registration, but via setters
        String email;
        String password;
        AccountFlags flags;

        @JsonView(PublicView.class)
        public void setEmail(String email) { this.email = email; }

        @JsonView(PublicView.class)
        public void setPassword(String password) { this.password = password; }

        @JsonView(AdminView.class) @JsonUnwrapped
        public void setFlags(AccountFlags flags) { this.flags = flags; }
    }

    @Test
    void testUnwrappedBypass() throws Exception {
        ObjectMapper mapper = sharedMapper();

        String jsonC = a2q("{'email':'e','flags':{'role':'ADMIN','approved':true,'creditBalance':1000000}}");
        Control c = mapper.readerWithView(PublicView.class)
                .forType(Control.class)
                .readValue(jsonC);
        assertEquals("e", c.email);
        assertNull(c.flags, "expected control flag to be null in PublicView read");

        String jsonR = a2q("{'email':'mallory@evil.test','password':'pw',"
                + "'role':'ADMIN','approved':true,'creditBalance':1000000}");
        Registration r = mapper.readerWithView(PublicView.class)
                .forType(Registration.class)
                .readValue(jsonR);
        assertEquals("mallory@evil.test", r.email);
        assertEquals("pw", r.password);
        // JsonUnwrapped flag in Registration class may affect this
        assertNull(r.flags, "expected registration flag to be null in PublicView read");
    }

    // Negative control: AdminView read MUST populate the unwrapped admin-only block,
    // so the PublicView assertion above is not passing vacuously.
    @Test
    void testUnwrappedVisibleInAdminView() throws Exception {
        ObjectMapper mapper = sharedMapper();

        String jsonR = a2q("{'email':'admin@corp.test','password':'pw',"
                + "'role':'ADMIN','approved':true,'creditBalance':1000000}");
        Registration r = mapper.readerWithView(AdminView.class)
                .forType(Registration.class)
                .readValue(jsonR);
        assertEquals("admin@corp.test", r.email);
        assertNotNull(r.flags, "expected registration flag to be populated in AdminView read");
        assertEquals("ADMIN", r.flags.role);
        assertEquals(1000000, r.flags.creditBalance);
    }

    @Test
    void testUnwrappedBypassWithSetters() throws Exception {
        ObjectMapper mapper = sharedMapper();

        String jsonR = a2q("{'email':'mallory@evil.test','password':'pw',"
                + "'role':'ADMIN','approved':true,'creditBalance':1000000}");
        RegistrationWithSetters r = mapper.readerWithView(PublicView.class)
                .forType(RegistrationWithSetters.class)
                .readValue(jsonR);
        assertEquals("mallory@evil.test", r.email);
        assertEquals("pw", r.password);
        // JsonUnwrapped flag in RegistrationWithSetters class may affect this
        assertNull(r.flags, "expected registration flag to be null in PublicView read");
    }

    // Negative control for the setter variant: AdminView read MUST populate flags.
    @Test
    void testUnwrappedVisibleInAdminViewWithSetters() throws Exception {
        ObjectMapper mapper = sharedMapper();

        String jsonR = a2q("{'email':'admin@corp.test','password':'pw',"
                + "'role':'ADMIN','approved':true,'creditBalance':1000000}");
        RegistrationWithSetters r = mapper.readerWithView(AdminView.class)
                .forType(RegistrationWithSetters.class)
                .readValue(jsonR);
        assertEquals("admin@corp.test", r.email);
        assertNotNull(r.flags, "expected registration flag to be populated in AdminView read");
        assertEquals("ADMIN", r.flags.role);
        assertEquals(1000000, r.flags.creditBalance);
    }

}
