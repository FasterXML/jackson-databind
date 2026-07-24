package tools.jackson.databind.ext.xml;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class DOMTypeReadWriteTest extends DatabindTestUtil
{
    final static String SIMPLE_XML =
        "<root attr='3'><leaf>Rock &amp; Roll!</leaf><?proc instr?></root>";
    final static String SIMPLE_XML_NS =
        "<root ns:attr='abc' xmlns:ns='http://foo' />";
    final static String SIMPLE_XML_DEFAULT_NS =
            "<root xmlns='http://foo'/>";

    static class DocHolder {
        public Document doc;

        protected DocHolder() { }
        public DocHolder(Document d) { doc = d; }
    }

    static class NodeHolder {
        public Node node;

        protected NodeHolder() { }
        public NodeHolder(Node n) { node = n; }
    }

    static class AnnotatedDocHolder {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Document doc;

        protected AnnotatedDocHolder() { }
        public AnnotatedDocHolder(Document d) { doc = d; }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();
    // Same default-typing setup as MiscJavaXMLTypesReadWriteTest for XMLGregorianCalendar
    private final ObjectMapper POLY_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                    DefaultTyping.NON_FINAL)
            .build();

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
    /* Polymorphic (default typing) tests
    /**********************************************************
     */

    /**
     * DOM Node is serialized as a JSON String (XML text). With default typing
     * enabled this must go through {@code serializeWithType}, same as
     * {@code XMLGregorianCalendarSerializer} (see databind#3217). Without that
     * override the base {@code ValueSerializer.serializeWithType} throws
     * {@code InvalidDefinitionException}.
     */
    @Test
    public void testPolymorphicDOMDocument() throws Exception
    {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse
            (new InputSource(new StringReader(SIMPLE_XML)));
        // Before fix: InvalidDefinitionException "Type id handling not implemented"
        // After fix: typed JSON string that round-trips to Document
        String json = POLY_MAPPER.writeValueAsString(doc);
        Object result = POLY_MAPPER.readValue(json, Object.class);
        assertTrue(result instanceof Document || result instanceof Node,
                "Expected a DOM Node/Document, got: " + result.getClass());
        String output = MAPPER.writeValueAsString(result);
        String normalized = normalizeOutput(MAPPER.readValue(output, String.class));
        assertEquals(SIMPLE_XML, normalized);
    }

    // [databind#6113]: Type Id must be `Document` (not `Node`) for `Document` values,
    // or else read side fails with "not a subtype of `org.w3c.dom.Document`"
    @Test
    public void polymorphicDocumentDeclaredType() throws Exception
    {
        String json = POLY_MAPPER.writeValueAsString(_simpleDocument());
        assertEquals(Document.class.getName(), _typeIdOf(json));
        Document result = POLY_MAPPER.readValue(json, Document.class);
        assertEquals(SIMPLE_XML, _asXml(result));
    }

    // [databind#6113]
    @Test
    public void polymorphicDocumentProperty() throws Exception
    {
        String json = POLY_MAPPER.writeValueAsString(new DocHolder(_simpleDocument()));
        DocHolder result = POLY_MAPPER.readValue(json, DocHolder.class);
        assertEquals(SIMPLE_XML, _asXml(result.doc));
    }

    // [databind#6113]: also needs to work via explicit `@JsonTypeInfo`, not just
    // default typing
    @Test
    public void polymorphicDocumentViaAnnotation() throws Exception
    {
        String json = MAPPER.writeValueAsString(new AnnotatedDocHolder(_simpleDocument()));
        AnnotatedDocHolder result = MAPPER.readValue(json, AnnotatedDocHolder.class);
        assertEquals(SIMPLE_XML, _asXml(result.doc));
    }

    // [databind#6113]: `Node`-declared values still get `Document` Type Id (that
    // being the runtime shape), which resolves fine as a subtype of `Node`
    @Test
    public void polymorphicNodeProperty() throws Exception
    {
        String json = POLY_MAPPER.writeValueAsString(new NodeHolder(_simpleDocument()));
        NodeHolder result = POLY_MAPPER.readValue(json, NodeHolder.class);
        assertEquals(SIMPLE_XML, _asXml(result.node));
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

    // Extract Type Id from `["type.id", "value"]` wrapper
    private String _typeIdOf(String json) throws Exception {
        return MAPPER.readValue(json, java.util.List.class).get(0).toString();
    }
}
