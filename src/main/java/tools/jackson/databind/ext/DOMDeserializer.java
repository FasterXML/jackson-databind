package tools.jackson.databind.ext;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.util.AccessPattern;
import tools.jackson.databind.util.ClassUtil;

/**
 * Base for deserializers that allows parsing DOM values from JSON Strings.
 * Nominal type can be {@link org.w3c.dom.Node} or one of the node types that
 * {@link DOMSerializer} writes a distinct Type Id for; see
 * {@link #findDeserializer} for the full set, and for which types fall back to
 * plain {@code Node} handling.
 */
public abstract class DOMDeserializer<T> extends FromStringDeserializer<T>
{
    /**
     * Name of throw-away root element used for parsing node types that are not
     * well-formed XML documents on their own; never part of the result.
     */
    private final static String WRAPPER_ELEMENT = "jacksonDomWrapper";

    private final static DocumentBuilderFactory DEFAULT_PARSER_FACTORY;
    static {
        DocumentBuilderFactory parserFactory = DocumentBuilderFactory.newInstance();
        // yup, only cave men do XML without recognizing namespaces...
        parserFactory.setNamespaceAware(true);
        // [databind#1279]: make sure external entities NOT expanded by default
        parserFactory.setExpandEntityReferences(false);
        // ... and in general, aim for "safety"
        try {
            parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch(ParserConfigurationException pce) {
            // not much point to do anything; could log but...
        }

        // [databind#2589] add two more settings just in case
        try {
            parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception t) { } // as per previous one, nothing much to do
        try {
            parserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception t) { } // as per previous one, nothing much to do
        DEFAULT_PARSER_FACTORY = parserFactory;
    }

    protected DOMDeserializer(Class<T> cls) { super(cls); }

    @Override
    public abstract T _deserialize(String value, DeserializationContext ctxt);

    /**
     * Lookup of deserializer for a DOM {@link Node} subtype. Order matters: the most
     * specific type must be checked first, since a check matching a supertype would
     * leave the more specific handler unreachable
     * (23-Jul-2026, [databind#6113]: `Document` vs `Node`;
     *  24-Jul-2026, [databind#6120]: the rest).
     *<p>
     * Node types with no entry here -- notably {@link Attr}, for which the
     * {@code Transformer} emits nothing usable -- fall back to {@code NodeDeserializer},
     * which can only produce a {@link Document}. Must stay in sync with the Type Ids
     * {@code DOMSerializer} writes.
     *
     * @since 3.3
     */
    public static ValueDeserializer<?> findDeserializer(Class<?> rawType)
    {
        if (Document.class.isAssignableFrom(rawType)) {
            return new DocumentDeserializer();
        }
        if (Element.class.isAssignableFrom(rawType)) {
            return new ElementDeserializer();
        }
        // `CDATASection` is a subtype of `Text`, so must precede it
        if (CDATASection.class.isAssignableFrom(rawType)) {
            return new CDATASectionDeserializer();
        }
        if (Text.class.isAssignableFrom(rawType)) {
            return new TextDeserializer();
        }
        if (Comment.class.isAssignableFrom(rawType)) {
            return new CommentDeserializer();
        }
        if (ProcessingInstruction.class.isAssignableFrom(rawType)) {
            return new ProcessingInstructionDeserializer();
        }
        if (DocumentFragment.class.isAssignableFrom(rawType)) {
            return new DocumentFragmentDeserializer();
        }
        return new NodeDeserializer();
    }

    protected final Document parse(String value) throws IllegalArgumentException {
        try {
            return documentBuilder().parse(new InputSource(new StringReader(value)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON String as XML: "+e.getMessage(), e);
        }
    }

    /**
     * Helper method for reconstructing node types that are not, by themselves,
     * well-formed XML documents (text, comments, processing instructions and so on):
     * wraps given XML text in a throw-away root element, parses that, and returns
     * the wrapper element whose children are the reconstructed node(s).
     *<p>
     * NOTE: input is concatenated into XML, but cannot widen what a caller could
     * already reach via {@link #parse}: content that closes the wrapper leaves the
     * document with 2 root elements, and a {@code DOCTYPE} may not follow a start tag
     * -- both are fatal parse errors, on top of the parser factory already disallowing
     * DOCTYPE declarations and entity expansion.
     *<p>
     * NOTE: DOM Level 3 {@code LSParser.parseWithContext()} would do this directly, but
     * the JDK's bundled parser does not implement it (throws {@code NOT_SUPPORTED_ERR}),
     * so wrapping is the only option that avoids a third-party XML dependency.
     *
     * @since 3.3
     */
    protected final Element parseWrapped(String value) throws IllegalArgumentException {
        return parse("<"+WRAPPER_ELEMENT+">"+value+"</"+WRAPPER_ELEMENT+">")
                .getDocumentElement();
    }

    /**
     * Overridable factory method used to create {@link DocumentBuilder} for parsing
     * XML as DOM.
     *
     * @since 2.7.6
     */
    protected DocumentBuilder documentBuilder() throws ParserConfigurationException {
        return DEFAULT_PARSER_FACTORY.newDocumentBuilder();
    }

    /**
     * Helper method for extracting the single child of expected type from wrapper
     * element created by {@link #parseWrapped}.
     *<p>
     * NOTE: only usable for node types with no sub-interface of their own (a
     * {@link CDATASection}, for example, would satisfy a {@link Text} check here).
     *
     * @since 3.3
     */
    protected final <N extends Node> N parseWrappedChild(String value, Class<N> expType)
        throws IllegalArgumentException
    {
        Node child = parseWrapped(value).getFirstChild();
        if (!expType.isInstance(child)) {
            throw new IllegalArgumentException(String.format(
                    "Failed to parse JSON String as XML %s node: got %s",
                    ClassUtil.nameOf(expType),
                    (child == null) ? "no content" : "node type "+child.getNodeType()));
        }
        return expType.cast(child);
    }

    /*
    /**********************************************************
    /* Concrete deserializers
    /**********************************************************
     */

    @JacksonStdImpl
    public static class NodeDeserializer extends DOMDeserializer<Node> {
        public NodeDeserializer() { super(Node.class); }
        @Override
        public Node _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }

    @JacksonStdImpl
    public static class DocumentDeserializer extends DOMDeserializer<Document> {
        public DocumentDeserializer() { super(Document.class); }
        @Override
        public Document _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }

    /**
     * @since 3.3
     */
    @JacksonStdImpl
    static class ElementDeserializer extends DOMDeserializer<Element> {
        ElementDeserializer() { super(Element.class); }
        @Override
        public Element _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            // Serialized Element is a well-formed document on its own (Transformer
            // hoists any ancestor namespace declarations into the subtree)
            return parse(value).getDocumentElement();
        }
    }

    /**
     * @since 3.3
     */
    @JacksonStdImpl
    static class TextDeserializer extends DOMDeserializer<Text> {
        TextDeserializer() { super(Text.class); }

        // 24-Jul-2026, tatu: [databind#6120] Unlike every other DOM type, the written
        //    form of a `Text` node IS its (escaped) content, with no delimiters. Default
        //    trimming would silently drop leading/trailing whitespace -- and turn a
        //    whitespace-only node (indentation in any pretty-printed document) into
        //    an empty String, and thereby `null`.
        @Override
        protected boolean _shouldTrim() { return false; }

        // ... and for the same reason empty content is a legal `Text` node, not `null`.
        // NOTE: must go via `getEmptyValue()`, which is what `CoercionAction.AsEmpty`
        // and `@JsonSetter(contentNulls=AS_EMPTY)` consult (compare to
        // `JDKFromStringDeserializer`)
        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return _deserialize("", ctxt);
        }

        // ... and unlike the immutable scalars `StdScalarDeserializer` assumes, a DOM
        // node is mutable and carries a parent pointer (`appendChild()` reparents), so
        // the empty value must NOT be shared: `CONSTANT` would have
        // `NullsConstantProvider` hand every null the very same node
        @Override
        public AccessPattern getEmptyAccessPattern() {
            return AccessPattern.DYNAMIC;
        }

        @Override
        protected Object _deserializeFromEmptyStringDefault(DeserializationContext ctxt) {
            return getEmptyValue(ctxt);
        }

        @Override
        public Text _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            // Cannot just take the first child: parser is allowed to split character
            // data into multiple `Text` nodes
            Element wrapper = parseWrapped(value);
            return wrapper.getOwnerDocument().createTextNode(wrapper.getTextContent());
        }
    }

    /**
     * @since 3.3
     */
    @JacksonStdImpl
    static class CDATASectionDeserializer extends DOMDeserializer<CDATASection> {
        CDATASectionDeserializer() { super(CDATASection.class); }
        @Override
        public CDATASection _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            // Same as with `Text`: content containing "]]>" is written out as multiple
            // CDATA sections, so recombine into one
            Element wrapper = parseWrapped(value);
            return wrapper.getOwnerDocument().createCDATASection(wrapper.getTextContent());
        }
    }

    /**
     * @since 3.3
     */
    @JacksonStdImpl
    static class CommentDeserializer extends DOMDeserializer<Comment> {
        CommentDeserializer() { super(Comment.class); }
        @Override
        public Comment _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parseWrappedChild(value, Comment.class);
        }
    }

    /**
     * @since 3.3
     */
    @JacksonStdImpl
    static class ProcessingInstructionDeserializer extends DOMDeserializer<ProcessingInstruction> {
        ProcessingInstructionDeserializer() { super(ProcessingInstruction.class); }
        @Override
        public ProcessingInstruction _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parseWrappedChild(value, ProcessingInstruction.class);
        }
    }

    /**
     * @since 3.3
     */
    @JacksonStdImpl
    static class DocumentFragmentDeserializer extends DOMDeserializer<DocumentFragment> {
        DocumentFragmentDeserializer() { super(DocumentFragment.class); }
        @Override
        public DocumentFragment _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            Element wrapper = parseWrapped(value);
            Document doc = wrapper.getOwnerDocument();
            DocumentFragment frag = doc.createDocumentFragment();
            // NOTE: `appendChild()` detaches from wrapper, so this consumes the list
            for (Node child; (child = wrapper.getFirstChild()) != null; ) {
                frag.appendChild(child);
            }
            return frag;
        }
    }
}
