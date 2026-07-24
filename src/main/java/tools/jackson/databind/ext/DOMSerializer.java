package tools.jackson.databind.ext;

import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

@JacksonStdImpl
public class DOMSerializer extends StdSerializer<Node>
{
    protected final TransformerFactory transformerFactory;

    public DOMSerializer() {
        super(Node.class);
        try {
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
	    // 22-Mar-2023, tatu: [databind#3837] add these 2 settings further
            setTransformerFactoryAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
            setTransformerFactoryAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate `TransformerFactory`: "+e.getMessage(), e);
        }
    }

    @Override
    public void serialize(Node value, JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(new DOMSource(value), result);
            g.writeString(result.getWriter().toString());
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("Could not create XML Transformer for writing DOM `Node` value: "+e.getMessage(), e);
        } catch (TransformerException e) {
            provider.reportMappingProblem(e, "DOM `Node` value serialization failed: %s", e.getMessage());
        }
    }

    @Override
    public void serializeWithType(Node value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        // 23-Jul-2026, tatu: [databind#6113] `Node` written as XML text in JSON String,
        //    so needs same handling as `StdScalarSerializer` (compare to
        //    `XMLGregorianCalendarSerializer` / [databind#3217])

        // Also: must not use runtime type, as that is JDK-internal implementation
        // class (like `com.sun.org.apache.xerces.internal.dom.DocumentImpl`); but
        // cannot always use `Node` either, since that is not a subtype of `Document`
        // and would fail on deserialization of `Document`-declared values
        Class<?> typeForId = _typeIdClassFor(value);
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, typeForId, JsonToken.VALUE_STRING));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        // 23-Jul-2026, tatu: [databind#6113] `Node` is written as XML text in JSON
        //    String, so report as such (and not as "any format")
        visitStringFormat(visitor, typeHint);
    }

    /**
     * Helper method for finding the DOM interface to use as the Type Id for given
     * value: switches on {@link Node#getNodeType()} since runtime classes are
     * JDK-internal implementation types, and node types are mutually exclusive.
     *<p>
     * Returns the specific interface for the node types {@link DOMDeserializer} can
     * reconstruct; plain {@code Node} for the rest (notably {@link org.w3c.dom.Attr},
     * for which the {@code Transformer} emits nothing usable -- see [databind#6120]).
     *
     * @since 3.3
     */
    private static Class<? extends Node> _typeIdClassFor(Node value)
    {
        switch (value.getNodeType()) {
        case Node.DOCUMENT_NODE:
            return Document.class;
        case Node.ELEMENT_NODE:
            return Element.class;
        // NOTE: `CDATASection` is a subtype of `Text` but needs its own Type Id, or
        // it would be read back as a plain `Text` node
        case Node.CDATA_SECTION_NODE:
            return CDATASection.class;
        case Node.TEXT_NODE:
            return Text.class;
        case Node.COMMENT_NODE:
            return Comment.class;
        case Node.PROCESSING_INSTRUCTION_NODE:
            return ProcessingInstruction.class;
        case Node.DOCUMENT_FRAGMENT_NODE:
            return DocumentFragment.class;
        default:
            return Node.class;
        }
    }

    private static void setTransformerFactoryAttribute(final TransformerFactory transformerFactory,
                                                       final String name, final Object value) {
        try {
            transformerFactory.setAttribute(name, value);
        } catch (Exception e) {
            System.err.println("[DOMSerializer] Failed to set TransformerFactory attribute: " + name);
        }
    }
}
