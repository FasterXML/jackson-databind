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

		class JRefReader {

			JsonParser parser;
			Object result;
			
			JRefReader(JsonParser p, DeserializationContext ctxt) {
				this.parser = p;
				@SuppressWarnings("unchecked")
				Map<JsonPointer,Object> results = (Map<JsonPointer,Object>) ctxt.getAttribute("jrefResults");
				if (p.currentToken() == JsonToken.START_OBJECT) {
					// Read the whole node
					JsonNode node = ctxt.readTree(p);
					// Look for "$ref"
					JsonNode jrefValue = node.asObject().get(JRefUtil.JREF_NAME);
					if (jrefValue != null) {
						// Check for hash and strip
						String path = JRefUtil.checkHashAndStrip(p, jrefValue.asString());
						try {
							// compile JsonPointer
							JsonPointer ptr = JsonPointer.valueOf(path);
							// If empty, we throw
							if (ptr.equals(JsonPointer.empty())) {
								throw DatabindException.from(p, "JsonPointer value cannot be empty");
							}
							// Now lookup in results
							Object result = results.get(ptr);
							// We should find it, if not, throw
							if (result == null) {
								// should throw here
								throw DatabindException.from(p, "Could not find result value for JsonPointer="+ptr);
							}
							this.result = result;
						} catch (IllegalArgumentException e) {
							throw DatabindException.from(p, String.format("Illegal JsonPointer path=%s", path),
									e);
						}
					}
					if (this.result == null) {
						this.parser = new TreeTraversingParser(node);
						if (this.parser.currentToken() != JsonToken.END_OBJECT) {
							this.parser.nextToken();
						}
					}
				}
			}
		}

	
		public static class JRefJsonPointer extends JsonPointer {
			
		    final static JRefJsonPointer EMPTY = new JRefJsonPointer();

			// These constructors are all duplicated from JsonPointer 
			// so that they can be used in JJsonPointer.fromPath
		    JRefJsonPointer() {
				super();
			}

			JRefJsonPointer(String fullString, int fullStringOffset, String segment, int matchIndex,
					JsonPointer next) {
				super(fullString, fullStringOffset, segment, matchIndex, next);
			}

			JRefJsonPointer(String fullString, int fullStringOffset, String segment, JsonPointer next) {
				super(fullString, fullStringOffset, segment, next);
			}

			// Duplicated (no changes) from JsonPointer.PointerSegment so it can be used
			// in JJsonPointer.fromPath
			private static class PointerSegment {
		        public final PointerSegment next;
		        public final String property;
		        public final int index;

		        // Offset within external buffer, updated when constructing
		        public int pathOffset;

		        // And we actually need 2-way traversal, it turns out so:
		        public PointerSegment prev;

		        public PointerSegment(PointerSegment next, String pn, int ix) {
		            this.next = next;
		            property = pn;
		            index = ix;
		            // Ok not the cleanest thing but...
		            if (next != null) {
		                next.prev = this;
		            }
		        }
		    }
			// Duplicated (no changes) from JsonPointer._appendEscapted so it can be used
			// in JJsonPointer.fromPath
		    private static void _appendEscaped(StringBuilder sb, String segment)
		    {
		        for (int i = 0, end = segment.length(); i < end; ++i) {
		            char c = segment.charAt(i);
		            if (c == SEPARATOR) {
		                sb.append(ESC_SLASH);
		                continue;
		            }
		            if (c == ESC) {
		                sb.append(ESC_TILDE);
		                continue;
		            }
		            sb.append(c);
		        }
		    }

			// Duplicated *with changes* from JsonPointer.PointerSegment so it can be used
			// by JJsonPointers instead of supercliass
		    @Override
		    public JsonPointer last() {
		        JRefJsonPointer current = this;
		        if (current == EMPTY) {
		            return null;
		        }
		        JRefJsonPointer next;
		        while ((next = (JRefJsonPointer) current._nextSegment) != JsonPointer.EMPTY) {
		        	// XXX HERE IS DIFFERENCE FROM JsonPointer.last()
		        	// without this statement, an NPE is thrown as if next is null,
		            if (next == null) {
		            	break;
		            }
		        	// when current = next is executred the while ((next = current._nextSegment
	            	current = next;
		        }
		        return current;
		    }

			private static final long serialVersionUID = 1L;

			public static JsonPointer fromPath(JsonPointer parent, TokenStreamContext context) {
				// Jref:  There are three reasons for this implementation
				// 1) The use PointerSegment class (private static in JsonPointer.PointerSegment does
				//    not allow subclasses to use/access
				// 2) The use of JJsonPointer constructors, to allow override of public JsonPointer.last()
				//    method (NPE in JsonPointer.last()
				// 3) The addition of a JsonPointer parent argument
				
				// This is all the same as JsonPointer.fromPath
		        // First things first: last segment may be for START_ARRAY/START_OBJECT,
		        // in which case it does not yet point to anything, and should be skipped
		        if (context == null) {
		            return EMPTY;
		        }
		        // Otherwise if context was just created but is not advanced -- like,
		        // opening START_ARRAY/START_OBJECT returned -- drop the empty context.
		        if (!context.hasPathSegment()) {
		            // Except one special case: do not prune root if we need it
		            if (!(context.inRoot() && context.hasCurrentIndex())) {
		                context = context.getParent();
		            }
		        }

		        PointerSegment next = null;
		        int approxLength = 0;

		        for (; context != null; context = context.getParent()) {
		            if (context.inObject()) {
		                String propName = context.currentName();
		                if (propName == null) { // is this legal?
		                    propName = "";
		                }
		                approxLength += 2 + propName.length();
		                // This is where PointerSegment instances are created
		                next = new PointerSegment(next, propName, -1);
		            } else if (context.inArray()) {
		                int ix = context.getCurrentIndex();
		                approxLength += 6;
		                // This is where PointerSegment instances are created
		                next = new PointerSegment(next, null, ix);
		            }
		            // NOTE: this effectively drops ROOT node(s); should have 1 such node,
		            // as the last one, but we don't have to care (probably some paths have
		            // no root, for example)
		        }
		        if (next == null) {
		            return EMPTY;
		        }

		        // And here the fun starts! We have the head, need to traverse
		        // to compose full path String
		        StringBuilder pathBuilder = new StringBuilder(approxLength);
		        PointerSegment last = null;

		        for (; next != null; next = next.next) {
		            // Let's find the last segment as well, for reverse traversal
		            last = next;
		            next.pathOffset = pathBuilder.length();
		            pathBuilder.append(SEPARATOR);
		            if (next.property != null) {
		                _appendEscaped(pathBuilder, next.property);
		            } else {
		                pathBuilder.append(next.index);
		            }
		        }
		        final String fullPath = pathBuilder.toString();

		        // and then iteratively construct JsonPointer chain in reverse direction
		        // (from innermost back to outermost)
		        PointerSegment currSegment = last;
		        JsonPointer currPtr = EMPTY;

		        for (; currSegment != null; currSegment = currSegment.prev) {
		            if (currSegment.property != null) {
		                currPtr = new JRefJsonPointer(fullPath, currSegment.pathOffset,
		                      currSegment.property, currPtr);
		            } else {
		                int index = currSegment.index;
		                currPtr = new JRefJsonPointer(fullPath, currSegment.pathOffset,
		                        String.valueOf(index), index, currPtr);
		            }
		        }
		        
		        // This is the code that uses the parent JsonPointer
		        if (!parent.equals(EMPTY)) {
				    JsonPointer parentMatch = currPtr.matchProperty(parent.getMatchingProperty());
				    if (parentMatch != null && !JsonPointer.EMPTY.equals(parentMatch)) {
				    	currPtr = parent.append(parentMatch);
				    } else {
				    	parentMatch = currPtr.matchElement(currPtr.getMatchingIndex());
				    	if (parentMatch != null) {
				    		currPtr = parent.append(parentMatch);
				    	} else {
				    		currPtr = parent.append(currPtr);
				    	}
				    }
		        }
	        	return currPtr;
			}
		}
		
		public class JRefValueDeserializer extends DelegatingDeserializer {

			protected JRefValueDeserializer(ValueDeserializer<?> src) {
				super(src);
			}

			@Override
			public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
					TypeDeserializer typeDeserializer) throws JacksonException {
				var callStack = getCallStack(ctxt);
				JsonPointer ptr = JRefJsonPointer.fromPath(callStack.peek(), p.streamReadContext());
				System.out.println("ptr="+ptr);
				callStack.push(ptr);
				Object result = null;
				// Look for JRef and associated result
				JRefReader jref = new JRefReader(p, ctxt);
				if (jref.result != null) {
					result = jref.result;
				} else {
					// If jref result not found, delegate serialization by calling super class
					result = super.deserialize(jref.parser, ctxt, typeDeserializer);
				}
				// Once we have a result, put it in resultsMap
				getResultsMap(ctxt).put(ptr, result);
				// Pop from callStack before returning
				callStack.pollFirst();
				return result;
			}

			Deque<JsonPointer> getCallStack(DeserializationContext ctxt) {
				@SuppressWarnings("unchecked")
				Deque<JsonPointer> stack = (Deque<JsonPointer>) ctxt.getAttribute("jrefStack");
				// Create stack on first usage
				if (stack == null) {
					stack = new ArrayDeque<>();
					ctxt.setAttribute("jrefStack", stack);
				}
				return stack;
			}
			
			Map<JsonPointer,Object> getResultsMap(DeserializationContext ctxt) {
				@SuppressWarnings("unchecked")
				Map<JsonPointer,Object> results = (Map<JsonPointer,Object>) ctxt.getAttribute("jrefResults");
				if (results == null) {
					results = new HashMap<>();
					ctxt.setAttribute("jrefResults", results);
				}
				return results;
			}
			
			@Override
			public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
				var callStack = getCallStack(ctxt);
				JsonPointer ptr = JRefJsonPointer.fromPath(callStack.peek(), p.streamReadContext());
				System.out.println("ptr="+ptr);
				callStack.push(ptr);
				Object result = null;
				// Look for JRef and associated result
				JRefReader jref = new JRefReader(p, ctxt);
				if (jref.result != null) {
					result = jref.result;
				} else {
					// If jref result not found, delegate serialization by calling super class
					result = super.deserialize(jref.parser, ctxt);
				}
				// Once we have a result, put it in resultsMap
				getResultsMap(ctxt).put(ptr, result);
				// Pop from callStack before returning
				callStack.pollFirst();
				return result;
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
