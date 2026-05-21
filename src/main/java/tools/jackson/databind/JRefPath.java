package tools.jackson.databind;

import java.util.Objects;

public class JRefPath {

	private final DeserializationContext ctxt;
	private final String path;
	private final ValueDeserializer<?> deserializer;
	
	public JRefPath(String path, DeserializationContext ctxt, ValueDeserializer<?> deserializer) {
		Objects.requireNonNull(path, "path cannot be null");
		this.path = path;
		Objects.requireNonNull(ctxt, "ctxt cannot be null");
		this.ctxt = ctxt;
		Objects.requireNonNull(deserializer, "deserializer cannot be null");
		this.deserializer = deserializer;
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

	@Override
	public String toString() {
		return "JRefPath [ctxt=" + ctxt + ", path=" + path + ", deserializer=" + deserializer + "]";
	}
	
}
