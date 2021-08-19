package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MixingFactoryMethodTest {
    public static class Timestamped<T> {
        private final T value;
        private final int timestamp;

        private Timestamped(T value, int timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public static <T> Timestamped<T> stamp(T value, int timestamp) {
            return new Timestamped<>(value, timestamp);
        }

        public T getValue() {
            return value;
        }

        public int getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Timestamped<?> that = (Timestamped<?>) o;
            return timestamp == that.timestamp && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, timestamp);
        }
    }

    public abstract static class TimestampedMixin<T> {
        @JsonCreator
        public static <T> void stamp(
            @JsonProperty("value") T value,
            @JsonProperty("timestamp") int timestamp
        ) {
        }

        @JsonGetter("value")
        abstract T getValue();

        @JsonGetter("timestamp")
        abstract int getTimestamp();
    }

    public static class Profile {
        private final String firstName;
        private final String lastName;

        @JsonCreator
        public Profile(
            @JsonProperty("firstName")
                String firstName,
            @JsonProperty("lastName")
                String lastName
        ) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @JsonGetter("firstName")
        public String getFirstName() {
            return firstName;
        }

        @JsonGetter("lastName")
        public String getLastName() {
            return lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Profile profile = (Profile) o;
            return Objects.equals(firstName, profile.firstName) && Objects.equals(lastName, profile.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName);
        }
    }

    public static class User {
        private final Timestamped<Profile> profile;

        @JsonCreator
        private User(
            @JsonProperty("profile")
                Timestamped<Profile> profile
        ) {
            this.profile = profile;
        }

        @JsonGetter("profile")
        public Timestamped<Profile> getProfile() {
            return profile;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(profile, user.profile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profile);
        }
    }

    @Test
    public void testMixin() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Timestamped.class, TimestampedMixin.class);

        Profile profile = new Profile("Jackson", "Databind");
        User user = new User(new Timestamped<>(profile, 1));

        User deserializedUser = mapper.readValue(
            mapper.writerFor(User.class).writeValueAsString(user),
            User.class
        );

        Profile deserializedProfile = deserializedUser.getProfile().getValue();

        assertEquals(profile, deserializedProfile);
        assertEquals(user, deserializedUser);
    }
}
