package com.fasterxml.jackson.databind.jsonschema.visitors;

import java.util.Set;

import com.fasterxml.jackson.databind.jsonschema.types.JsonValueFormat;

public interface JsonValueFormatVisitor {

	void format(JsonValueFormat format);

	void enumTypes(Set<String> enums);
}
