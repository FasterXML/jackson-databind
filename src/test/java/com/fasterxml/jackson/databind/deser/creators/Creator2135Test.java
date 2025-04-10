package com.fasterxml.jackson.databind.deser.creators;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class Creator2135Test extends DatabindTestUtil
{
    static class House {
        @JsonProperty(required = true)
        List<Brick> bricks = new ArrayList<>();

        @JsonProperty(required = true)
        Integer houseNumber;

        @JsonCreator
        public House(@JsonProperty(value = "bricks", required = true) List<Brick> bricks,
                     @JsonProperty(value = "houseNumber", required = true) Integer houseNumber) {
            this.bricks.addAll(bricks);
            this.houseNumber = houseNumber;
        }

        public List<Brick> getBricks() {
            return bricks;
        }

        public Integer getHouseNumber() {
            return houseNumber;
        }
    }

    static class Brick {
        @JsonProperty(required = true)
        final String code;

        boolean creatorCalled;
        
        public Brick(@JsonProperty("code") String code) {
            this.code = code;
            creatorCalled = true;
        }

        @JsonCreator
        static Brick create(String code) {
             return new Brick(code);
        }

    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testBricksFromObject() throws Exception {
        House house = MAPPER.readValue(
                "{ \"bricks\": [{ \"code\": \"aa\" }, { \"code\": \"abcd\" } ], \"houseNumber\": 99 }", House.class);
        assertNotNull(house);
        assertEquals(2, house.getBricks().size());
        assertTrue(house.bricks.get(0).creatorCalled);
        assertTrue(house.bricks.get(1).creatorCalled);
    }

    @Test
    void testBricksFromStrings() throws Exception {
        House house = MAPPER.readValue(
                "{ \"bricks\": [\"aa\", \"abcd\"], \"houseNumber\": 99 }", House.class);
        assertNotNull(house);
        assertEquals(2, house.getBricks().size());
        assertTrue(house.bricks.get(0).creatorCalled);
        assertTrue(house.bricks.get(1).creatorCalled);
    }
}
