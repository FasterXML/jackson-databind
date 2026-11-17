package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6205]
/**
 * Tests that forward references within Object arrays are resolved into the slot they
 * were read from, whatever order they get resolved in, and whatever else the array holds.
 */
public class ObjectArrayForwardReference6205Test extends DatabindTestUtil
{
    static final class Container {
        public Node first;

        @JsonIdentityReference(alwaysAsId = true)
        public Node[] refs;

        public Node[] defs;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Node {
        public int id;
        public String name;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Definitions in the same order as the references
    @Test
    public void forwardOrderResolution() throws Exception {
        _verify(_document(1000, false), 1000, false);
    }

    // ... and in the reverse order, so that resolution runs back to front
    @Test
    public void reverseOrderResolution() throws Exception {
        _verify(_document(1000, true), 1000, true);
    }

    // Already-resolved entries sit in the same array as pending ones: placement of
    // the pending ones must not be disturbed by them
    @Test
    public void referencesMixedWithInlineValues() throws Exception {
        // "0" is defined before "refs" so references to it resolve inline, but
        // 1 and 2 are only defined after it: those references, at slots 1 and 3,
        // are the only pending ones
        String json = a2q("{'first':{'id':0,'name':'zero'},"
                +"'refs':[0,1,0,2],"
                +"'defs':[{'id':1,'name':'one'},{'id':2,'name':'two'}]}");
        Container c = MAPPER.readValue(json, Container.class);

        assertEquals(4, c.refs.length);
        Node[] expected = new Node[] { c.first, c.defs[0], c.first, c.defs[1] };
        for (int i = 0; i < expected.length; ++i) {
            assertNotNull(c.refs[i], "Null entry at #"+i);
            assertSame(expected[i], c.refs[i], "Not same instance at #"+i);
        }
    }

    private String _document(int count, boolean reverse) {
        StringBuilder sb = new StringBuilder("{'refs':[");
        for (int i = 0; i < count; ++i) {
            sb.append(i).append((i == count-1) ? "" : ",");
        }
        sb.append("],'defs':[");
        for (int i = 0; i < count; ++i) {
            int id = reverse ? (count - 1 - i) : i;
            sb.append("{'id':").append(id).append(",'name':'n").append(id).append("'}");
            sb.append((i == count-1) ? "" : ",");
        }
        return a2q(sb.append("]}").toString());
    }

    private void _verify(String json, int count, boolean reverse) throws Exception {
        Container c = MAPPER.readValue(json, Container.class);

        assertEquals(count, c.refs.length);
        for (int i = 0; i < count; ++i) {
            assertNotNull(c.refs[i], "Null entry at #"+i);
            assertEquals(i, c.refs[i].id, "Wrong entry at #"+i);
            int defIndex = reverse ? (count - 1 - i) : i;
            assertSame(c.defs[defIndex], c.refs[i], "Not same instance at #"+i);
        }
    }
}
