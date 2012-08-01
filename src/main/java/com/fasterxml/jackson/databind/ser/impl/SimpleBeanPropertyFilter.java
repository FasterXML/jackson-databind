package com.fasterxml.jackson.databind.ser.impl;

import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

/**
 * Simple {@link BeanPropertyFilter} implementation that only uses property name
 * to determine whether to serialize property as is, or to filter it out.
 */
public abstract class SimpleBeanPropertyFilter implements BeanPropertyFilter
{
	/*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
	 */

	protected SimpleBeanPropertyFilter() { }

	/**
	 * Factory method to construct filter that filters out all properties <b>except</b>
	 * ones includes in set
	 */
	public static SimpleBeanPropertyFilter filterOutAllExcept(Set<String> properties) {
		return new FilterExceptFilter(properties);
	}

	public static SimpleBeanPropertyFilter filterOutAllExcept(String... propertyArray) {
		HashSet<String> properties = new HashSet<String>(propertyArray.length);
		Collections.addAll(properties, propertyArray);
		return new FilterExceptFilter(properties);
	}

	public static SimpleBeanPropertyFilter serializeAllExcept(Set<String> properties) {
		return new SerializeExceptFilter(properties);
	}

	public static SimpleBeanPropertyFilter serializeAllExcept(String... propertyArray) {
		HashSet<String> properties = new HashSet<String>(propertyArray.length);
		Collections.addAll(properties, propertyArray);
		return new SerializeExceptFilter(properties);
	}

	/*
    /**********************************************************
    /* Sub-classes
    /**********************************************************
	 */

	/**
	 * Filter implementation which defaults to filtering out unknown
	 * properties and only serializes ones explicitly listed.
	 */
	public static class FilterExceptFilter
	extends SimpleBeanPropertyFilter
	{
		/**
		 * Set of property names to serialize.
		 */
		protected final Set<String> _propertiesToInclude;

		public FilterExceptFilter(Set<String> properties) {
			_propertiesToInclude = properties;
		}

		public void serializeAsField(Object bean, JsonGenerator jgen,
				SerializerProvider provider, BeanPropertyWriter writer) throws Exception
		{
			if (_propertiesToInclude.contains(writer.getName())) {
				writer.serializeAsField(bean, jgen, provider);
			}
		}
		
		public void depositSchemaProperty(BeanPropertyWriter writer,
				ObjectNode propertiesNode, SerializerProvider provider) {
			if (_propertiesToInclude.contains(writer.getName())) {
				BeanSerializerBase.depositSchemaProperty(writer, propertiesNode, provider);
			}
		}
	}

	/**
	 * Filter implementation which defaults to serializing all
	 * properties, except for ones explicitly listed to be filtered out.
	 */
	public static class SerializeExceptFilter
	extends SimpleBeanPropertyFilter
	{
		/**
		 * Set of property names to filter out.
		 */
		protected final Set<String> _propertiesToExclude;

		public SerializeExceptFilter(Set<String> properties) {
			_propertiesToExclude = properties;
		}

		public void serializeAsField(Object bean, JsonGenerator jgen,
				SerializerProvider provider, BeanPropertyWriter writer) throws Exception
		{
			if (!_propertiesToExclude.contains(writer.getName())) {
				writer.serializeAsField(bean, jgen, provider);
			}
		}

		public void depositSchemaProperty(BeanPropertyWriter writer,
				ObjectNode propertiesNode, SerializerProvider provider) {
			if (!_propertiesToExclude.contains(writer.getName())) {
				BeanSerializerBase.depositSchemaProperty(writer, propertiesNode, provider);
			}
		}
	}

}
