package tools.jackson.databind.jsontype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.TestSubtypesSubPackage.SuperType.InnerType;
import tools.jackson.databind.jsontype.subpackage.SubCSubPackage;
import tools.jackson.databind.testutil.DatabindTestUtil;

// For [databind#4983]: `JsonTypeInfo.Id.MINIMAL_CLASS` generates invalid type on sub-package
public class TestSubtypesSubPackage extends DatabindTestUtil
{
	// Extended by SubCSubPackage which is in a sub package
    @JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS)
    public static abstract class SuperType {

        public static class InnerType extends SuperType {
        	public int b = 2;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSubPackage() throws Exception
    {
    	// type should be computed consider base=SuperType (as it provides the annotation)
    	SubCSubPackage bean = new SubCSubPackage();
        assertEquals("{\"@c\":\".subpackage.SubCSubPackage\",\"c\":2}", MAPPER.writeValueAsString(bean));
    }

    @Test
    public void testInner() throws Exception
    {
    	// type should be computed consider base=SuperType (as it provides the annotation)
    	InnerType bean = new InnerType();
        assertEquals("{\"@c\":\".TestSubtypesSubPackage$SuperType$InnerType\",\"b\":2}", MAPPER.writeValueAsString(bean));
    }
}
