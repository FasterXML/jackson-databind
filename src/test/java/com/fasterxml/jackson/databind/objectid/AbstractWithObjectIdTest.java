package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

import java.util.*;

public class AbstractWithObjectIdTest extends BaseMapTest
{
    interface BaseInterface { }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class BaseInterfaceImpl implements BaseInterface {

        @JsonProperty
        private List<BaseInterfaceImpl> myInstances = new ArrayList<BaseInterfaceImpl>();

        void addInstance(BaseInterfaceImpl instance) {
            myInstances.add(instance);
        }
    }

    static class ListWrapper<T extends BaseInterface> {

        @JsonProperty
        private List<T> myList = new ArrayList<T>();

        void add(T t) {
            myList.add(t);
        }

        int size() {
            return myList.size();
        }
    }

    public void testIssue877() throws Exception
    {
        // make two instances
        BaseInterfaceImpl one = new BaseInterfaceImpl();
        BaseInterfaceImpl two = new BaseInterfaceImpl();

        // add them to each other's list to show identify info being used
        one.addInstance(two);
        two.addInstance(one);

        // make a typed version of the list and add the 2 instances to it
        ListWrapper<BaseInterfaceImpl> myList = new ListWrapper<BaseInterfaceImpl>();
        myList.add(one);
        myList.add(two);

        // make an object mapper that will add class info in so deserialisation works
        ObjectMapper om = JsonMapper.builder()
                .activateDefaultTypingAsProperty(NoCheckSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL, "@class")
                .build();

        // write and print the JSON
        String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(myList);
        ListWrapper<BaseInterfaceImpl> result;

        result = om.readValue(json, new TypeReference<ListWrapper<BaseInterfaceImpl>>() { });

        assertNotNull(result);
        // see what we get back
        assertEquals(2, result.size());
    }
}
