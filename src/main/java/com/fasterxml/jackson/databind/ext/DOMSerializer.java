package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("serial")
public class DOMSerializer extends StdSerializer<Node>
{
    protected final TransformerFactory transformerFactory;

    public DOMSerializer() {
        super(Node.class);
        try {
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate `TransformerFactory`: "+e.getMessage(), e);
        }
    }

    @Override
    public void serialize(Node value, JsonGenerator g, SerializerProvider provider)
        throws IOException
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

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, java.lang.reflect.Type typeHint) {
        // Well... it is serialized as String
        return createSchemaNode("string", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
        if (visitor != null) visitor.expectAnyFormat(typeHint);
    }
}
