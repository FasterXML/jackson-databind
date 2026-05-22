package com.fasterxml.jackson.databind.views;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5969] / [databind#5971]: `@JsonView` must be honored for properties
// buffered during property-based-creator collection, for builder-based POJOs
// (and the regular bean path, sibling of #5969).
public class BuilderViewBypass5969And5971Test extends DatabindTestUtil
{
    static class PublicV { }
    static class AdminV extends PublicV { }

    // Builder-based POJO with a 2-arg property-based creator so the creator is
    // not complete after the first property.
    @JsonDeserialize(builder = User.Builder.class)
    static class User {
        @JsonView(PublicV.class) public final String name;
        @JsonView(PublicV.class) public final String city;
        @JsonView(AdminV.class)  public final String password;

        private User(String n, String c, String p) { name = n; city = c; password = p; }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String name, city, password;

            @JsonCreator
            public Builder(@JsonProperty("name") String n, @JsonProperty("city") String c) {
                name = n; city = c;
            }
            @JsonView(PublicV.class) public Builder name(String n)     { name = n; return this; }
            @JsonView(PublicV.class) public Builder city(String c)     { city = c; return this; }
            @JsonView(AdminV.class)  public Builder password(String p) { password = p; return this; }
            public User build() { return new User(name, city, password); }
        }
    }

    // Regular (non-builder) POJO with a 2-arg property-based creator and a
    // view-restricted setter -- sibling case (#5969).
    static class Bean {
        public String name, city;
        protected String password;

        @JsonCreator
        public Bean(@JsonProperty("name") String n, @JsonProperty("city") String c) {
            name = n; city = c;
        }
        @JsonView(AdminV.class)
        public void setPassword(String p) { password = p; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void builderViewHonoredBetweenCreatorProps() throws Exception {
        // password placed BETWEEN the two creator props -> hits the buffering branch
        String json = a2q("{'name':'alice','password':'BYPASS','city':'NY'}");
        User u = MAPPER.readerFor(User.class).withView(PublicV.class).readValue(json);
        assertEquals("alice", u.name);
        assertEquals("NY", u.city);
        assertNull(u.password, "password should be hidden by active view");
    }

    @Test
    public void builderViewHonoredAfterCreatorProps() throws Exception {
        String json = a2q("{'name':'alice','city':'NY','password':'BYPASS'}");
        User u = MAPPER.readerFor(User.class).withView(PublicV.class).readValue(json);
        assertNull(u.password, "password should be hidden by active view");
    }

    @Test
    public void builderAdminViewStillWrites() throws Exception {
        String json = a2q("{'name':'alice','password':'OK','city':'NY'}");
        User u = MAPPER.readerFor(User.class).withView(AdminV.class).readValue(json);
        assertEquals("OK", u.password, "password should be writable under AdminV");
    }

    @Test
    public void beanViewHonoredBetweenCreatorProps() throws Exception {
        String json = a2q("{'name':'alice','password':'BYPASS','city':'NY'}");
        Bean b = MAPPER.readerFor(Bean.class).withView(PublicV.class).readValue(json);
        assertEquals("alice", b.name);
        assertNull(b.password, "password should be hidden by active view");
    }

    @Test
    public void beanAdminViewStillWrites() throws Exception {
        String json = a2q("{'name':'alice','password':'OK','city':'NY'}");
        Bean b = MAPPER.readerFor(Bean.class).withView(AdminV.class).readValue(json);
        assertEquals("OK", b.password, "password should be writable under AdminV");
    }
}
