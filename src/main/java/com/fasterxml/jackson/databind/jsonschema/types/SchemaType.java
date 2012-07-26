package com.fasterxml.jackson.databind.jsonschema.types;

public enum SchemaType {
	STRING {
		@Override
		public String toString() { return "string"; }
	},
	NUMBER {
		@Override
		public String toString() { return "number"; }
	},
	INTEGER {
		@Override
		public String toString() { return "integer"; }
	},
	BOOLEAN {
		@Override
		public String toString() { return "boolean"; }
	},
	OBJECT {
		@Override
		public String toString() { return "object"; }
	},
	ARRAY {
		@Override
		public String toString() { return "array"; }
	},
	NULL {
		@Override
		public String toString() { return "null"; }
	},
	ANY {
		@Override
		public String toString() { return "any"; }
	}
	
}