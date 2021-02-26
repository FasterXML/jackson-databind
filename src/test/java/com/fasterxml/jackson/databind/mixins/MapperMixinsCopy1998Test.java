package com.fasterxml.jackson.databind.mixins;

import java.io.IOException;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class MapperMixinsCopy1998Test extends BaseMapTest
{
    final static String FULLMODEL="{\"format\":\"1.0\",\"child\":{\"type\":\"CHILD_B\",\"name\":\"testB\"},\"notVisible\":\"should not be present\"}";
    final static String EXPECTED="{\"format\":\"1.0\",\"child\":{\"name\":\"testB\"}}";

    static class MyModelView { }

    interface MixinConfig {
        interface MyModelRoot {
            @JsonView(MyModelView.class)
            public String getFormat();

            @JsonView(MyModelView.class)
            public MyModelChildBase getChild();
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.EXISTING_PROPERTY)
        interface MyModelChildBase {
            @JsonView(MyModelView.class)
            public String getName();
        }

    }

    @JsonPropertyOrder({ "format", "child" })
    static class MyModelRoot {
        @JsonProperty
        private String format = "1.0";

        public String getFormat() {
            return format;
        }
        @JsonProperty
        private MyModelChildBase child;

        public MyModelChildBase getChild() {
            return child;
        }

        public void setChild(MyModelChildBase child) {
            this.child = child;
        }

        @JsonProperty
        private String notVisible = "should not be present";

        public String getNotVisible() {
            return notVisible;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MyChildA.class, name = "CHILD_A"),
            @JsonSubTypes.Type(value = MyChildB.class, name = "CHILD_B")
    })
    abstract static class MyModelChildBase {
        @JsonProperty
        private String name;

        public String getName() {
            return name;
        }

        @JsonIgnore
        public void setName(String name) {
            this.name = name;
        }
    }

    static class MyChildA extends MyModelChildBase {
        public MyChildA(String name) {
            setName(name);
        }
    }

    static class MyChildB extends MyModelChildBase {
        public MyChildB(String name) {
            setName(name);
        }
    }

    @SuppressWarnings("deprecation")
    public void testB_KO() throws Exception
    {
        final ObjectMapper DEFAULT = defaultMapper();
        MyModelRoot myModelInstance = new MyModelRoot();
        myModelInstance.setChild(new MyChildB("testB"));

        ObjectMapper myObjectMapper = DEFAULT.copy();

        String postResult = getString(myModelInstance, myObjectMapper);
        assertEquals(FULLMODEL, postResult);
//        System.out.println("postResult: "+postResult);

        myObjectMapper = DEFAULT.copy();
//        myObjectMapper = defaultMapper();
        myObjectMapper.addMixIn(MyModelRoot.class, MixinConfig.MyModelRoot.class)
                .addMixIn(MyModelChildBase.class, MixinConfig.MyModelChildBase.class)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .setConfig(myObjectMapper.getSerializationConfig().withView(MyModelView.class));

        String result = getString(myModelInstance, myObjectMapper);
        assertEquals(EXPECTED, result);

    }

    private String getString(MyModelRoot myModelInstance, ObjectMapper myObjectMapper) throws IOException {
        return myObjectMapper.writerFor(MyModelRoot.class).writeValueAsString(myModelInstance);
    }

    private ObjectMapper defaultMapper()
    {
        return jsonMapperBuilder()
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY,
                        JsonInclude.Include.NON_EMPTY))
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();
    }
}
