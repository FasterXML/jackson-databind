package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ImmutablesTypeSerializationTest
{

    @Value.Immutable
    @JsonDeserialize(as = ImmutableAccount.class)
    @JsonSerialize(as = ImmutableAccount.class)
    interface Account {
        Long getId();
        String getName();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableKey.class)
    @JsonSerialize(as = ImmutableKey.class)
    interface Key<T> {
        T getId();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableEntry.class)
    @JsonSerialize(as = ImmutableEntry.class)
    interface Entry<K, V> {
        K getKey();
        V getValue();
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
