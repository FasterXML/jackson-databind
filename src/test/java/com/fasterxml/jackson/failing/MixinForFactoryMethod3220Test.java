package com.fasterxml.jackson.failing;

import java.util.Objects;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class MixinForFactoryMethod3220Test
    extends BaseMapTest
{
    // [databind#3220]
    static class Timestamped<T> {
        private final T value;
        private final int timestamp;

        Timestamped(T value, int timestamp) {
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

    abstract static class TimestampedMixin<T> {
        @JsonCreator
        public static <T> void stamp(
            @JsonProperty("value") T value,
            @JsonProperty("timestamp") int timestamp) {
        }

        @JsonGetter("value")
        abstract T getValue();

        @JsonGetter("timestamp")
        abstract int getTimestamp();
    }

    static class Profile {
        private final String firstName;
        private final String lastName;

        @JsonCreator
        public Profile(@JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName
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

    static class User {
        private final Timestamped<Profile> profile;

        @JsonCreator
        User(@JsonProperty("profile") Timestamped<Profile> profile) {
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

    // [databind#3220]
    public void testMixin() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(Timestamped.class, TimestampedMixin.class)
                .build();

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
