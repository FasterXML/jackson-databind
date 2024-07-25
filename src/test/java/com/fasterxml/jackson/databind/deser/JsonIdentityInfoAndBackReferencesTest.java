package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.cases.Animals3964;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3964] MismatchedInputException, Bean not yet resolved
class JsonIdentityInfoAndBackReferencesTest extends DatabindTestUtil {

    final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /**
     * Passes : Testing lean without getters and setters
     * and also without {@link JsonCreator}.
     */
    @Test
    void leanWithoutGetterAndSettersAndCreator() throws Exception {
        String json = a2q("{" +
                "              'id': 1," +
                "              'squids': [" +
                "                {" +
                "                  'id': 2," +
                "                  'fish': 1," + // back reference
                "                  'shrimps': [" +
                "                    {" +
                "                      'id': 3," +
                "                      'squid': 2" +
                "                    }" +
                "                  ]" +
                "                }" +
                "              ]" +
                "            }");

        Animals3964.Fish fish = MAPPER.readValue(json, Animals3964.Fish.class);
        assertEquals(fish, fish.squids.get(0).fish);
    }
}
