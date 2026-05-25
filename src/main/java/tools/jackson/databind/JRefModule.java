package tools.jackson.databind;

import java.util.List;
import java.util.function.Function;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.type.ReferenceType;

public class JRefModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public JRefModule() {
		super("JRef");
	}

	protected class JRefValueDeserializerModifier extends ValueDeserializerModifier {

		private static final long serialVersionUID = 1L;

		protected ValueDeserializer<?> makeJRefValueDeserializer(DeserializationConfig config, JavaType jt,
				Supplier beanDescRef, ValueDeserializer<?> d) {
			return new JRefValueDeserializer(d);
		}

		@Override
		public ValueDeserializer<?> modifyArrayDeserializer(DeserializationConfig config, ArrayType valueType,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, valueType, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyCollectionLikeDeserializer(DeserializationConfig config,
				CollectionLikeType type, Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, Supplier beanDescRef,
				ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, null, beanDescRef, deserializer);
		}

		@Override
		public KeyDeserializer modifyKeyDeserializer(DeserializationConfig config, JavaType type,
				KeyDeserializer deserializer) {
			return super.modifyKeyDeserializer(config, type, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyMapLikeDeserializer(DeserializationConfig config, MapLikeType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, Supplier beanDescRef,
				BeanDeserializerBuilder builder) {
			return super.updateBuilder(config, beanDescRef, builder);
		}

		@Override
		public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, Supplier beanDescRef,
				List<BeanPropertyDefinition> propDefs) {
			return super.updateProperties(config, beanDescRef, propDefs);
		}

		@Override
		public ValueDeserializer<?> modifyReferenceDeserializer(DeserializationConfig config, ReferenceType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return super.modifyReferenceDeserializer(config, type, beanDescRef, deserializer);
		}
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addDeserializerModifier(new JRefValueDeserializerModifier());
	}

	public static class JRefValueDeserializer extends DelegatingDeserializer {

		void trace(String method, JRefValueDeserializer vds) {
			System.out.println(new StringBuffer(method).append(".").append(vds.toString().toString()));
		}

		@Override
		public String toString() {
			return "JRefValueDeserializer[delegate=" + _delegatee + ", class=" + _valueClass + "]";
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

		protected TreeTraversingParser createTreeTraversingParser(JsonNode node) {
			TreeTraversingParser result = new TreeTraversingParser(node);
			result.nextToken();
			return result;
		}

		/**
		 * Find JRef in parser input stream.
		 * 
		 * @param p                JsonParser to use
		 * @param ctxt             the current context
		 * @param typeDeserializer optional typeDeserializer
		 * @return JRefFindResult with JRefPath either set to non null (path found), or
		 *         set to null (meaning that the further deserialization should be
		 *         undertaken with the parse given in JRefFindResult
		 */
		protected JRefFindResult findJRef(JsonParser p, DeserializationContext ctxt,
				TypeDeserializer typeDeserializer) {
			JRefFindResult result = new JRefFindResult();
			JsonToken tok = p.currentToken();
			if (tok == JsonToken.START_OBJECT) {
				JsonNode n = ctxt.readTree(p);
				if (n instanceof ObjectNode) {
					ObjectNode on = (ObjectNode) n;
					JsonNode valNode = on.get(JRefPath.JREF_REF);
					if (valNode != null) {
						String valStr = valNode.asString();
						if (valStr != null) {
							result.jrefPath = new JRefPath(valStr, ctxt, getDelegatee(), typeDeserializer);
							result.parser = p;
							return result;
						}
						// JREF_REF found, but no/null path. This is a syntax error
						throw DatabindException.from(p, "JRefPath detected on stream but path is null", null);
					}
				}
				result.parser = createTreeTraversingParser(n);
			} else {
				result.parser = p;
			}
			return result;
		}

		/**
		 * Deserialize with jref handling. Calls
		 * {@link #findJRef(JsonParser, DeserializationContext, TypeDeserializer)} to
		 * look on the parser stream for objects with 1 key == '$ref'. If found then the
		 * JRefPath is returned. If not found then the func arg is applied with the
		 * appropriate parser returned in JRefFindResult.parser field.
		 * 
		 * @param p
		 * @param ctxt
		 * @param typeDeserializer
		 * @param func
		 * @return
		 */
		protected Object deserializerWithJRef(JsonParser p, DeserializationContext ctxt,
				TypeDeserializer typeDeserializer, Function<JsonParser, Object> func) {
			Object result = null;
			JRefFindResult findResult = findJRef(p, ctxt, typeDeserializer);
			if (findResult.jrefPath != null) {
				result = findResult.jrefPath;
			} else {
				result = func.apply(findResult.parser);
			}
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

}
