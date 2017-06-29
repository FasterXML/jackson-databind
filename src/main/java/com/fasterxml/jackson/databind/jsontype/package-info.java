/**
 * Package that contains interfaces that define how to implement
 * functionality for dynamically resolving type during deserialization.
 * This is needed for complete handling of polymorphic types, where
 * actual type cannot be determined statically (declared type is
 * a supertype of actual polymorphic serialized types).
 */
package com.fasterxml.jackson.databind.jsontype;
