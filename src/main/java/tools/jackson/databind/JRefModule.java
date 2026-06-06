package tools.jackson.databind;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.jsontype.TypeDeserializer;
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
import tools.jackson.databind.type.ReferenceType;

public class JRefModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public JRefModule() {
		super("JRef");
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addDeserializerModifier(new JRefValueDeserializerModifier());
		context.addSerializerModifier(new JRefValueSerializerModifier());
	}

	public static abstract class JRefNamedValueSerializer extends DelegatingSerializer {

		String localName;
		
		protected JRefNamedValueSerializer(ValueSerializer<?> delegatee) {
			super(delegatee);
		}

		protected JRefNamedValueSerializer(ValueSerializer<?> delegatee, String name) {
			super(delegatee);
			this.localName = name;
		}
		
		abstract ValueSerializer<Object> newDelegatingNamedInstance(ValueSerializer<?> delegatee, String name);
			
		@Override
		protected ValueSerializer<Object> newDelegatingInstance(ValueSerializer<?> newDelegatee) {
			return newDelegatingNamedInstance(newDelegatee, localName);
		}
		
		static Map<Integer, AbstractNode> nodes = new HashMap<Integer, AbstractNode>();
		static Deque<AbstractNode> serializationStack = new ArrayDeque<>();
		
		static Map<Integer, JsonPointer> ptrs = new HashMap<Integer, JsonPointer>();
		
		class AbstractNode {
			AbstractNode parent;
			Integer objectId;
			String name;
			String location;
			
			AbstractNode(AbstractNode parent, Object object, String name) {
				this.parent = parent;
				this.objectId = System.identityHashCode(object);
				this.name = name;
				nodes.put(objectId, this);	
			}
			
			AbstractNode(AbstractNode parent, Object object) {
				this(parent, object, null);
			}
			
			String buildFullPath() {
				if (parent != null) {
					String parentName = parent.buildFullPath();
					if (this.name == null) {
						return JRefUtil.escape(parentName);
					} else {
						return JRefUtil.append(this.name, parentName);
					}
				} else {
					return "";
				}
			}
			
			Optional<String> getLocation() {
				if (this.location == null) {
					return Optional.empty();
				}
				return Optional.of(this.location);
			}
			
			void addToSerializationStack() {
				serializationStack.addFirst(this);
			}
			
			void removeFromSerializationStack() {
				serializationStack.pop();
			}

			void cachePathLocation() {
				if (this.location == null) {
					this.location = buildFullPath();
				}
			}
		}
		
		class POJONode extends AbstractNode {
			POJONode(AbstractNode parent, Object object, String name) {
				super(parent, object, name);
			}
		}
		
		class ArrayElementNode extends AbstractNode {
			ArrayElementNode(AbstractNode parent, Object value, int elementIndex) {
				super(parent, value, String.valueOf(elementIndex));
			}
		}

		class CollectionNode extends AbstractNode {
			public CollectionNode(AbstractNode parent, Collection<?> c, String name) {
				super(parent, c, name);
				Iterator<?> iter = c.iterator();
				int i = 0;
				do {
					// Create and add arrayelement nodes for each element
					new ArrayElementNode(this, iter.next(), i);
					i++;
				} while (iter.hasNext());
			}
		}
			
		class MapNode extends AbstractNode {

			Map<?,?> map;
			HashMap<Object,String> keyNames;
			
			MapNode(AbstractNode parent, Map<?,?> map, String name) {
				super(parent, map, name);
				this.map = map;
				this.keyNames = new HashMap<Object,String>(map.keySet().size());
			}
			
			void setKeyName(Object k, String name) {
				// The key and it's name are the same
				if (k == name) {
					keyNames.put(k,name);
				} else {
					for(Object key: map.keySet()) {
						if (key == k) {
							keyNames.put(key, name);
						}
					}
				}
			}
			
			String getNameForValue(Object val) {
				for(Map.Entry<?,?> e: map.entrySet()) {
					if (e.getValue() == val) {
						return keyNames.get(e.getKey());
					}
				}
				return null;
			}
		}
		
		protected AbstractNode currentParent() {
			return serializationStack.peek();
		}
		
		protected AbstractNode findNode(Object value) {
			return nodes.get(System.identityHashCode(value));
		}
		
		MapNode createMapNode(Map<?,?> instance) {
			return new MapNode(serializationStack.peek(), instance, localName);
		}
		
		@SuppressWarnings("unchecked")
		AbstractNode createNodeByValueType(AbstractNode parent, Object value, String name) {
			Objects.requireNonNull(value, "Value must not be null");
			Objects.requireNonNull(name, "Name must not be null");
			AbstractNode result = null;
			if (value instanceof Collection<?>) {
				result = new CollectionNode(parent, (Collection<?>) value, name);
			} else if (value instanceof Map<?,?>) {
				result = new MapNode(parent, (Map<Object,Object>) value, name);
			} else {
				result = new POJONode(parent, value, name);
			}
			return result;
		}

		void trace(String method, JRefNamedValueSerializer vds) {
			System.out.println(new StringBuffer(method).append(".").append(vds.toString().toString()));
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName()+"[type=" + handledType() + "]";
		}

		void trace(String method) {
			trace(method, this);
		}

		@Override
		public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
			ValueSerializer<Object> s = ctxt.handleSecondaryContextualization(getDelegatee(), property);
			if (property != null) {
				return newDelegatingNamedInstance(s, property.getFullName().getName());
			} else {
				return this;
			}
		}
		
		void serializeWithNode(AbstractNode node, Object value, JsonGenerator gen, SerializationContext ctxt) {
			// Setup on serialization stack during call to serialize value
			node.addToSerializationStack();
			super.serialize(value, gen, ctxt);
			TokenStreamContext swc = gen.streamWriteContext();
			if (swc.hasPathSegment()) {
				JsonPointer ptr = JsonPointer.forPath(swc, false);
				ptrs.put(System.identityHashCode(value), ptr);
				System.out.println("hasPathSegment="+ptr);
			}
			
			// Immediately remove from stack
			node.removeFromSerializationStack();
			// Get current parent
			AbstractNode parent = currentParent();
			// If parent is mapnode, we get the name for the value just serialized
			if (parent instanceof MapNode) {
				// This name is setup in the keyserializer, which is processed
				// before this above value serialization
				node.name = (((MapNode) parent).getNameForValue(value));
			} 
			// cache the location for subsequent usage, now that the value has been serialized
			node.cachePathLocation();
			trace("jrefPath="+node.getLocation().get());
		}
	}

	protected class JRefValueSerializerModifier extends ValueSerializerModifier {

		private static final long serialVersionUID = 1L;

		ValueSerializer<?> newJRefValueSerializer(ValueSerializer<?> d) {
			return new JRefValueSerializer(d);
		}

		@Override
		public ValueSerializer<?> modifySerializer(SerializationConfig config, Supplier beanDesc,
				ValueSerializer<?> serializer) {
			return newJRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyArraySerializer(SerializationConfig config, ArrayType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return newJRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config, CollectionType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return newJRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyCollectionLikeSerializer(SerializationConfig config,
				CollectionLikeType valueType, Supplier beanDesc, ValueSerializer<?> serializer) {
			return newJRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, Supplier beanDesc,
				ValueSerializer<?> serializer) {
			return new JRefMapValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyMapLikeSerializer(SerializationConfig config, MapLikeType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return newJRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType,
				Supplier beanDesc, ValueSerializer<?> serializer) {
			return newJRefValueSerializer(serializer);
		}

		@Override
		public ValueSerializer<?> modifyKeySerializer(SerializationConfig config, JavaType valueType, Supplier beanDesc,
				ValueSerializer<?> serializer) {
			return new JRefKeySerializer(serializer);
		}
		
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
	
	public static class JRefMapValueSerializer extends JRefNamedValueSerializer {

		protected JRefMapValueSerializer(ValueSerializer<?> delegatee) {
			super(delegatee);
		}

		protected JRefMapValueSerializer(ValueSerializer<?> delegatee, String name) {
			super(delegatee, name);
		}

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
			
			JsonPointer ptr = ptrs.get(System.identityHashCode(value));
			if (ptr != null) {
				System.out.println("Found ptr="+ptr+" to value="+value);
			}
			// First thing we do is look for map node
			MapNode node = (MapNode) findNode(value);
			if (node != null) {
				Optional<String> loc = node.getLocation();
				if (loc.isPresent()) {
					// If so, we've got a completed path and we serialize it as a $ref object
					gen.writeStartObject();
					gen.writeStringProperty(JRefPath.JREF_REF, JRefUtil.HASH + loc.get());
					gen.writeEndObject();					
				}
			} else {
				// First thing is we create new map node
				node = createMapNode((Map<?, ?>) value);
				serializeWithNode(node, value, gen, ctxt);
			}
		}
		
		@Override
		ValueSerializer<Object> newDelegatingNamedInstance(ValueSerializer<?> delegatee, String name) {
			return new JRefMapValueSerializer(delegatee, name);
		}
		
	}
	
	public static class JRefKeySerializer extends JRefNamedValueSerializer {

		public JRefKeySerializer(ValueSerializer<?> delegatee, String name) {
			super(delegatee, name);
		}

		public JRefKeySerializer(ValueSerializer<?> delegatee) {
			super(delegatee);
		}

		@Override
		public void serialize(Object keyValue, JsonGenerator gen, SerializationContext ctxt) {
			super.serialize(keyValue, gen, ctxt);
			((MapNode) currentParent()).setKeyName(keyValue, keyValue.toString());
		}
		
		@Override
		ValueSerializer<Object> newDelegatingNamedInstance(ValueSerializer<?> delegatee, String name) {
			return new JRefKeySerializer(delegatee, name);
		}
		
	}
	
	public static class JRefValueSerializer extends JRefNamedValueSerializer {

		JRefValueSerializer(ValueSerializer<?> delegatee) {
			super(delegatee);
		}
		
		JRefValueSerializer(ValueSerializer<?> delegatee, String name) {
			super(delegatee, name);
		}

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
			JsonPointer ptr = ptrs.get(System.identityHashCode(value));
			if (ptr != null) {
				System.out.println("Found ptr="+ptr+" to value="+value);
			}
			AbstractNode parent = currentParent();
			AbstractNode current = null;
			if (parent != null) {
				current = findNode(value);
				if (current == null) {
					if (parent instanceof MapNode) {
						current = createNodeByValueType(parent, value, "");
					} else {
						current = createNodeByValueType(parent, value, this.localName);
					}
				} else {
					Optional<String> location = current.getLocation();
					if (location.isPresent()) {
						// If so, we've got a completed path and we serialize it as a $ref object
						gen.writeStartObject();
						gen.writeStringProperty(JRefPath.JREF_REF, JRefUtil.HASH + location.get());
						gen.writeEndObject();
						trace("found jref="+location.get()+",value="+value);
						return;
					} else {
						if (parent instanceof MapNode) {
							current = createNodeByValueType(parent, value, "");
						} else if (parent instanceof ArrayElementNode) {
							current = createNodeByValueType(parent, value, "");
						}
					}
				}
			} else {
				current = createNodeByValueType(parent, value, "");
			}
			serializeWithNode(current, value, gen, ctxt);					
		}
		
		@Override
		ValueSerializer<Object> newDelegatingNamedInstance(ValueSerializer<?> delegatee, String name) {
			return new JRefValueSerializer(delegatee, name);
		}
		
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
