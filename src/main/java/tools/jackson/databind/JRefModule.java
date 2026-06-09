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
import tools.jackson.databind.node.TreeTraversingParser;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.std.DelegatingSerializer;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.util.ClassUtil;

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

			protected Map<Integer, JsonPointer> getMap(SerializationContext ctxt) {
				@SuppressWarnings("unchecked")
				Map<Integer, JsonPointer> map = (Map<Integer, JsonPointer>) ctxt.getAttribute(PTR_MAP_ATTR);
				// if it doesn't exist, then create and add as context attribute
				if (map == null) {
					map = new ConcurrentHashMap<>();
					ctxt.setAttribute(PTR_MAP_ATTR, map);
				}
				return map;
			}

			protected JsonPointer findJsonPointer(Object value, SerializationContext ctxt) {
				if (ClassUtil.primitiveType(value.getClass()) != null) {
					return null;
				}
				return getMap(ctxt).get(System.identityHashCode(value));
			}

			protected void checkAndSetJsonPointer(Object value, JsonGenerator gen, SerializationContext ctxt) {
				if (ClassUtil.primitiveType(value.getClass()) != null) {
					return;
				}
				// Get TokenStreamContext
				TokenStreamContext swc = gen.streamWriteContext();
				if (swc.hasPathSegment()) {
					getMap(ctxt).put(System.identityHashCode(value), JsonPointer.forPath(swc, false));
				}
			}

			protected void jrefSerialize(Object value, JsonGenerator gen, SerializationContext ctxt,
					Serializer serializer) {
				// do lookup first
				JsonPointer ptr = findJsonPointer(value, ctxt);
				if (ptr != null) {
					// If JsonPointer found for value id, write it out and we're done!
					gen.writeStartObject();
					gen.writeStringProperty(JRefUtil.JREF_NAME, JRefUtil.HASH + ptr.toString());
					gen.writeEndObject();
				} else {
					// Needs to serialize value, so call the serializer
					serializer.serialize();
					// Then check and set JsonPointer before returning
					checkAndSetJsonPointer(value, gen, ctxt);
				}
			}

			@Override
			public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt,
					TypeSerializer typeSer) {
				jrefSerialize(value, gen, ctxt, () -> super.serializeWithType(value, gen, ctxt, typeSer));
			}

			@Override
			public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
				jrefSerialize(value, gen, ctxt, () -> super.serialize(value, gen, ctxt));
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
	}

	public class JRefValueDeserializerModifier extends ValueDeserializerModifier {

		private static final long serialVersionUID = 1L;

		@FunctionalInterface
		interface Deserializer {
			Object deserialize(JsonParser p);
		}

		class ObjectNodeTraversingParser extends TreeTraversingParser {

			public ObjectNodeTraversingParser(JsonNode n) {
				super(n);
				if (currentToken() != JsonToken.END_OBJECT) {
					nextToken();
				}
			}
		}

		public class JRefValueDeserializer extends DelegatingDeserializer {

			protected JRefValueDeserializer(ValueDeserializer<?> src) {
				super(src);
			}

			protected Object deserializerWithJRef(JsonParser p, DeserializationContext ctxt,
					Deserializer deserializer) {
				Object result = null;
				// Only objects have potential to be JsonPointers
				if (p.currentToken() == JsonToken.START_OBJECT) {
					JsonNode node = ctxt.readTree(p);
					// Look for "$ref"
					JsonNode jrefValue = node.asObject().get(JRefUtil.JREF_NAME);
					if (jrefValue != null) {
						// If found, convert to string
						String jrefValueStr = jrefValue.asString();
						// Must start with # (local-only json pointers)
						if (!jrefValueStr.startsWith(JRefUtil.HASH)) {
							// throw if it doesn't have hash
							throw DatabindException.from(p, String.format(
									"JsonPointer value=%s must start with '#' character (local only)", jrefValueStr));
						}
						// Remove hash
						jrefValueStr = jrefValueStr.substring(1);
						try {
							// compile JsonPointer
							result = JsonPointer.valueOf(jrefValueStr);
							// If empty, we throw
							if (result.equals(JsonPointer.empty())) {
								throw DatabindException.from(p, "JsonPointer value cannot be empty");
							}
						} catch (IllegalArgumentException e) {
							throw DatabindException.from(p, String.format("Illegal JsonPointer value=%s", jrefValueStr),
									e);
						}
					}
					// If JsonPointer result not found/set above, aka result == null
					if (result == null) {
						// pass along to ObjectNodeTraversingParser
						p = new ObjectNodeTraversingParser(node);
					}
				}
				// If JsonPointer as result, return else deserialize with updated parser
				return (result != null) ? result : deserializer.deserialize(p);
			}

			@Override
			public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
					TypeDeserializer typeDeserializer) throws JacksonException {
				return deserializerWithJRef(p, ctxt, ps -> super.deserializeWithType(ps, ctxt, typeDeserializer));
			}

			@Override
			public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
				return deserializerWithJRef(p, ctxt, ps -> super.deserialize(ps, ctxt));
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
