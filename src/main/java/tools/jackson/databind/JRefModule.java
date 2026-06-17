package tools.jackson.databind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
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

			Map<Integer, JsonPointer> getObjectToPtrMap(SerializationContext ctxt) {
				@SuppressWarnings("unchecked")
				Map<Integer, JsonPointer> map = (Map<Integer, JsonPointer>) ctxt.getAttribute(PTR_MAP_ATTR);
				// if it doesn't exist, then create and add as context attribute
				if (map == null) {
					map = new ConcurrentHashMap<>();
					ctxt.setAttribute(PTR_MAP_ATTR, map);
				}
				return map;
			}

			JsonPointer findJsonPointer(Object value, SerializationContext ctxt) {
				if (ClassUtil.primitiveType(value.getClass()) != null) {
					return null;
				}
				return getObjectToPtrMap(ctxt).get(System.identityHashCode(value));
			}

			void checkAndSetJsonPointer(Object value, JsonGenerator gen, SerializationContext ctxt) {
				if (ClassUtil.primitiveType(value.getClass()) != null) {
					return;
				}
				TokenStreamContext swc = gen.streamWriteContext();
				if (swc.hasPathSegment()) {
					getObjectToPtrMap(ctxt).put(System.identityHashCode(value), JsonPointer.forPath(swc, false));
				}
			}

			void jrefSerialize(Object value, JsonGenerator gen, SerializationContext ctxt, Serializer serializer) {
				JsonPointer ptr = findJsonPointer(value, ctxt);
				if (ptr != null) {
					// If JsonPointer found for value id, write it out and we're done!
					gen.writeStartObject();
					gen.writeStringProperty(JREF_NAME, "#" + ptr.toString());
					gen.writeEndObject();
				} else {
					// No JsonPointer, so serialize value
					serializer.serialize();
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
		
		static final String STACK_ATTR = JRefValueDeserializerModifier.class.getName() + ".callStack";
		static final String OBJECT_PTR_MAP_ATTR = JRefValueSerializerModifier.class.getName() + ".objectPtrMap";

		@FunctionalInterface
		interface Deserializer {
			Object deserialize(JsonParser p) throws RuntimeException;
		}

		class JRefValueDeserializer extends DelegatingDeserializer {

			JRefValueDeserializer(ValueDeserializer<?> src) {
				super(src);
			}

			JsonPointer buildJsonPointer(JsonPointer parentPtr, TokenStreamContext context) {
				JsonPointer currPtr = JsonPointer.forPath(context, false);
				// Uses the currPtr and the parentPtr to build new JsonPointer
				if (parentPtr != null && !parentPtr.equals(JsonPointer.empty())) {
					JsonPointer parentMatch = currPtr.matchProperty(parentPtr.getMatchingProperty());
					if (parentMatch != null && !JsonPointer.empty().equals(parentMatch)) {
						currPtr = parentPtr.append(parentMatch);
					} else {
						parentMatch = currPtr.matchElement(currPtr.getMatchingIndex());
						if (parentMatch != null) {
							currPtr = parentPtr.append(parentMatch);
						} else {
							currPtr = parentPtr.append(currPtr);
						}
					}
				}
				return currPtr;
			}

			Deque<JsonPointer> getCallStack(DeserializationContext ctxt) {
				@SuppressWarnings("unchecked")
				Deque<JsonPointer> stack = (Deque<JsonPointer>) ctxt.getAttribute(STACK_ATTR);
				// Create stack on first usage
				if (stack == null) {
					stack = new ArrayDeque<>();
					ctxt.setAttribute(STACK_ATTR, stack);
				}
				return stack;
			}

			Map<JsonPointer, Object> getResultsMap(DeserializationContext ctxt) {
				@SuppressWarnings("unchecked")
				Map<JsonPointer, Object> results = (Map<JsonPointer, Object>) ctxt.getAttribute(OBJECT_PTR_MAP_ATTR);
				if (results == null) {
					results = new HashMap<>();
					ctxt.setAttribute(OBJECT_PTR_MAP_ATTR, results);
				}
				return results;
			}

			Object jrefDeserialize(JsonParser p, DeserializationContext ctxt, Deserializer deserializer) {
				var callStack = getCallStack(ctxt);
				// Build JsonPointer
				JsonPointer currPtr = buildJsonPointer(callStack.peek(), p.streamReadContext());
				callStack.push(currPtr);
				Object result = null;
				if (p.currentToken() == JsonToken.START_OBJECT) {
					JsonNode node = ctxt.readTree(p);
					// Look for "$ref" property
					JsonNode jrefValue = node.asObject().get(JREF_NAME);
					if (jrefValue != null) {
						String pathWithHashExpected = jrefValue.asString();
						// Must start with # (local-only json pointers)
						if (!pathWithHashExpected.startsWith(HASH)) {
							// throw if it doesn't have hash
							throw DatabindException.from(p, String.format(
									"JsonPointer value=%s must start with '#' character (local only)", pathWithHashExpected));
						}
						// Remove hash
						String path = pathWithHashExpected.substring(1);
						try {
							// create JsonPointer from path
							JsonPointer ptr = JsonPointer.valueOf(path);
							// throw if empty/not well-formed
							if (ptr.equals(JsonPointer.empty())) {
								throw DatabindException.from(p, "JsonPointer value cannot be empty");
							}
							// Now lookup in results
							Object previousResult = getResultsMap(ctxt).get(ptr);
							// If not found, throw
							if (previousResult == null) {
								throw DatabindException.from(p, "Could not find result value for JsonPointer=" + ptr);
							}
							// else we are done
							result = previousResult;
						} catch (IllegalArgumentException e) {
							throw DatabindException.from(p, String.format("Illegal JsonPointer path=%s", path), e);
						}
					}
					// If we have not found result via jref, then reset parser to TreeTraversingParser
					if (result == null) {
						p = new TreeTraversingParser(node);
						if (p.currentToken() != JsonToken.END_OBJECT) {
							p.nextToken();
						}
					}
				} 
				// Only call deserializr if no result to this point
				if (result == null) {
					// If jref result not found, delegate serialization by calling super class
					result = deserializer.deserialize(p);
					// Once we have a result, put it in resultsMap
					getResultsMap(ctxt).put(currPtr, result);
				}
				// Pop from callStack before returning result
				callStack.pollFirst();
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
