package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Generic1133Test extends BaseMapTest
{
    @SuppressWarnings("rawtypes")
    public static abstract class HObj<M extends HObj> {
        public long id;
        public M parent;
    }

    static class DevBase extends HObj<DevBase> {
        public String tag;
    }

    static class Dev extends DevBase {
        public long p1;
    }

    public static class DevM extends Dev {
        public long m1;
    }

    public static abstract class ContainerBase<T> {
        public T entity;
    }

    static class DevMContainer extends ContainerBase<DevM>{ }

    public void testIssue1128() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
//        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    
        final DevMContainer devMContainer1 = new DevMContainer();
        final DevM entity = new DevM();
        final Dev parent = new Dev();
        parent.id = 2L;
        entity.parent = parent;
        devMContainer1.entity = entity;
    
        String json = mapper.writeValueAsString(devMContainer1);
        System.out.println("serializedContainer = " + json);
        final DevMContainer devMContainer = mapper.readValue(json, DevMContainer.class);
        System.out.println("devMContainer.getEntity().getParent().getId() = " + devMContainer.entity.parent.id);
    }
}
