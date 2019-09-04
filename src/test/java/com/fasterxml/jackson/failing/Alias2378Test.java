package com.fasterxml.jackson.failing;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// for [databind#2378]
public class Alias2378Test extends BaseMapTest
{
    @JsonTypeName("user")
    static abstract class UserEventContext {

         public abstract String getId();

        public abstract String getPartitionId();

        public abstract String getAccountId();

        public abstract AtomicReference<String> getDeviceId();

        @JsonCreator
        public static UserEventContext create(@JsonProperty("partitionId") String partitionId,
                @JsonProperty("accountId") String accountId,
                @JsonProperty("id") @JsonAlias("userId") String userId,
                @JsonProperty("deviceId") String deviceId) {
            return new AutoValue_UserEventContext(userId, partitionId, accountId,
                    new AtomicReference<>(deviceId));
        }
    }

    static class AutoValue_UserEventContext extends UserEventContext {

        private final String id;

        private final String partitionId;

        private final String accountId;

        private final AtomicReference<String> deviceId;

        AutoValue_UserEventContext(
            String id,
            String partitionId,
            String accountId,
            AtomicReference<String> deviceId) {
          if (id == null) {
            throw new NullPointerException("Null id");
          }
          this.id = id;
          if (partitionId == null) {
            throw new NullPointerException("Null partitionId");
          }
          this.partitionId = partitionId;
          if (accountId == null) {
            throw new NullPointerException("Null accountId");
          }
          this.accountId = accountId;
          if (deviceId == null) {
            throw new NullPointerException("Null deviceId");
          }
          this.deviceId = deviceId;
        }

        @Override
        public String getId() {
          return id;
        }

        @Override
        public String getPartitionId() {
          return partitionId;
        }

        @Override
        public String getAccountId() {
          return accountId;
        }

        @Override
        public AtomicReference<String> getDeviceId() {
          return deviceId;
        }
      }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testIssue2378() throws Exception
    {
        UserEventContext value = MAPPER.readValue(
                aposToQuotes(
//"{'userId' : 'abc', 'partitionId' : '123', 'accountId' : '1', 'deviceId':'x' }"
"{'id' : 'abc', 'partitionId' : '123', 'accountId' : '1', 'deviceId':'x' }"
                ), UserEventContext.class);
        assertNotNull(value);
    }
}
