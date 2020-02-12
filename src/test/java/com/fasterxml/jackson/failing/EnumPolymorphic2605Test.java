package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;

public class EnumPolymorphic2605Test extends BaseMapTest
{
    // for [databind#2605]
    static class EnumContaintingClass <ENUM_TYPE extends Enum<ENUM_TYPE>> {
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@class"
        )
         private ENUM_TYPE selected;

        protected EnumContaintingClass() { }

        public EnumContaintingClass(ENUM_TYPE selected) {
          this.selected = selected;
        }

        public ENUM_TYPE getSelected() {
          return selected;
        }

        public void setSelected(ENUM_TYPE selected) {
          this.selected = selected;
        }
    }

    static enum TestEnum { FIRST, SECOND, THIRD; }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    // for [databind#2605]
    public void testRoundtrip() throws Exception
    {
      EnumContaintingClass<TestEnum> gui = new EnumContaintingClass<TestEnum>(TestEnum.SECOND);
      String str = MAPPER.writeValueAsString(gui);
      Object o = MAPPER.readerFor(EnumContaintingClass.class).readValue(str);
      assertNotNull(o);
    }
}
