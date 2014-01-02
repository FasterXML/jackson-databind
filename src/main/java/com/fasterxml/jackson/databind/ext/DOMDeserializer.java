package com.fasterxml.jackson.databind.ext;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

/**
 * Base for serializers that allows parsing DOM Documents from JSON Strings.
 * Nominal type can be either {@link org.w3c.dom.Node} or
 * {@link org.w3c.dom.Document}.
 */
public abstract class DOMDeserializer<T> extends FromStringDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    private final static DocumentBuilderFactory _parserFactory;
    static {
        _parserFactory = DocumentBuilderFactory.newInstance();
        // yup, only cave men do XML without recognizing namespaces...
        _parserFactory.setNamespaceAware(true);
    }

    protected DOMDeserializer(Class<T> cls) { super(cls); }

    @Override
    public abstract T _deserialize(String value, DeserializationContext ctxt);

    protected final Document parse(String value) throws IllegalArgumentException {
        try {
            return _parserFactory.newDocumentBuilder().parse(new InputSource(new StringReader(value)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON String as XML: "+e.getMessage(), e);
        }
    }

    /*
    /**********************************************************
    /* Concrete deserializers
    /**********************************************************
     */
    
    public static class NodeDeserializer extends DOMDeserializer<Node> {
        private static final long serialVersionUID = 1L;
        public NodeDeserializer() { super(Node.class); }
        @Override
        public Node _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }    

    public static class DocumentDeserializer extends DOMDeserializer<Document> {
        private static final long serialVersionUID = 1L;
        public DocumentDeserializer() { super(Document.class); }
        @Override
        public Document _deserialize(String value, DeserializationContext ctxt) throws IllegalArgumentException {
            return parse(value);
        }
    }    
}
