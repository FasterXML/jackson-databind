package tools.jackson.databind.ext.xml;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class DOMTypeReadWriteTest extends DatabindTestUtil
{
    final static String SIMPLE_XML =
        "<root attr='3'><leaf>Rock &amp; Roll!</leaf><?proc instr?></root>";
    final static String SIMPLE_XML_NS =
        "<root ns:attr='abc' xmlns:ns='http://foo' />";
    final static String SIMPLE_XML_DEFAULT_NS =
            "<root xmlns='http://foo'/>";

    // [databind#6113]: wrappers for polymorphic handling, with the 2 DOM types
    // for which deserializers exist (see `OptionalHandlerFactory`)
    static class NodeWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Node value;

        protected NodeWrapper() { }
        public NodeWrapper(Node n) { value = n; }
    }

    static class DocumentWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Document value;

        protected DocumentWrapper() { }
        public DocumentWrapper(Document d) { value = d; }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSerializeSimpleNonNS() throws Exception
    {
        // Let's just parse first, easiest
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse
            (new InputSource(new StringReader(SIMPLE_XML)));
        assertNotNull(doc);
        // need to strip xml declaration, if any
        String outputRaw = MAPPER.writeValueAsString(doc);
        // And re-parse as String, since JSON has quotes...
        String output = MAPPER.readValue(outputRaw, String.class);
        /* ... and finally, normalize to (close to) canonical XML
         * output (single vs double quotes, xml declaration etc)
         */
        assertEquals(SIMPLE_XML, normalizeOutput(output));
    }

    @Test
    public void testSerializeSimpleDefaultNS() throws Exception
    {
        // Let's just parse first, easiest
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse
                (new InputSource(new StringReader(SIMPLE_XML_DEFAULT_NS)));
        assertNotNull(doc);
        // need to strip xml declaration, if any
        String outputRaw = MAPPER.writeValueAsString(doc);
        // And re-parse as String, since JSON has quotes...
        String output = MAPPER.readValue(outputRaw, String.class);
        /* ... and finally, normalize to (close to) canonical XML
         * output (single vs double quotes, xml declaration etc)
         */
        assertEquals(SIMPLE_XML_DEFAULT_NS, normalizeOutput(output));
    }

    @Test
    public void testDeserializeNonNS() throws Exception
    {
        for (int i = 0; i < 2; ++i) {
            Document doc;

            if (i == 0) {
                // First, as Document:
                doc = MAPPER.readValue(q(SIMPLE_XML), Document.class);
            } else {
                // and then as plain Node (no difference)
                Node node = MAPPER.readValue(q(SIMPLE_XML), Node.class);
                doc = (Document) node;
            }
            Element root = doc.getDocumentElement();
            assertNotNull(root);
            // non-ns, simple...
            assertEquals("root", root.getTagName());
            assertEquals("3", root.getAttribute("attr"));
            assertEquals(1, root.getAttributes().getLength());
            NodeList nodes = root.getChildNodes();
            assertEquals(2, nodes.getLength());
            Element leaf = (Element) nodes.item(0);
            assertEquals("leaf", leaf.getTagName());
            assertEquals(0, leaf.getAttributes().getLength());
            //"<root attr='3'><leaf>Rock &amp; Roll!</leaf><?proc instr?></root>";
            ProcessingInstruction pi = (ProcessingInstruction) nodes.item(1);
            assertEquals("proc", pi.getTarget());
            assertEquals("instr", pi.getData());
        }
    }

    @Test
    public void testDeserializeNS() throws Exception
    {
        Document doc = MAPPER.readValue(q(SIMPLE_XML_NS), Document.class);
        Element root = doc.getDocumentElement();
        assertNotNull(root);
        assertEquals("root", root.getTagName());
        // Not sure if it ought to be "" or null...
        String uri = root.getNamespaceURI();
        assertTrue((uri == null) || "".equals(uri));
        // no child nodes:
        assertEquals(0, root.getChildNodes().getLength());
        // DOM is weird, includes ns decls as attributes...
        assertEquals(2, root.getAttributes().getLength());
        assertEquals("abc", root.getAttributeNS("http://foo", "attr"));
    }

    /*
    /**********************************************************
    /* Polymorphic (`@JsonTypeInfo`) tests
    /**********************************************************
     */

    // [databind#6113]: without `serializeWithType()` override this failed with
    // `InvalidDefinitionException` ("Type id handling not implemented")
    @Test
    public void polymorphicDocumentField() throws Exception
    {
        String json = MAPPER.writeValueAsString(new DocumentWrapper(_simpleDocument()));
        // Type Id must be `Document`, not `Node`: latter is not a subtype of the
        // declared `Document` and read side would fail with `InvalidTypeIdException`
        assertEquals(Document.class.getName(), _typeIdOf(json));

        DocumentWrapper result = MAPPER.readValue(json, DocumentWrapper.class);
        assertEquals(SIMPLE_XML, _asXml(result.value));
    }

    // [databind#6113]: `Node`-declared field gets `Document` Type Id too (that being
    // the actual shape written), which resolves fine as a subtype of `Node`
    @Test
    public void polymorphicNodeField() throws Exception
    {
        String json = MAPPER.writeValueAsString(new NodeWrapper(_simpleDocument()));
        assertEquals(Document.class.getName(), _typeIdOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        assertEquals(SIMPLE_XML, _asXml(result.value));
    }

    /*
    /**********************************************************
    /* Format visitor tests
    /**********************************************************
     */

    // [databind#6113]: `Node` is written as JSON String, so must be reported
    // as such (and not as "any format")
    @Test
    public void formatVisitorReportsString() throws Exception
    {
        for (Class<?> domType : new Class<?>[] { Node.class, Document.class, Element.class }) {
            final AtomicReference<JavaType> result = new AtomicReference<>();
            MAPPER.acceptJsonFormatVisitor(domType, new JsonFormatVisitorWrapper.Base() {
                @Override
                public JsonStringFormatVisitor expectStringFormat(JavaType type) {
                    result.set(type);
                    return null;
                }
            });
            assertNotNull(result.get(), "Should have called expectStringFormat() for "+domType.getName());
            assertEquals(domType, result.get().getRawClass());
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected static String normalizeOutput(String output)
    {
        // XML declaration to get rid of?
        output = output.trim();
        if (output.startsWith("<?xml")) {
            // can find closing '>' of xml decl...
            output = output.substring(output.indexOf('>')+1).trim();
        }
        // And replace double quotes with single-quotes...
        return output.replace('"', '\'');
    }

    private Document _simpleDocument() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(SIMPLE_XML)));
    }

    // Serialize given Node with non-polymorphic mapper, to get comparable XML text
    private String _asXml(Node n) throws Exception {
        return normalizeOutput(MAPPER.readValue(MAPPER.writeValueAsString(n), String.class));
    }

    // Scalar values use WRAPPER_ARRAY inclusion, so JSON is
    // `{"value":["type.id","<xml>"]}`: dig out the Type Id
    private String _typeIdOf(String json) throws Exception {
        JsonNode wrapped = MAPPER.readTree(json).path("value");
        assertTrue(wrapped.isArray(), "Expected WRAPPER_ARRAY for typed `Node`, got: "+json);
        return wrapped.path(0).stringValue();
    }
}
