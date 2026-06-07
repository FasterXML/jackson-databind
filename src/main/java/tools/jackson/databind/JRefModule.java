package tools.jackson.databind;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.TreeTraversingParser;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.std.DelegatingSerializer;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.MapType;

public class JRefModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public JRefModule() {
		super("JRefModule");
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addDeserializerModifier(new JRefValueDeserializerModifier());
		context.addSerializerModifier(new JRefValueSerializerModifier());
	}

	@FunctionalInterface
	interface Serializer {
		void serialize() throws RuntimeException;
	}

	public class JRefValueSerializerModifier extends ValueSerializerModifier {

		private static final long serialVersionUID = 1L;

		static final String PTR_MAP_ATTR = JRefValueSerializerModifier.class.getName() + ".ptrMap";

		public class JRefValueSerializer extends DelegatingSerializer {

			protected JRefValueSerializer(ValueSerializer<?> delegatee) {
				super(delegatee);
			}

			@Override
			public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
				// If the serialization context doesn't already have it, a new
				// Integer->JsonPointer
				// map is set to PTR_MAP_ATTRIBUTE for lookup and addition of serialized values
				if (ctxt.getAttribute(PTR_MAP_ATTR) == null) {
					ctxt.setAttribute(PTR_MAP_ATTR, new ConcurrentHashMap<>());
				}
				return super.createContextual(ctxt, property);
			}

			@SuppressWarnings("unchecked")
			protected JsonPointer findJsonPointer(Object value, SerializationContext ctxt) {
				if (value != null) {
					return ((Map<Integer, JsonPointer>) ctxt.getAttribute(PTR_MAP_ATTR))
							.get(System.identityHashCode(value));
				}
				return null;
			}

			@SuppressWarnings("unchecked")
			protected void addJsonPointerIfComplete(Object value, JsonGenerator gen, SerializationContext ctxt) {
				if (value != null) {
					TokenStreamContext writeContext = gen.streamWriteContext();
					if (writeContext.hasPathSegment()) {
						((Map<Integer, JsonPointer>) ctxt.getAttribute(PTR_MAP_ATTR))
								.put(System.identityHashCode(value), JsonPointer.forPath(writeContext, false));
					}
				}
			}

			protected void serializeWithJRef(Object value, JsonGenerator gen, SerializationContext ctxt,
					Serializer serializer) {
				// First look for json pointer for instance
				JsonPointer foundPtr = findJsonPointer(value, ctxt);
				if (foundPtr != null) {
					// If found, write out and we're done!
					gen.writeStartObject();
					gen.writeStringProperty(JRefUtil.JREF_NAME, JRefUtil.HASH + foundPtr.toString());
					gen.writeEndObject();
				} else {
					// Call the given serializer to do it's work (with typeref or not)
					serializer.serialize();
					// Add JsonPointer to map if the streamWriteContext has a path segment to
					// contribute
					addJsonPointerIfComplete(value, gen, ctxt);
				}
			}

			@Override
			public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt,
					TypeSerializer typeSer) {
				serializeWithJRef(value, gen, ctxt, () -> super.serializeWithType(value, gen, ctxt, typeSer));
			}

			@Override
			public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
				serializeWithJRef(value, gen, ctxt, () -> super.serialize(value, gen, ctxt));
			}

			@Override
			public ValueSerializer<Object> newDelegatingInstance(ValueSerializer<?> delegatee) {
				return new JRefValueSerializer(delegatee);
			}

		}

		@Override
		public ValueSerializer<?> modifySerializer(SerializationConfig config, Supplier beanDesc,
				ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyArraySerializer(SerializationConfig config, ArrayType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config, CollectionType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyCollectionLikeSerializer(SerializationConfig config,
				CollectionLikeType valueType, Supplier beanDesc, ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, Supplier beanDesc,
				ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyMapLikeSerializer(SerializationConfig config, MapLikeType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyKeySerializer(SerializationConfig config, JavaType valueType, Supplier beanDesc,
				ValueSerializer<?> serializer) {
			return new JRefValueSerializer(serializer);
		}

	}

	public class JRefValueDeserializerModifier extends ValueDeserializerModifier {

		private static final long serialVersionUID = 1L;

		@FunctionalInterface
		interface Deserializer {
			Object deserialize(JsonParser p);
		}

		public class JRefValueDeserializer extends DelegatingDeserializer {

			protected JRefValueDeserializer(ValueDeserializer<?> src) {
				super(src);
			}

			protected class JRefFindResult {
				JsonPointer ptr;
				JsonParser parser;
			}

			protected JRefFindResult findJRef(JsonParser p, DeserializationContext ctxt,
					TypeDeserializer typeDeserializer) {
				JRefFindResult result = new JRefFindResult();
				result.parser = p;
				JsonToken tok = p.currentToken();
				if (tok == JsonToken.START_OBJECT) {
					JsonNode n = ctxt.readTree(p);
					if (n instanceof ObjectNode) {
						JsonNode jrefValue = ((ObjectNode) n).get(JRefUtil.JREF_NAME);
						if (jrefValue != null) {
							// We've found a '$ref' as first object name
							String jrefValueStr = jrefValue.asString();
							if (jrefValueStr != null) {
								// Must start with # (local-only json pointers)
								if (!jrefValueStr.startsWith(JRefUtil.HASH)) {
									throw DatabindException.from(p,
											String.format(
													"JsonPointer value=%s must start with '#' character (local only)",
													jrefValueStr));
								}
								// Remove hash
								jrefValueStr = jrefValueStr.substring(1);
								try {
									// compile JsonPointer
									result.ptr = JsonPointer.valueOf(jrefValueStr);
									// If empty, we throw
									if (result.ptr.equals(JsonPointer.empty())) {
										throw DatabindException.from(p, "JsonPointer value cannot be empty");
									}
								} catch (IllegalArgumentException e) {
									throw DatabindException.from(p,
											String.format("Illegal JsonPointer value=%s", jrefValueStr), e);
								}
							}
						}
					}
					// If we get here, it means we update the parser to create a tree-traversing
					// parser
					result.parser = new TreeTraversingParser(n);
					result.parser.nextToken();
				}
				return result;
			}

			protected Object deserializerWithJRef(JsonParser p, DeserializationContext ctxt,
					TypeDeserializer typeDeserializer, Deserializer deserializer) {
				JRefFindResult findResult = findJRef(p, ctxt, typeDeserializer);
				if (findResult.ptr != null) {
					return findResult.ptr;
				} else {
					return deserializer.deserialize(findResult.parser);
				}
			}

			@Override
			public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
					TypeDeserializer typeDeserializer) throws JacksonException {
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

		@Override
		public ValueDeserializer<?> modifyArrayDeserializer(DeserializationConfig config, ArrayType valueType,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyCollectionLikeDeserializer(DeserializationConfig config,
				CollectionLikeType type, Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, Supplier beanDescRef,
				ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyMapLikeDeserializer(DeserializationConfig config, MapLikeType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return new JRefValueDeserializer(deserializer);
		}

	}

}
