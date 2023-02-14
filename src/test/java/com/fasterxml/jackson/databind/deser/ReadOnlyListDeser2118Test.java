package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class ReadOnlyListDeser2118Test extends BaseMapTest
{
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

    private final ObjectMapper mapper = newJsonMapper();

    // [databind#2118]
    public void testAccessReadOnly() throws Exception {
        String data ="{\"security_group_rules\": [{\"id\": \"id1\"}]}";
// This would work around the issue:
//        mapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
        SecurityGroup sg = mapper.readValue(data, SecurityGroup.class);
        assertEquals(Collections.emptyList(), sg.securityGroupRules);
    }
}
