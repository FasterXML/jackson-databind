package tools.jackson.databind.deser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for deserializing read-only List properties.
 */
public class ReadOnlyListDeserTest
{
    /*
    /**********************************************************
    /* Helper types, [databind#2118]
    /**********************************************************
     */

    // [databind#2118]
    static class SecurityGroup {
        List<SecurityGroupRule> securityGroupRules;

        public SecurityGroup() {
            this.securityGroupRules = new ArrayList<>();
        }

        @JsonProperty(value="security_group_rules", access=JsonProperty.Access.READ_ONLY)
        public List<SecurityGroupRule> getSecurityGroupRules() {
            return securityGroupRules;
        }

        public SecurityGroup setSecurityGroupRules(List<SecurityGroupRule> securityGroupRules) {
            throw new Error("Should not be called");
        }
    }

    static class SecurityGroupRule {
        private String id;

        public SecurityGroupRule() { }

        @JsonProperty
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "{SecurityGroupRule '"+id+"'}";
        }
    }

    /*
    /**********************************************************
    /* Helper types, [databind#2283]
    /**********************************************************
     */

    static class RenamedToSameOnGetter {
        @JsonProperty(value = "list", access = JsonProperty.Access.READ_ONLY)
        List<Long> getList() {
            return Collections.emptyList();
        }
    }

    static class RenamedToDifferentOnGetter {
        @JsonProperty(value = "renamedList", access = JsonProperty.Access.READ_ONLY)
        List<Long> getList() {
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(value={ "renamedList" }, allowGetters=true)
    static class RenamedOnClass {
        @JsonProperty("renamedList")
        List<Long> getList() {
            return Collections.emptyList();
        }
    }

    /*
    /**********************************************************
    /* Test methods, [databind#2118]
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2118]
    @Test
    public void testAccessReadOnly2118() throws Exception {
        String data ="{\"security_group_rules\": [{\"id\": \"id1\"}]}";
// This would work around the issue:
//        mapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
        SecurityGroup sg = MAPPER.readValue(data, SecurityGroup.class);
        assertEquals(Collections.emptyList(), sg.securityGroupRules);
    }

    /*
    /**********************************************************
    /* Test methods, [databind#2283]
    /**********************************************************
     */

    private final ObjectMapper GETTER_AS_SETTER_MAPPER = jsonMapperBuilder()
            .configure(MapperFeature.USE_GETTERS_AS_SETTERS, true).build();

    @Test
    public void testRenamedToSameOnGetter2283() throws Exception
    {
        assertEquals("{\"list\":[]}",
                GETTER_AS_SETTER_MAPPER.writeValueAsString(new RenamedToSameOnGetter()));
        String payload = "{\"list\":[1,2,3,4]}";
        RenamedToSameOnGetter foo = GETTER_AS_SETTER_MAPPER.readValue(payload, RenamedToSameOnGetter.class);
        assertTrue(foo.getList().isEmpty(), "List should be empty");
    }

    @Test
    public void testRenamedToDifferentOnGetter2283() throws Exception
    {
        assertEquals("{\"renamedList\":[]}",
                GETTER_AS_SETTER_MAPPER.writeValueAsString(new RenamedToDifferentOnGetter()));
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedToDifferentOnGetter foo = GETTER_AS_SETTER_MAPPER.readValue(payload, RenamedToDifferentOnGetter.class);
        assertTrue(foo.getList().isEmpty(), "List should be empty");
    }

    @Test
    public void testRenamedOnClass2283() throws Exception
    {
        assertEquals("{\"renamedList\":[]}",
                GETTER_AS_SETTER_MAPPER.writeValueAsString(new RenamedOnClass()));
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedOnClass foo = GETTER_AS_SETTER_MAPPER.readValue(payload, RenamedOnClass.class);
        assertTrue(foo.getList().isEmpty(), "List should be empty");
    }
}
