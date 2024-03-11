package tools.jackson.databind.mixins;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class MapperMixinsCopy1998Test extends DatabindTestUtil
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

    // [databind#1998]: leakage of state via ObjectMapper.copy() (2.x) and similar (3.x)
    @Test
    public void testSharedBuilder() throws Exception
    {
        final MapperBuilder<?,?> B = defaultMapper();
        MyModelRoot myModelInstance = new MyModelRoot();
        myModelInstance.setChild(new MyChildB("testB"));

        ObjectMapper mapper = B.build();

        String postResult = mapper.writeValueAsString(myModelInstance);
        assertEquals(FULLMODEL, postResult);

        mapper = B
                .addMixIn(MyModelRoot.class, MixinConfig.MyModelRoot.class)
                .addMixIn(MyModelChildBase.class, MixinConfig.MyModelChildBase.class)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        String result = mapper
                .writerWithView(MyModelView.class)
                .writeValueAsString(myModelInstance);
        assertEquals(EXPECTED, result);
    }

    // [databind#1998]: leakage of state via ObjectMapper.copy() (2.x) and similar (3.x)
    public void testSharingViaRebuild() throws Exception
    {
        final MapperBuilder<?,?> B = defaultMapper();
        MyModelRoot myModelInstance = new MyModelRoot();
        myModelInstance.setChild(new MyChildB("testB"));

        ObjectMapper mapper = B.build();

        String postResult = mapper.writeValueAsString(myModelInstance);
        assertEquals(FULLMODEL, postResult);

        mapper = mapper.rebuild()
                .addMixIn(MyModelRoot.class, MixinConfig.MyModelRoot.class)
                .addMixIn(MyModelChildBase.class, MixinConfig.MyModelChildBase.class)
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        String result = mapper
                .writerWithView(MyModelView.class)
                .writeValueAsString(myModelInstance);
        assertEquals(EXPECTED, result);
    }

    private MapperBuilder<?,?> defaultMapper()
    {
        return jsonMapperBuilder().changeDefaultPropertyInclusion(incl ->
            incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
        ;
    }
}
