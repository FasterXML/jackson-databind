package com.fasterxml.jackson.databind.deser.filter;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

// Test(s) to verify [databind#1440]
public class ProblemHandlerLocation1440Test extends BaseMapTest
{
    static class DeserializationProblem {
        public List<String> unknownProperties = new ArrayList<>();

        public DeserializationProblem() { }

        public void addUnknownProperty(final String prop) {
            unknownProperties.add(prop);
        }
        public boolean foundProblems() {
            return !unknownProperties.isEmpty();
        }

        @Override
        public String toString() {
            return "DeserializationProblem{" +"unknownProperties=" + unknownProperties +'}';
        }
    }

    static class DeserializationProblemLogger extends DeserializationProblemHandler {

        public DeserializationProblem probs = new DeserializationProblem();

        public List<String> problems() {
            return probs.unknownProperties;
        }

        @Override
        public boolean handleUnknownProperty(final DeserializationContext ctxt, final JsonParser p,
                JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName)
                        throws IOException
        {
            final JsonStreamContext parsingContext = p.getParsingContext();
            final List<String> pathList = new ArrayList<>();
            addParent(parsingContext, pathList);
            Collections.reverse(pathList);
            final String path = _join(".", pathList) + "#" + propertyName;

            probs.addUnknownProperty(path);

            p.skipChildren();
            return true;
        }

        static String _join(String sep, Collection<String> parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(part);
            }
            return sb.toString();
        }

        private void addParent(final JsonStreamContext streamContext, final List<String> pathList) {
            if (streamContext != null && streamContext.getCurrentName() != null) {
                pathList.add(streamContext.getCurrentName());
                addParent(streamContext.getParent(), pathList);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Activity {
        public ActivityEntity actor;
        public String verb;
        public ActivityEntity object;
        public ActivityEntity target;

        @JsonCreator
        public Activity(@JsonProperty("actor") final ActivityEntity actor, @JsonProperty("object") final ActivityEntity object, @JsonProperty("target") final ActivityEntity target, @JsonProperty("verb") final String verb) {
            this.actor = actor;
            this.verb = verb;
            this.object = object;
            this.target = target;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ActivityEntity {
        public String id;
        public String type;
        public String status;
        public String context;

        @JsonCreator
        public ActivityEntity(@JsonProperty("id") final String id, @JsonProperty("type") final String type, @JsonProperty("status") final String status, @JsonProperty("context") final String context) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.context = context;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testIncorrectContext() throws Exception
    {
        // need invalid to trigger problem:
        final String invalidInput = a2q(
"{'actor': {'id': 'actor_id','type': 'actor_type',"
+"'status': 'actor_status','context':'actor_context','invalid_1': 'actor_invalid_1'},"
+"'verb': 'verb','object': {'id': 'object_id','type': 'object_type',"
+"'invalid_2': 'object_invalid_2','status': 'object_status','context': 'object_context'},"
+"'target': {'id': 'target_id','type': 'target_type','invalid_3': 'target_invalid_3',"
+"'invalid_4': 'target_invalid_4','status': 'target_status','context': 'target_context'}}"
);

        ObjectMapper mapper = newJsonMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final DeserializationProblemLogger logger = new DeserializationProblemLogger();
        mapper.addHandler(logger);
        mapper.readValue(invalidInput, Activity.class);

        List<String> probs = logger.problems();
        assertEquals(4, probs.size());
        assertEquals("actor.invalid_1#invalid_1", probs.get(0));
        assertEquals("object.invalid_2#invalid_2", probs.get(1));
        assertEquals("target.invalid_3#invalid_3", probs.get(2));
        assertEquals("target.invalid_4#invalid_4", probs.get(3));
    }
}
