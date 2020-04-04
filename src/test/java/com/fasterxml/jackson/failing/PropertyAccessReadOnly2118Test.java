package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class PropertyAccessReadOnly2118Test extends BaseMapTest
{
    // [databind#2118]
    static class SecurityGroup {

        private List<SecurityGroupRule> securityGroupRules;

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
    }

    // [databind#2118]
    public void testAccessReadOnly() throws Exception {
        String data ="{\"security_group_rules\": [{\"id\": \"id1\"}, {\"id\": \"id2\"}, {\"id\": \"id3\"}, {\"id\": \"id4\"}]}";
        ObjectMapper mapper = new ObjectMapper();
// This would work around the issue:        
//        mapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
        SecurityGroup sg = mapper.readValue(data, SecurityGroup.class);
        System.out.println(mapper.writeValueAsString(sg));
    }
}
