package tools.jackson.databind.ext;

import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import tools.jackson.core.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.ser.std.StdSerializer;

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
    public void serialize(Node value, JsonGenerator g, SerializerProvider provider)
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
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        if (visitor != null) visitor.expectAnyFormat(typeHint);
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
