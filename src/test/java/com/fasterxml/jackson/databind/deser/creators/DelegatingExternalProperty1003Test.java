package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DelegatingExternalProperty1003Test extends DatabindTestUtil
{
    // [databind#1003]
    public interface Hero1003 { }

    static class HeroBattle1003 {

        private final Hero1003 hero;

        HeroBattle1003(Hero1003 hero) {
            if (hero == null) throw new Error();
            this.hero = hero;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "heroType")
        public Hero1003 getHero() {
            return hero;
        }

        @JsonCreator
        static HeroBattle1003 fromJson(Delegate1003 json) {
            return new HeroBattle1003(json.hero);
        }
    }

    static class Delegate1003 {
        @JsonProperty
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "heroType")
        public Hero1003 hero;
    }

    static class Superman1003 implements Hero1003 {
        String name = "superman";

        public String getName() {
            return name;
        }
    }

    // [databind#1003]
    @Test
    public void testExtrnalPropertyDelegatingCreator() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

        final String json = mapper.writeValueAsString(new HeroBattle1003(new Superman1003()));
        final HeroBattle1003 battle = mapper.readValue(json, HeroBattle1003.class);

        assertTrue(battle.getHero() instanceof Superman1003);
    }
}
