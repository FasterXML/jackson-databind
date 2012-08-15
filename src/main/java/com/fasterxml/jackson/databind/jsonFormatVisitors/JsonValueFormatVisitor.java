package com.fasterxml.jackson.databind.jsonFormatVisitors;

import java.util.Set;

public interface JsonValueFormatVisitor {

	void format(JsonValueFormat format);

	void enumTypes(Set<String> enums);
}
