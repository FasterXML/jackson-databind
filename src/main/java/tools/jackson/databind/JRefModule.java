package tools.jackson.databind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.core.JsonToken;
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

public class JRefModule extends SimpleModule {

	private static final long serialVersionUID = 1L;
	public static final String JREF_NAME = "$ref";
	public static final String HASH = "#";

	public JRefModule() {
		super("JRefModule");
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addDeserializerModifier(new JRefValueDeserializerModifier());
		context.addSerializerModifier(new JRefValueSerializerModifier());
	}

	public class JRefValueSerializerModifier extends ValueSerializerModifier {

		private static final long serialVersionUID = 1L;

		static final String PTR_MAP_ATTR = JRefValueSerializerModifier.class.getName() + ".ptrMap";

		@FunctionalInterface
		interface Serializer {
			void serialize() throws RuntimeException;
		}

		class JRefValueSerializer extends DelegatingSerializer {

			JRefValueSerializer(ValueSerializer<?> delegatee) {
				super(delegatee);
			}

			void jrefSerialize(Object value, JsonGenerator gen, SerializationContext ctxt, Serializer serializer) {
				@SuppressWarnings("unchecked")
				Map<Object, JsonPointer> valueToPtrMap = (Map<Object, JsonPointer>) ctxt.getAttribute(PTR_MAP_ATTR);
				// if it doesn't exist, then create and add as context attribute
				if (valueToPtrMap == null) {
					valueToPtrMap = new HashMap<>();
					ctxt.setAttribute(PTR_MAP_ATTR, valueToPtrMap);
				}
				JsonPointer ptr = valueToPtrMap.get(value);
				if (ptr != null) {
					// If JsonPointer found for value id, write it out and we're done!
					gen.writeStartObject();
					gen.writeStringProperty(JREF_NAME, "#" + ptr.toString());
					gen.writeEndObject();
				} else {
					// serialize the value with delegate
					serializer.serialize();
					// put the object -> ptr into for possible reference usage
					valueToPtrMap.put(value, JsonPointer.forPath(gen.streamWriteContext(), false));
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

		static final String STACK_ATTR = JRefValueDeserializerModifier.class.getName() + ".callStack";
		static final String OBJECT_PTR_MAP_ATTR = JRefValueDeserializerModifier.class.getName() + ".objectPtrMap";

		@FunctionalInterface
		interface Deserializer {
			Object deserialize(JsonParser p) throws RuntimeException;
		}

		class JRefValueDeserializer extends DelegatingDeserializer {

			JRefValueDeserializer(ValueDeserializer<?> src) {
				super(src);
			}

			Object jrefDeserialize(JsonParser p, DeserializationContext ctxt, Deserializer deserializer) {
				@SuppressWarnings("unchecked")
				Deque<JsonPointer> ptrStack = (Deque<JsonPointer>) ctxt.getAttribute(STACK_ATTR);
				if (ptrStack == null) {
					// Create on first access
					ptrStack = new ArrayDeque<>();
					ctxt.setAttribute(STACK_ATTR, ptrStack);
				}
				JsonPointer parentPtr = ptrStack.peek();
				if (parentPtr == null) {
					// use empty
					parentPtr = JsonPointer.empty();
				}
				JsonPointer ctxtPtr = JsonPointer.forPath(p.streamReadContext(), false);
				// build currPtr from context and parent
				JsonPointer currPtr = ctxtPtr.toString().startsWith(parentPtr.toString()) ? ctxtPtr
						: parentPtr.append(ctxtPtr);
				ptrStack.push(currPtr);
				Object result = null;
				if (p.currentToken() == JsonToken.START_OBJECT) {
					JsonNode node = ctxt.readTree(p);
					// Look for "$ref" property
					JsonNode jrefValue = node.asObject().get(JREF_NAME);
					if (jrefValue != null) {
						String pathWithHashExpected = jrefValue.asString();
						// Must start with # (local-only json pointers)
						if (!pathWithHashExpected.startsWith(HASH)) {
							throw DatabindException.from(p,
									String.format("JsonPointer value=%s must start with '#' character (local only)",
											pathWithHashExpected));
						}
						// Remove hash prefix (local only)
						String path = pathWithHashExpected.substring(1);
						try {
							// create JsonPointer from jref path
							JsonPointer pathPtr = JsonPointer.valueOf(path);
							if (pathPtr.equals(JsonPointer.empty())) {
								throw DatabindException.from(p, "JsonPointer value cannot be empty");
							}
							@SuppressWarnings("unchecked")
							Map<JsonPointer, Object> resultsMap = (Map<JsonPointer, Object>) ctxt
									.getAttribute(OBJECT_PTR_MAP_ATTR);
							if (resultsMap != null) {
								// lookup previous result with ptr
								Object previousResult = resultsMap.get(pathPtr);
								if (previousResult == null) {
									throw DatabindException.from(p,
											"Could not find result value for JsonPointer=" + pathPtr);
								}
								// Our result found
								result = previousResult;
							}
						} catch (IllegalArgumentException e) {
							throw DatabindException.from(p, String.format("Illegal JsonPointer path=%s", path), e);
						}
					}
					// If we have not found result via jref, then reset parser to
					// TreeTraversingParser
					if (result == null) {
						p = new TreeTraversingParser(node);
						if (p.currentToken() != JsonToken.END_OBJECT) {
							p.nextToken();
						}
					}
				}
				// call delegate deserializer if no result yet
				if (result == null) {
					// If jref result not found, delegate serialization by calling super class
					result = deserializer.deserialize(p);
					if (result != null) {
						@SuppressWarnings("unchecked")
						Map<JsonPointer, Object> resultsMap = (Map<JsonPointer, Object>) ctxt
								.getAttribute(OBJECT_PTR_MAP_ATTR);
						if (resultsMap == null) {
							resultsMap = new HashMap<>();
							ctxt.setAttribute(OBJECT_PTR_MAP_ATTR, resultsMap);
						}
						resultsMap.put(currPtr, result);
					}
				}
				ptrStack.pollFirst();
				return result;
			}

			@Override
			public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
					TypeDeserializer typeDeserializer) throws JacksonException {
				return jrefDeserialize(p, ctxt, (p1) -> super.deserializeWithType(p1, ctxt, typeDeserializer));
			}

			@Override
			public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
				return jrefDeserialize(p, ctxt, (p1) -> super.deserialize(p1, ctxt));
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
