package com.fasterxml.jackson.databind.jsonschema.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SchemaType {
	
	STRING,
	NUMBER,
	INTEGER,
	BOOLEAN,
	OBJECT,
	ARRAY,
	NULL,
	ANY;
	
	
	@JsonValue
	public String value() {
		return this.name().toLowerCase();
	}
	
	@JsonCreator
	public static SchemaType forValue(String s) {
		return valueOf(s.toUpperCase());
	}
}