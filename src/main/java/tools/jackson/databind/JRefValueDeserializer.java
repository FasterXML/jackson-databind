package tools.jackson.databind;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;

public class JRefValueDeserializer extends DelegatingDeserializer {

	public static final String JREF = "$ref";
	
	void trace(String method, JRefValueDeserializer vds) {
		StringBuffer buf = new StringBuffer(method).append(".");
		buf.append(vds.toString());
		System.out.println(buf.toString());
	}

	void trace(String method) {
		trace(method, this);
	}

	public JRefValueDeserializer(ValueDeserializer<?> src) {
		super(src);
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		trace("deserialize");
		Object result = null;
		JsonToken current = p.currentToken();		
		if (current == JsonToken.START_OBJECT) {
			JsonNode node = ctxt.readTree(p);
			if (node instanceof ObjectNode) {
				ObjectNode onode = (ObjectNode) node;
				JsonNode valNode = onode.get(JREF);
				if (valNode != null) {
					String valStr = valNode.asString();
					if (valStr != null) {
						return new JRefResolver(ctxt, valStr);
					}
				}
			}
			TreeTraversingParser tpp = new TreeTraversingParser(node);
			tpp.nextToken();
			result = super.deserialize(tpp, ctxt);
		} else {
			result = super.deserialize(p, ctxt);
		}
		trace("deserialized result=" + result);
		return result;
	}

	@Override
	protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
		return new JRefValueDeserializer(newDelegatee);
	}

}
