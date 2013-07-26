/**
 * Classes used for exposing logical structure of POJOs as Jackson
 * sees it, and exposed via
 * {@link com.fasterxml.jackson.databind.ObjectMapper#acceptJsonFormatVisitor(Class, JsonFormatVisitorWrapper)}
 * and
 * {@link com.fasterxml.jackson.databind.ObjectMapper#acceptJsonFormatVisitor(com.fasterxml.jackson.databind.JavaType, JsonFormatVisitorWrapper)}
 * methods.
 *<p>
 * The main entrypoint for code, then, is {@link com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper} and other
 * types are recursively needed during traversal.
 */
package com.fasterxml.jackson.databind.jsonFormatVisitors;
