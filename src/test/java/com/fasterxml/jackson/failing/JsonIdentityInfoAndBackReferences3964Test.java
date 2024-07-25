package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.deser.cases.Animals3964;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.cases.Tree3964;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// [databind#3964] MismatchedInputException, Bean not yet resolved
class JsonIdentityInfoAndBackReferences3964Test extends DatabindTestUtil {

    final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /**
     * Fails : Original test
     */
    @Test
    void original() throws Exception {
        String json = "{" +
                "              \"id\": 1,\n" +
                "              \"fruits\": [\n" +
                "                {\n" +
                "                  \"id\": 2,\n" +
                "                  \"tree\": 1,\n" +
                "                  \"calories\": [\n" +
                "                    {\n" +
                "                      \"id\": 3,\n" +
                "                      \"fruit\": 2\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              ]\n" +
                "            }";

        try {
            Tree3964 tree = MAPPER.readValue(json, Tree3964.class);
            // should reach here and pass... but throws Exception and fails
            assertEquals(tree, tree.fruits.get(0).getTree());
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot resolve ObjectId forward reference using property 'animal'");
            verifyException(e, "Bean not yet resolved");
            fail("Should not reach");
        }
    }

    /**
     * Fails : Lean version that fails and Without getters and setters
     */
    @Test
    void leanWithoutGetterAndSetters() throws Exception {
        String json = a2q("{" +
                "              'id': 1," +
                "              'cats': [" +
                "                {" +
                "                  'id': 2," +
                "                  'animal': 1," + // reference here
                "                  'foods': [" +
                "                    {" +
                "                      'id': 3," +
                "                      'cat': 2" +
                "                    }" +
                "                  ]" +
                "                }" +
                "              ]" +
                "            }");

        try {
            Animals3964.Animal animal = MAPPER.readValue(json, Animals3964.Animal.class);
            // should reach here and pass... but throws Exception and fails
            assertEquals(animal, animal.cats.get(0).animal);
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot resolve ObjectId forward reference using property 'animal'");
            verifyException(e, "Bean not yet resolved");
            fail("Should not reach");
        }
    }
}
