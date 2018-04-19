package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.*;

/**
 * Test for [databind#1964], wherein slightly incompatible type hierarchy,
 * where `Map` key is downcast from `String` to `Object` (via use of "raw"
 * types to force compiler to ignore incompatibility) causes exception
 * during serialization. Although ideally code would not force round peg
 * through square hole, it makes sense to add specific exception to allow
 * such downcast just for Map key types (for now at least).
 */
@SuppressWarnings("serial")
public class SubTypeResolution1964Test extends BaseMapTest
{
    static class AccessModel {
        private Map<String, Collection<String>> repositoryPrivileges;

        public AccessModel() {
            repositoryPrivileges = new HashMap<>();
        }

        // 19-Apr-2018, tatu; this would prevent issues
//        @JsonSerialize(typing = JsonSerialize.Typing.STATIC)
        public Map<String, Collection<String>> getRepositoryPrivileges() {
            return repositoryPrivileges;
        }

        public void setRepositoryPrivileges(Map<String, Collection<String>> repositoryPrivileges) {
            this.repositoryPrivileges = repositoryPrivileges;
        }
    }

    static class CustomMap<T> extends LinkedHashMap<Object, T> { }

    public void testTypeCompatibility1964() throws Exception
    {
        // Important! Must use raw type since assignment requires effectively
        // casting due incompatible type parameters.
        @SuppressWarnings("unchecked")
        Map<String, Collection<String>> repoPrivilegesMap = new CustomMap();
        String key = "/storages/storage0/releases";
        Collection<String> values = new HashSet<>();
        values.add("ARTIFACTS_RESOLVE");
        repoPrivilegesMap.put(key, values);
        
        AccessModel accessModel = new AccessModel();
        accessModel.setRepositoryPrivileges(repoPrivilegesMap);

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(accessModel);
        // ... could/should verify more, perhaps, but for now let it be.
        assertNotNull(jsonStr);
    }
}
