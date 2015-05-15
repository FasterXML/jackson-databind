package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestGenericTypes extends BaseMapTest
{
    static class Account {
        private Long id;        
        private String name;
        
        public Account(String name, Long id) {
            this.id = id;
            this.name = name;
        }

        public String getName() { return name; }
        public Long getId() { return id; }
    }

    static class Key<T> {
        private final T id;
        
        public Key(T id) { this.id = id; }
        
        public T getId() { return id; }

        public <V> Key<V> getParent() { return null; }
    }
 
    static class Person1 {
        private Long id;
        private String name;
        private Key<Account> account;
        
        public Person1(String name) { this.name = name; }

        public String getName() {
            return name;
        }

        public Key<Account> getAccount() {
            return account;
        }

        public Long getId() {
            return id;
        }

        public void setAccount(Key<Account> account) {
            this.account = account;
        }    
    }

    static class Person2 {
        private Long id;
        private String name;
        private List<Key<Account>> accounts;
        
        public Person2(String name) {
                this.name = name;
        }

        public String getName() { return name; }
        public List<Key<Account>> getAccounts() { return accounts; }
        public Long getId() { return id; }

        public void setAccounts(List<Key<Account>> accounts) {
            this.accounts = accounts;
        }
    }

    static class GenericBogusWrapper<T> {
        public Element wrapped;

        public GenericBogusWrapper(T v) { wrapped = new Element(v); }

        class Element {
            public T value;
    
            public Element(T v) { value = v; }
        }
    }

    // For [databind#728]
    static class Base727 {
        public int a;
    }
    
    @JsonPropertyOrder(alphabetic=true)
    static class Impl727 extends Base727 {
        public int b;

        public Impl727(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public void testIssue468a() throws Exception
    {
        Person1 p1 = new Person1("John");
        p1.setAccount(new Key<Account>(new Account("something", 42L)));
        
        // First: ensure we can serialize (pre 1.7 this failed)
        String json = MAPPER.writeValueAsString(p1);

        // and then verify that results make sense
        Map<String,Object> map = MAPPER.readValue(json, Map.class);
        assertEquals("John", map.get("name"));
        Object ob = map.get("account");
        assertNotNull(ob);
        Map<String,Object> acct = (Map<String,Object>) ob;
        Object idOb = acct.get("id");
        assertNotNull(idOb);
        Map<String,Object> key = (Map<String,Object>) idOb;
        assertEquals("something", key.get("name"));
        assertEquals(Integer.valueOf(42), key.get("id"));
    }

    @SuppressWarnings("unchecked")
    public void testIssue468b() throws Exception
    {
        Person2 p2 = new Person2("John");
        List<Key<Account>> accounts = new ArrayList<Key<Account>>();
        accounts.add(new Key<Account>(new Account("a", 42L)));
        accounts.add(new Key<Account>(new Account("b", 43L)));
        accounts.add(new Key<Account>(new Account("c", 44L)));
        p2.setAccounts(accounts);

        // serialize without error:
        String json = MAPPER.writeValueAsString(p2);

        // then verify output
        Map<String,Object> map = MAPPER.readValue(json, Map.class);
        assertEquals("John", map.get("name"));
        Object ob = map.get("accounts");
        assertNotNull(ob);
        List<?> acctList = (List<?>) ob;
        assertEquals(3, acctList.size());
        // ... might want to verify more, but for now that should suffice
    }

    /**
     * Test related to unbound type variables, usually resulting
     * from inner classes of generic classes (like Sets).
     */
    public void testUnboundTypes() throws Exception
    {
        GenericBogusWrapper<Integer> list = new GenericBogusWrapper<Integer>(Integer.valueOf(7));
        String json = MAPPER.writeValueAsString(list);
        assertEquals("{\"wrapped\":{\"value\":7}}", json);
    }

    public void testRootTypeForCollections727() throws Exception
    {
        List<Base727> input = new ArrayList<Base727>();
        input.add(new Impl727(1, 2));

        final String EXP = aposToQuotes("[{'a':1,'b':2}]");
        // Without type enforcement, produces expected output:
        assertEquals(EXP, MAPPER.writeValueAsString(input));
        assertEquals(EXP, MAPPER.writer().writeValueAsString(input));

        // but enforcing type will hinder:
        TypeReference<?> typeRef = new TypeReference<List<Base727>>() { };
        assertEquals(EXP, MAPPER.writer().forType(typeRef).writeValueAsString(input));
    }
}
