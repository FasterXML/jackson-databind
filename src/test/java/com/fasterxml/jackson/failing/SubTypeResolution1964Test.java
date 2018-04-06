package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.*;

@SuppressWarnings("serial")
public class SubTypeResolution1964Test extends BaseMapTest
{
    static class AccessModel {
        private Map<Object, Collection<String>> repositoryPrivileges;
        
        public AccessModel() {
            repositoryPrivileges = new HashMap<>();
        }
        
        public Map<Object, Collection<String>> getRepositoryPrivileges() {
            return repositoryPrivileges;
        }
        
        public void setRepositoryPrivileges(Map<Object, Collection<String>> repositoryPrivileges) {
            this.repositoryPrivileges = repositoryPrivileges;
        }
    }

    static class CustomMap<T> extends LinkedHashMap<Object, T> { }

    public void testTypeCompatibility1964() throws Exception
    {
        Map<Object, Collection<String>> repoPrivilegesMap = new CustomMap<>();
        String key = "/storages/storage0/releases";
        Collection<String> values = new HashSet<>();
        values.add("ARTIFACTS_RESOLVE");
        repoPrivilegesMap.put(key, values);
        
        AccessModel accessModel = new AccessModel();
        accessModel.setRepositoryPrivileges(repoPrivilegesMap);

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(accessModel);
        assertNotNull(jsonStr);
    }
}
