package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Generic1128Test extends BaseMapTest
{
    @SuppressWarnings("rawtypes")
    static abstract class HObj<M extends HObj> {
        public long id;

        // important: do not serialize as subtype, but only as type that
        // is statically recognizable here.
        @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
        public M parent;
    }

    static class DevBase extends HObj<DevBase> {
        public String tag;

        // for some reason, setter is needed to expose this...
        public void setTag(String t) { tag = t; }

        //public String getTag() { return tag; }
    }

    static class Dev extends DevBase {
        public long p1;

        public void setP1(long l) { p1 = l; }
        public long getP1() { return p1; }
    }

    static class DevM extends Dev {
        private long m1;

        public long getM1() { return m1; }
//        public void setM1(int m) { m1 = m; }
    }

    static abstract class ContainerBase<T> {
        public T entity;
    }

    static class DevMContainer extends ContainerBase<DevM>{ }

    public void testIssue1128() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        final DevMContainer devMContainer1 = new DevMContainer();
        final DevM entity = new DevM();
        final Dev parent = new Dev();
        parent.id = 2L;
        entity.parent = parent;
        devMContainer1.entity = entity;

        String json = mapper.writeValueAsString(devMContainer1);

//        System.out.println("serializedContainer = " + json);
        final DevMContainer devMContainer = mapper.readValue(json, DevMContainer.class);
        long id = devMContainer.entity.parent.id;
//        System.out.println("devMContainer.getEntity().getParent().getId() = " + id);
        assertEquals(2, id);
    }
}
