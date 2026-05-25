package tools.jackson.databind;

import java.util.Objects;

import tools.jackson.databind.jsontype.TypeDeserializer;

public class JRefPath {

	public static final String JREF_REF = "$ref";
	private final DeserializationContext ctxt;
	private final String path;
	private final ValueDeserializer<?> deserializer;
	private final TypeDeserializer typeDeserializer;

	public JRefPath(String path, DeserializationContext ctxt, ValueDeserializer<?> deserializer,
			TypeDeserializer typeDeserializer) {
		Objects.requireNonNull(path, "path cannot be null");
		this.path = path;
		Objects.requireNonNull(ctxt, "ctxt cannot be null");
		this.ctxt = ctxt;
		Objects.requireNonNull(deserializer, "deserializer cannot be null");
		this.deserializer = deserializer;
		this.typeDeserializer = typeDeserializer;
	}

	public JRefPath(String path, DeserializationContext ctxt, ValueDeserializer<?> deserializer) {
		this(path, ctxt, deserializer, null);
	}

	public DeserializationContext getContext() {
		return this.ctxt;
	}

	public String getPath() {
		return this.path;
	}

	public ValueDeserializer<?> getValueDeserializer() {
		return deserializer;
	}

	public TypeDeserializer getTypeDeserializer() {
		return typeDeserializer;
	}

	@Override
	public String toString() {
		return "JRefPath[ctxt=" + ctxt + ", path=" + path + ", deserializer=" + deserializer + ", typeDeserializer="
				+ typeDeserializer + "]";
	}

}
