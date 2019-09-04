package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// for [databind#2378]
public class Alias2378Test extends BaseMapTest
{
    static abstract class UserEventContext {
        public abstract String getId();

        public abstract String getPartitionId();

        @JsonCreator
        public static UserEventContext create(@JsonProperty("partitionId") String partitionId,
                @JsonProperty("id") @JsonAlias("userId") String userId) {
            return new AutoValue_UserEventContext(userId, partitionId);
        }
    }

    static class AutoValue_UserEventContext extends UserEventContext {

        private final String id;

        private final String partitionId;

        AutoValue_UserEventContext(String id, String partitionId) {
          if (id == null) {
              throw new NullPointerException("Null id");
          }
          this.id = id;
          if (partitionId == null) {
              throw new NullPointerException("Null partitionId");
          }
          this.partitionId = partitionId;
        }

        @Override
        public String getId() {
          return id;
        }

        @Override
        public String getPartitionId() {
          return partitionId;
        }
      }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testIssue2378() throws Exception
    {
        UserEventContext value = MAPPER.readValue(
                aposToQuotes("{'userId' : 'abc', 'partitionId' : '123' }"
//                aposToQuotes("{'id' : 'abc', 'partitionId' : '123' }"
                ), UserEventContext.class);
        assertNotNull(value);
    }
}
