package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

// This is probably impossible to handle, in general case, since
// there is a cycle for Parent2/Child2... unless special handling
// could be made to ensure that
public class TestObjectIdWithInjectables639 extends BaseMapTest
{
        public static final class Context { }

        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        public static final class Parent1 {
            public Child1 child;

            public Parent1() { }
        }

        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        public static final class Child1 {

            @JsonProperty
            private final Parent1 parent;

            @JsonCreator
            public Child1(@JsonProperty("parent") Parent1 parent) {
                this.parent = parent;
            }
        }

        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        public static final class Parent2 {

            protected final Context context;

            public Child2 child;

            @JsonCreator
            public Parent2(@JacksonInject Context context) {
                this.context = context;
            }
        }

        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        public static final class Child2 {

            protected final Context context;

            @JsonProperty
            protected Parent2 parent;

            @JsonCreator
            public Child2(@JacksonInject Context context,
                    @JsonProperty("parent") Parent2 parent) {
                this.context = context;
                this.parent = parent;
            }
        }

        public void testObjectIdWithInjectables() throws Exception
        {
            ObjectMapper mapper = new ObjectMapper();
            Context context = new Context();
            InjectableValues iv = new InjectableValues.Std().
                    addValue(Context.class, context);
            mapper.setInjectableValues(iv);

            Parent1 parent1 = new Parent1();
            Child1 child1 = new Child1(parent1);
            parent1.child = child1;

            Parent2 parent2 = new Parent2(context);
            Child2 child2 = new Child2(context, parent2);
            parent2.child = child2;

            String json = mapper.writeValueAsString(parent1);
            parent1 = mapper.readValue(json, Parent1.class);
//            System.out.println("This works: " + json);

            json = mapper.writeValueAsString(parent2);
//System.out.println("This fails: " + json);
            parent2 = mapper.readValue(json, Parent2.class);
        }
}
