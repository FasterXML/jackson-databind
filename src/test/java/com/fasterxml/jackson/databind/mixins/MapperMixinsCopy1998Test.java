package com.fasterxml.jackson.databind.mixins;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

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

    // [databind#1998]: leakage of state via ObjectMapper.copy() (et al)
    public void testSharedBuilder() throws Exception
    {
        final MapperBuilder<?,?> B = defaultMapper();
        MyModelRoot myModelInstance = new MyModelRoot();
        myModelInstance.setChild(new MyChildB("testB"));

        ObjectMapper mapper = B.build();

System.err.println("FIRST/shared");

        String postResult = mapper.writeValueAsString(myModelInstance);
        assertEquals(FULLMODEL, postResult);

System.err.println("SECOND/shared");

        mapper = B
                .addMixIn(MyModelRoot.class, MixinConfig.MyModelRoot.class)
                .addMixIn(MyModelChildBase.class, MixinConfig.MyModelChildBase.class)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        String result = mapper
                .writerWithView(MyModelView.class)
                .writeValueAsString(myModelInstance);
System.err.println("Shared, result: "+result);
        assertEquals(EXPECTED, result);
    }

    // [databind#1998]: leakage of state via ObjectMapper.copy() (et al)
    public void testSharingViaRebuild() throws Exception
    {
        final MapperBuilder<?,?> B = defaultMapper();
        MyModelRoot myModelInstance = new MyModelRoot();
        myModelInstance.setChild(new MyChildB("testB"));

        ObjectMapper mapper = B.build();

System.err.println("FIRST/Rebuild");

        String postResult = mapper.writeValueAsString(myModelInstance);
        assertEquals(FULLMODEL, postResult);

System.err.println("SECOND/Rebuild");

        mapper = mapper.rebuild()
                .addMixIn(MyModelRoot.class, MixinConfig.MyModelRoot.class)
                .addMixIn(MyModelChildBase.class, MixinConfig.MyModelChildBase.class)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        String result = mapper
                .writerWithView(MyModelView.class)
                .writeValueAsString(myModelInstance);
System.err.println("Rebuild, esult: "+result);
        assertEquals(EXPECTED, result);
    }

    private MapperBuilder<?,?> defaultMapper()
    {
        return ObjectMapper.builder().changeDefaultPropertyInclusion(incl ->
            incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
        ;
    }
}
