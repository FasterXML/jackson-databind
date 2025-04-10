package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// [databind#3596] possibility to not require JsonTypeInfo annotation for sealed classes?
public class SealedPolymorphic3596Test
    extends DatabindTestUtil
{

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
    static sealed class SealedSuperWithAnno3596
            permits SealedSubWithAnno3596A, SealedSubWithAnno3596B {
    }

    static sealed class SealedSuperWithoutAnno3596
            permits SealedSubWithoutAnno3596A, SealedSubWithoutAnno3596B {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Same as [databind#4601], but just so we have something to compare with
    @Test
    public void testSealedClassWithAnnotation()
            throws Exception
    {
        String jsonStr = a2q(String.format("{'@type':'%s'}", SealedSubWithAnno3596A.class.getSimpleName()));

        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new SealedSubWithAnno3596A()));

        // deser
        SealedSuperWithAnno3596 bean = MAPPER.readValue(jsonStr, SealedSuperWithAnno3596.class);
        assertInstanceOf(SealedSuperWithAnno3596.class, bean);
        assertInstanceOf(SealedSubWithAnno3596A.class, bean);
    }

    // Without annotation
    @JacksonTestFailureExpected
    @Test
    public void testSealedClassWithoutAnnotation()
            throws Exception
    {
        String jsonStr = a2q(String.format("{'@type':'%s'}", SealedSubWithoutAnno3596A.class.getSimpleName()));

        // ser
        assertEquals(jsonStr, MAPPER.writeValueAsString(new SealedSubWithoutAnno3596A()));

        // deser
        SealedSuperWithoutAnno3596 bean = MAPPER.readValue(jsonStr, SealedSuperWithoutAnno3596.class);
        assertInstanceOf(SealedSuperWithoutAnno3596.class, bean);
        assertInstanceOf(SealedSubWithoutAnno3596A.class, bean);
    }

}

final class SealedSubWithAnno3596A
        extends SealedPolymorphic3596Test.SealedSuperWithAnno3596 {
}

final class SealedSubWithAnno3596B
        extends SealedPolymorphic3596Test.SealedSuperWithAnno3596 {
}

final class SealedSubWithoutAnno3596A
        extends SealedPolymorphic3596Test.SealedSuperWithoutAnno3596 {
}

final class SealedSubWithoutAnno3596B
        extends SealedPolymorphic3596Test.SealedSuperWithoutAnno3596 {
}
