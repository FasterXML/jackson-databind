package com.fasterxml.jackson.databind.interop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for serialization and deserialization of objects based on
 * <a href="https://immutables.github.io/">immutables</a>.
 *<p>
 * Originally to verify fix for
 * <a href="https://github.com/FasterXML/jackson-databind/pull/2894">databind#2894</a>
 * to guard against regression.
 */
public class ImmutablesTypeSerializationTest
{
    /*
     * Interface Definitions based on the immutables annotation processor: https://immutables.github.io/
     */

    @JsonDeserialize(as = ImmutableAccount.class)
    @JsonSerialize(as = ImmutableAccount.class)
    interface Account {
        Long getId();
        String getName();
    }

    @JsonDeserialize(as = ImmutableKey.class)
    @JsonSerialize(as = ImmutableKey.class)
    interface Key<T> {
        T getId();
    }

    @JsonDeserialize(as = ImmutableEntry.class)
    @JsonSerialize(as = ImmutableEntry.class)
    interface Entry<K, V> {
        K getKey();
        V getValue();
    }

    /*
     * Implementations based on the output of the immutables annotation processor version 2.8.8.
     * See https://immutables.github.io/
     */

    static final class ImmutableAccount
            implements ImmutablesTypeSerializationTest.Account {
        private final Long id;
        private final String name;

        ImmutableAccount(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @JsonProperty("id")
        @Override
        public Long getId() {
            return id;
        }

        @JsonProperty("name")
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object another) {
            if (this == another) return true;
            return another instanceof ImmutableAccount
                    && equalTo((ImmutableAccount) another);
        }

        private boolean equalTo(ImmutableAccount another) {
            return id.equals(another.id)
                    && name.equals(another.name);
        }

        @Override
        public int hashCode() {
            int h = 5381;
            h += (h << 5) + id.hashCode();
            h += (h << 5) + name.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "Account{id=" + id + ", name=" + name + "}";
        }

        @JsonDeserialize
        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
        static final class Json
                implements ImmutablesTypeSerializationTest.Account {
            Long id;
            String name;
            @JsonProperty("id")
            public void setId(Long id) {
                this.id = id;
            }
            @JsonProperty("name")
            public void setName(String name) {
                this.name = name;
            }
            @Override
            public Long getId() { throw new UnsupportedOperationException(); }
            @Override
            public String getName() { throw new UnsupportedOperationException(); }
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ImmutableAccount fromJson(ImmutableAccount.Json json) {
            ImmutableAccount.Builder builder = ImmutableAccount.builder();
            if (json.id != null) {
                builder.id(json.id);
            }
            if (json.name != null) {
                builder.name(json.name);
            }
            return builder.build();
        }

        public static ImmutableAccount.Builder builder() {
            return new ImmutableAccount.Builder();
        }

        public static final class Builder {
            private static final long INIT_BIT_ID = 0x1L;
            private static final long INIT_BIT_NAME = 0x2L;
            private long initBits = 0x3L;

            private Long id;
            private String name;

            Builder() {
            }

            public final ImmutableAccount.Builder from(ImmutablesTypeSerializationTest.Account instance) {
                Objects.requireNonNull(instance, "instance");
                id(instance.getId());
                name(instance.getName());
                return this;
            }

            @JsonProperty("id")
            public final ImmutableAccount.Builder id(Long id) {
                this.id = Objects.requireNonNull(id, "id");
                initBits &= ~INIT_BIT_ID;
                return this;
            }

            @JsonProperty("name")
            public final ImmutableAccount.Builder name(String name) {
                this.name = Objects.requireNonNull(name, "name");
                initBits &= ~INIT_BIT_NAME;
                return this;
            }

            public ImmutableAccount build() {
                if (initBits != 0) {
                    throw new IllegalStateException(formatRequiredAttributesMessage());
                }
                return new ImmutableAccount(id, name);
            }

            private String formatRequiredAttributesMessage() {
                List<String> attributes = new ArrayList<>();
                if ((initBits & INIT_BIT_ID) != 0) attributes.add("id");
                if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
                return "Cannot build Account, some of required attributes are not set " + attributes;
            }
        }
    }

    static final class ImmutableKey<T>
            implements ImmutablesTypeSerializationTest.Key<T> {
        private final T id;

        ImmutableKey(T id) {
            this.id = id;
        }

        @JsonProperty("id")
        @Override
        public T getId() {
            return id;
        }

        @Override
        public boolean equals(Object another) {
            if (this == another) return true;
            return another instanceof ImmutableKey<?>
                    && equalTo((ImmutableKey<?>) another);
        }

        private boolean equalTo(ImmutableKey<?> another) {
            return id.equals(another.id);
        }

        @Override
        public int hashCode() {
            int h = 5381;
            h += (h << 5) + id.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "Key{id=" + id + "}";
        }

        @JsonDeserialize
        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
        static final class Json<T>
                implements ImmutablesTypeSerializationTest.Key<T> {
            T id;
            @JsonProperty("id")
            public void setId(T id) {
                this.id = id;
            }
            @Override
            public T getId() { throw new UnsupportedOperationException(); }
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static <T> ImmutableKey<T> fromJson(ImmutableKey.Json<T> json) {
            ImmutableKey.Builder<T> builder = ImmutableKey.<T>builder();
            if (json.id != null) {
                builder.id(json.id);
            }
            return builder.build();
        }

        public static <T> ImmutableKey.Builder<T> builder() {
            return new ImmutableKey.Builder<>();
        }

        public static final class Builder<T> {
            private static final long INIT_BIT_ID = 0x1L;
            private long initBits = 0x1L;

            private T id;

            Builder() {
            }

            public final ImmutableKey.Builder<T> from(ImmutablesTypeSerializationTest.Key<T> instance) {
                Objects.requireNonNull(instance, "instance");
                id(instance.getId());
                return this;
            }

            @JsonProperty("id")
            public final ImmutableKey.Builder<T> id(T id) {
                this.id = Objects.requireNonNull(id, "id");
                initBits &= ~INIT_BIT_ID;
                return this;
            }

            public ImmutableKey<T> build() {
                if (initBits != 0) {
                    throw new IllegalStateException(formatRequiredAttributesMessage());
                }
                return new ImmutableKey<>(id);
            }

            private String formatRequiredAttributesMessage() {
                List<String> attributes = new ArrayList<>();
                if ((initBits & INIT_BIT_ID) != 0) attributes.add("id");
                return "Cannot build Key, some of required attributes are not set " + attributes;
            }
        }
    }

    static final class ImmutableEntry<K, V>
            implements ImmutablesTypeSerializationTest.Entry<K, V> {
        private final K key;
        private final V value;

        ImmutableEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @JsonProperty("key")
        @Override
        public K getKey() {
            return key;
        }

        @JsonProperty("value")
        @Override
        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object another) {
            if (this == another) return true;
            return another instanceof ImmutableEntry<?, ?>
                    && equalTo((ImmutableEntry<?, ?>) another);
        }

        private boolean equalTo(ImmutableEntry<?, ?> another) {
            return key.equals(another.key)
                    && value.equals(another.value);
        }

        @Override
        public int hashCode() {
            int h = 5381;
            h += (h << 5) + key.hashCode();
            h += (h << 5) + value.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "Entry{key=" + key + ", value=" + value + "}";
        }

        @JsonDeserialize
        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
        static final class Json<K, V>
                implements ImmutablesTypeSerializationTest.Entry<K, V> {
            K key;
            V value;
            @JsonProperty("key")
            public void setKey(K key) {
                this.key = key;
            }
            @JsonProperty("value")
            public void setValue(V value) {
                this.value = value;
            }
            @Override
            public K getKey() { throw new UnsupportedOperationException(); }
            @Override
            public V getValue() { throw new UnsupportedOperationException(); }
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static <K, V> ImmutableEntry<K, V> fromJson(ImmutableEntry.Json<K, V> json) {
            ImmutableEntry.Builder<K, V> builder = ImmutableEntry.<K, V>builder();
            if (json.key != null) {
                builder.key(json.key);
            }
            if (json.value != null) {
                builder.value(json.value);
            }
            return builder.build();
        }

        public static <K, V> ImmutableEntry.Builder<K, V> builder() {
            return new ImmutableEntry.Builder<>();
        }

        public static final class Builder<K, V> {
            private static final long INIT_BIT_KEY = 0x1L;
            private static final long INIT_BIT_VALUE = 0x2L;
            private long initBits = 0x3L;

            private K key;
            private V value;

            Builder() {
            }

            public final ImmutableEntry.Builder<K, V> from(ImmutablesTypeSerializationTest.Entry<K, V> instance) {
                Objects.requireNonNull(instance, "instance");
                key(instance.getKey());
                value(instance.getValue());
                return this;
            }

            @JsonProperty("key")
            public final ImmutableEntry.Builder<K, V> key(K key) {
                this.key = Objects.requireNonNull(key, "key");
                initBits &= ~INIT_BIT_KEY;
                return this;
            }

            @JsonProperty("value")
            public final ImmutableEntry.Builder<K, V> value(V value) {
                this.value = Objects.requireNonNull(value, "value");
                initBits &= ~INIT_BIT_VALUE;
                return this;
            }

            public ImmutableEntry<K, V> build() {
                if (initBits != 0) {
                    throw new IllegalStateException(formatRequiredAttributesMessage());
                }
                return new ImmutableEntry<>(key, value);
            }

            private String formatRequiredAttributesMessage() {
                List<String> attributes = new ArrayList<>();
                if ((initBits & INIT_BIT_KEY) != 0) attributes.add("key");
                if ((initBits & INIT_BIT_VALUE) != 0) attributes.add("value");
                return "Cannot build Entry, some of required attributes are not set " + attributes;
            }
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testImmutablesSimpleDeserialization() throws IOException {
        Account expected = ImmutableAccount.builder()
                .id(1L)
                .name("foo")
                .build();
        Account actual = MAPPER.readValue("{\"id\": 1,\"name\":\"foo\"}", Account.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testImmutablesSimpleRoundTrip() throws IOException {
        Account original = ImmutableAccount.builder()
                .id(1L)
                .name("foo")
                .build();
        String json = MAPPER.writeValueAsString(original);
        Account deserialized = MAPPER.readValue(json, Account.class);
        assertEquals(original, deserialized);
    }

    @Test
    public void testImmutablesSimpleGenericDeserialization() throws IOException {
        Key<Account> expected = ImmutableKey.<Account>builder()
                .id(ImmutableAccount.builder()
                        .id(1L)
                        .name("foo")
                        .build())
                .build();
        Key<Account> actual = MAPPER.readValue(
                "{\"id\":{\"id\": 1,\"name\":\"foo\"}}",
                new TypeReference<Key<Account>>() {});
        assertEquals(expected, actual);
    }

    @Test
    public void testImmutablesSimpleGenericRoundTrip() throws IOException {
        Key<Account> original = ImmutableKey.<Account>builder()
                .id(ImmutableAccount.builder()
                        .id(1L)
                        .name("foo")
                        .build())
                .build();
        String json = MAPPER.writeValueAsString(original);
        Key<Account> deserialized = MAPPER.readValue(json, new TypeReference<Key<Account>>() {});
        assertEquals(original, deserialized);
    }

    @Test
    public void testImmutablesMultipleTypeParametersDeserialization() throws IOException {
        Entry<Key<Account>, Account> expected = ImmutableEntry.<Key<Account>, Account>builder()
                .key(ImmutableKey.<Account>builder()
                        .id(ImmutableAccount.builder()
                                .id(1L)
                                .name("foo")
                                .build())
                        .build())
                .value(ImmutableAccount.builder()
                        .id(2L)
                        .name("bar")
                        .build())
                .build();
        Entry<Key<Account>, Account> actual = MAPPER.readValue(
                "{\"key\":{\"id\":{\"id\": 1,\"name\":\"foo\"}},\"value\":{\"id\":2,\"name\":\"bar\"}}",
                new TypeReference<Entry<Key<Account>, Account>>() {});
        assertEquals(expected, actual);
    }

    @Test
    public void testImmutablesMultipleTypeParametersRoundTrip() throws IOException {
        Entry<Key<Account>, Account> original = ImmutableEntry.<Key<Account>, Account>builder()
                .key(ImmutableKey.<Account>builder()
                        .id(ImmutableAccount.builder()
                                .id(1L)
                                .name("foo")
                                .build())
                        .build())
                .value(ImmutableAccount.builder()
                        .id(2L)
                        .name("bar")
                        .build())
                .build();
        String json = MAPPER.writeValueAsString(original);
        Entry<Key<Account>, Account> deserialized = MAPPER.readValue(
                json, new TypeReference<Entry<Key<Account>, Account>>() {});
        assertEquals(original, deserialized);
    }
}
