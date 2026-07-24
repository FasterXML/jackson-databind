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
import tools.jackson.databind.exc.InvalidFormatException;
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

    // [databind#6120]: node types not present in `SIMPLE_XML`
    final static String MIXED_XML =
        "<root><![CDATA[cd & data]]><!--comment--></root>";

    // [databind#6113]: wrappers for polymorphic handling; [databind#6120] extended
    // the set of DOM types for which deserializers exist (see `OptionalHandlerFactory`)
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
    /* Deserializer resolution tests
    /**********************************************************
     */

    // [databind#6113]: `DocumentDeserializer` used to be unreachable, since lookup
    // checked `Node` first and `Document` is a subtype of it. Only externally visible
    // difference is the target type reported on failure, so verify via that.
    @Test
    public void documentUsesDocumentDeserializer() throws Exception
    {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> MAPPER.readValue(q("not xml at all"), Document.class));
        verifyException(e, "Cannot deserialize value of type `org.w3c.dom.Document`");
    }

    // ... whereas `Node`-declared values keep using `NodeDeserializer`
    @Test
    public void nodeUsesNodeDeserializer() throws Exception
    {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> MAPPER.readValue(q("not xml at all"), Node.class));
        verifyException(e, "Cannot deserialize value of type `org.w3c.dom.Node`");
    }

    // [databind#6120]: each reconstructible node type now resolves to its own
    // deserializer, so it may also be used as the declared type directly
    @Test
    public void readAsElement() throws Exception
    {
        Element el = MAPPER.readValue(q("<leaf>Rock &amp; Roll!</leaf>"), Element.class);
        _assertNodeType(Node.ELEMENT_NODE, Element.class, el);
        assertEquals("leaf", el.getTagName());
        assertEquals("Rock & Roll!", el.getTextContent());
    }

    @Test
    public void readAsText() throws Exception
    {
        Text text = MAPPER.readValue(q("Rock &amp; Roll!"), Text.class);
        _assertNodeType(Node.TEXT_NODE, Text.class, text);
        assertEquals("Rock & Roll!", text.getData());
    }

    @Test
    public void readAsComment() throws Exception
    {
        Comment c = MAPPER.readValue(q("<!--comment-->"), Comment.class);
        _assertNodeType(Node.COMMENT_NODE, Comment.class, c);
        assertEquals("comment", c.getData());
    }

    // ... and failure to reconstruct expected node type is reported as usual
    @Test
    public void readAsCommentFailsForNonComment() throws Exception
    {
        InvalidFormatException e = assertThrows(InvalidFormatException.class,
                () -> MAPPER.readValue(q("<elem/>"), Comment.class));
        verifyException(e, "Failed to parse JSON String as XML Comment node");
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
        _assertNodeType(Node.DOCUMENT_NODE, Document.class, result.value);
        assertEquals(SIMPLE_XML, _asXml(result.value));
    }

    // [databind#6113]: `Node`-declared field gets `Document` Type Id too (that being
    // the actual shape written), which resolves fine as a subtype of `Node`
    @Test
    public void polymorphicNodeFieldWithDocument() throws Exception
    {
        String json = MAPPER.writeValueAsString(new NodeWrapper(_simpleDocument()));
        assertEquals(Document.class.getName(), _typeIdOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.DOCUMENT_NODE, Document.class, result.value);
        assertEquals(SIMPLE_XML, _asXml(result.value));
    }

    // [databind#6120]: `Element` value now gets an `Element` Type Id and is
    // reconstructed as an `Element` (before, it came back as a `Document`)
    @Test
    public void polymorphicNodeFieldWithElement() throws Exception
    {
        Element leaf = (Element) _simpleDocument().getDocumentElement()
                .getElementsByTagName("leaf").item(0);
        _assertNodeType(Node.ELEMENT_NODE, Element.class, leaf);

        String json = MAPPER.writeValueAsString(new NodeWrapper(leaf));
        assertEquals(Element.class.getName(), _typeIdOf(json));
        assertEquals("<leaf>Rock &amp; Roll!</leaf>", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.ELEMENT_NODE, Element.class, result.value);
        Element resultLeaf = (Element) result.value;
        assertEquals("leaf", resultLeaf.getTagName());
        assertEquals("Rock & Roll!", resultLeaf.getTextContent());
    }

    // [databind#6120]: namespaces survive subtree serialization -- the Transformer
    // hoists the ancestor declaration into the subtree it writes
    @Test
    public void polymorphicNodeFieldWithNamespacedElement() throws Exception
    {
        Document doc = _parse("<root xmlns:ns='http://foo'><ns:leaf ns:a='1'>text</ns:leaf></root>");
        Element leaf = (Element) doc.getDocumentElement().getFirstChild();
        _assertNodeType(Node.ELEMENT_NODE, Element.class, leaf);

        String json = MAPPER.writeValueAsString(new NodeWrapper(leaf));
        assertEquals(Element.class.getName(), _typeIdOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.ELEMENT_NODE, Element.class, result.value);
        Element resultLeaf = (Element) result.value;
        assertEquals("http://foo", resultLeaf.getNamespaceURI());
        assertEquals("leaf", resultLeaf.getLocalName());
        assertEquals("1", resultLeaf.getAttributeNS("http://foo", "a"));
        assertEquals("text", resultLeaf.getTextContent());
    }

    // [databind#6120]: `Text` node gets a `Text` Type Id, and is rebuilt by wrapping
    // the (escaped) text in a throw-away root element before parsing
    @Test
    public void polymorphicNodeFieldWithText() throws Exception
    {
        Text text = (Text) _simpleDocument().getDocumentElement()
                .getElementsByTagName("leaf").item(0).getFirstChild();
        _assertNodeType(Node.TEXT_NODE, Text.class, text);

        String json = MAPPER.writeValueAsString(new NodeWrapper(text));
        assertEquals(Text.class.getName(), _typeIdOf(json));
        assertEquals("Rock &amp; Roll!", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.TEXT_NODE, Text.class, result.value);
        // Escaping round-trips exactly
        assertEquals("Rock & Roll!", ((Text) result.value).getData());
    }

    // [databind#6120]: `CDATASection` is a subtype of `Text` but must keep its own
    // Type Id, or it would come back as a plain `Text` node
    @Test
    public void polymorphicNodeFieldWithCDATA() throws Exception
    {
        CDATASection cdata = (CDATASection) _parse(MIXED_XML).getDocumentElement()
                .getFirstChild();
        _assertNodeType(Node.CDATA_SECTION_NODE, CDATASection.class, cdata);

        String json = MAPPER.writeValueAsString(new NodeWrapper(cdata));
        assertEquals(CDATASection.class.getName(), _typeIdOf(json));
        assertEquals("<![CDATA[cd & data]]>", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.CDATA_SECTION_NODE, CDATASection.class, result.value);
        assertEquals("cd & data", ((CDATASection) result.value).getData());
    }

    @Test
    public void polymorphicNodeFieldWithComment() throws Exception
    {
        Comment comment = (Comment) _parse(MIXED_XML).getDocumentElement()
                .getLastChild();
        _assertNodeType(Node.COMMENT_NODE, Comment.class, comment);

        String json = MAPPER.writeValueAsString(new NodeWrapper(comment));
        assertEquals(Comment.class.getName(), _typeIdOf(json));
        assertEquals("<!--comment-->", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.COMMENT_NODE, Comment.class, result.value);
        assertEquals("comment", ((Comment) result.value).getData());
    }

    @Test
    public void polymorphicNodeFieldWithProcessingInstruction() throws Exception
    {
        ProcessingInstruction pi = (ProcessingInstruction) _simpleDocument()
                .getDocumentElement().getLastChild();
        _assertNodeType(Node.PROCESSING_INSTRUCTION_NODE, ProcessingInstruction.class, pi);

        String json = MAPPER.writeValueAsString(new NodeWrapper(pi));
        assertEquals(ProcessingInstruction.class.getName(), _typeIdOf(json));
        assertEquals("<?proc instr?>", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.PROCESSING_INSTRUCTION_NODE, ProcessingInstruction.class, result.value);
        assertEquals("proc", ((ProcessingInstruction) result.value).getTarget());
        assertEquals("instr", ((ProcessingInstruction) result.value).getData());
    }

    @Test
    public void polymorphicNodeFieldWithDocumentFragment() throws Exception
    {
        Document doc = _parse(SIMPLE_XML);
        DocumentFragment frag = doc.createDocumentFragment();
        frag.appendChild(doc.createElement("a"));
        frag.appendChild(doc.createElement("b"));

        String json = MAPPER.writeValueAsString(new NodeWrapper(frag));
        assertEquals(DocumentFragment.class.getName(), _typeIdOf(json));
        assertEquals("<a/><b/>", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        _assertNodeType(Node.DOCUMENT_FRAGMENT_NODE, DocumentFragment.class, result.value);
        NodeList children = result.value.getChildNodes();
        assertEquals(2, children.getLength());
        assertEquals("a", ((Element) children.item(0)).getTagName());
        assertEquals("b", ((Element) children.item(1)).getTagName());
    }

    // [databind#6120]: `Attr` remains unsupported -- the `Transformer` emits an empty
    // String for it, so name and value are gone before the read side is reached.
    // Verified here so that a change in behavior is caught.
    @Test
    public void polymorphicNodeFieldWithAttr() throws Exception
    {
        Attr attr = _simpleDocument().getDocumentElement().getAttributeNode("attr");
        _assertNodeType(Node.ATTRIBUTE_NODE, Attr.class, attr);

        String json = MAPPER.writeValueAsString(new NodeWrapper(attr));
        // No usable content written, so no point in a more specific Type Id
        assertEquals(Node.class.getName(), _typeIdOf(json));
        assertEquals("", _valueOf(json));

        NodeWrapper result = MAPPER.readValue(json, NodeWrapper.class);
        assertNull(result.value);
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
        return _parse(SIMPLE_XML);
    }

    private Document _parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    // Serialize given Node with non-polymorphic mapper, to get comparable XML text
    private String _asXml(Node n) throws Exception {
        return normalizeOutput(MAPPER.readValue(MAPPER.writeValueAsString(n), String.class));
    }

    // Scalar values use WRAPPER_ARRAY inclusion, so JSON is
    // `{"value":["type.id","<xml>"]}`: dig out the Type Id
    private String _typeIdOf(String json) throws Exception {
        return _wrapperArrayOf(json).path(0).stringValue();
    }

    // ... and matching XML text payload
    private String _valueOf(String json) throws Exception {
        return _wrapperArrayOf(json).path(1).stringValue();
    }

    private JsonNode _wrapperArrayOf(String json) throws Exception {
        JsonNode wrapped = MAPPER.readTree(json).path("value");
        assertTrue(wrapped.isArray(), "Expected WRAPPER_ARRAY for typed `Node`, got: "+json);
        assertEquals(2, wrapped.size(), "Expected [typeId, value], got: "+json);
        return wrapped;
    }

    // Verify exact DOM type: `getNodeType()` is the canonical discriminator (node
    // types are mutually exclusive), since runtime classes are JDK-internal impls
    // like `com.sun.org.apache.xerces.internal.dom.DeferredDocumentImpl`
    private void _assertNodeType(short expNodeType, Class<? extends Node> expType, Node actual) {
        assertNotNull(actual, "Expected "+expType.getName()+", got `null`");
        assertEquals(expNodeType, actual.getNodeType(),
                "Wrong `getNodeType()` for value of class "+actual.getClass().getName());
        assertInstanceOf(expType, actual);
    }
}
