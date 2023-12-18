package tools.jackson.databind.type;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

public class RecursiveType1658Test
{
    @SuppressWarnings("serial")
    static class Tree<T> extends HashMap<T, Tree<T>> // implements Serializable
    {
        public Tree() { }

        public Tree(List<T> children) {
            this();
            for (final T t : children) {
                this.put(t, new Tree<T>());
            }
        }

        public List<Tree<T>> getLeafTrees() {
            return null;
        }
    }

    @Test
    public void testRecursive1658() throws Exception
    {
        Tree<String> t = new Tree<String>(Arrays.asList("hello", "world"));
        final TypeResolverBuilder<?> typer = new StdTypeResolverBuilder(JsonTypeInfo.Id.CLASS,
                JsonTypeInfo.As.PROPERTY, null);
        ObjectMapper mapper = jsonMapperBuilder()
                .setDefaultTyping(typer)
                .build();
        String res = mapper.writeValueAsString(t);
        Tree<?> tRead = mapper.readValue(res, Tree.class);
        assertNotNull(tRead);

        // 30-Oct-2019, tatu: Let's actually verify that description will be safe to use, too
        JavaType resolved = mapper.getTypeFactory()
                .constructType(new TypeReference<Tree<String>> () { });
        final String namePath = Tree.class.getName().replace('.', '/');
        assertEquals("L"+namePath+";", resolved.getErasedSignature());
        assertEquals("L"+namePath+"<Ljava/lang/String;L"+namePath+";>;",
                resolved.getGenericSignature());
    }
}
