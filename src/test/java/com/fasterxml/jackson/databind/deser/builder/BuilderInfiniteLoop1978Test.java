package com.fasterxml.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BuilderInfiniteLoop1978Test
{
    static class Builder
    {
         private SubBean temp;
         private Integer id;

         Builder(@JsonProperty("beanId") Integer beanId) {
              this.id = beanId;
         }

         @JsonUnwrapped(prefix="sub.")
         public Builder withThing(SubBean thing) {
             this.temp = thing;
             return this;
         }

         public Bean build()
         {
             Bean bean = new Bean(id);
             bean.setThing( temp );
             return bean;
         }
    }

    @JsonDeserialize(builder = Builder.class)
    static class Bean
    {
        Integer id;
        SubBean thing;

        public Bean(Integer id) {
            this.id = id;
        }

        public SubBean getThing() {
            return thing;
        }

        public void setThing( SubBean thing ) {
            this.thing = thing;
        }
    }

    static class SubBuilder
    {
         private int element1;
         private String element2;

         @JsonProperty("el1")
         public SubBuilder withElement1(int e1) {
              this.element1 = e1;
              return this;
         }

         public SubBean build()
         {
              SubBean bean = new SubBean();
              bean.element1 = element1;
              bean.element2 = element2;
              return bean;
         }
    }

    @JsonDeserialize(builder = SubBuilder.class)
    static class SubBean
    {
        public int element1;
        public String element2;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // for [databind#1978]
    @Test
    public void testInfiniteLoop1978() throws Exception
    {
        String json = "{\"sub.el1\":34,\"sub.el2\":\"some text\"}";
        ObjectMapper mapper = new ObjectMapper();
        Bean bean = mapper.readValue( json, Bean.class );
        assertNotNull(bean);
    }
}
