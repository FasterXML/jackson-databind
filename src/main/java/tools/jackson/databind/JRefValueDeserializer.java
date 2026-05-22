package tools.jackson.databind;

import java.util.function.Function;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;

public class JRefValueDeserializer extends DelegatingDeserializer {

	public static final String JREF = "$ref";

	void trace(String method, JRefValueDeserializer vds) {
		System.out.println(new StringBuffer(method).append(".").append(vds.toString().toString()));
	}

	@Override
	public String toString() {
		return "JRefVD[d=" + _delegatee + ", class=" + _valueClass + "]";
	}

	void trace(String method) {
		trace(method, this);
	}

	public JRefValueDeserializer(ValueDeserializer<?> src) {
		super(src);
	}

	protected class JRefFindResult {
		JRefPath jrefPath;
		JsonParser parser;
	}

	/**
	 * Check for JRef in parser with context.
	 * 
	 * @param p                JsonParser to use
	 * @param ctxt             the current context
	 * @param typeDeserializer optional typeDeserializer
	 * @return JRefFindResult with JRefPath either set to non null (path found), or
	 *         set to null (meaning that the further deserialization should be
	 *         undertaken with the parse given in JRefFindResult
	 */
	protected JRefFindResult checkForJRef(JsonParser p, DeserializationContext ctxt,
			TypeDeserializer typeDeserializer) {
		JRefFindResult result = new JRefFindResult();
		JsonToken tok = p.currentToken();
		if (tok == JsonToken.START_OBJECT) {
			JsonNode n = ctxt.readTree(p);
			if (n instanceof ObjectNode) {
				ObjectNode on = (ObjectNode) n;
				JsonNode valNode = on.get(JREF);
				if (valNode != null) {
					String valStr = valNode.asString();
					if (valStr != null) {
						result.jrefPath = new JRefPath(valStr, ctxt, this, typeDeserializer);
						result.parser = p;
						return result;
					}
				}
			}
			@SuppressWarnings("resource")
			TreeTraversingParser tpp = new TreeTraversingParser(n);
			tpp.nextToken();
			result.parser = tpp;
		} else {
			result.parser = p;
		}
		return result;
	}

	/**
	 * Deserialize with jref handling. Calls
	 * {@link #checkForJRef(JsonParser, DeserializationContext, TypeDeserializer)}
	 * to look on the parser stream for objects with 1 key == '$ref'. If found then
	 * the JRefPath is returned. If not found then the func arg is applied with the
	 * appropriate parser returned in JRefFindResult.parser field.
	 * 
	 * @param p
	 * @param ctxt
	 * @param typeDeserializer
	 * @param func
	 * @return
	 */
	protected Object deserializerWithJRef(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer,
			Function<JsonParser, Object> func) {
		String method = "deserialize" + ((typeDeserializer == null) ? "" : "WithType");
		trace(method);
		Object result = null;
		JRefFindResult findResult = checkForJRef(p, ctxt, typeDeserializer);
		if (findResult.jrefPath != null) {
			trace("   JRefPath=" + findResult.jrefPath);
			result = findResult.jrefPath;
		} else {
			result = func.apply(findResult.parser);
		}
		trace(method + " result=" + result);
		return result;

	}

	@Override
	public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
			throws JacksonException {
		return deserializerWithJRef(p, ctxt, typeDeserializer,
				ps -> super.deserializeWithType(ps, ctxt, typeDeserializer));
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		return deserializerWithJRef(p, ctxt, null, ps -> super.deserialize(ps, ctxt));
	}

	@Override
	protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
		return new JRefValueDeserializer(newDelegatee);
	}

}
