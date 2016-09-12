package com.fasterxml.jackson.databind;

import java.io.IOException;

import org.junit.Test;

public class TestJsonMappingException {
	public static class NoSerdeConstructor {
		private String strVal;
		public String getVal() { return strVal; }
		public NoSerdeConstructor( String strVal ) {
			this.strVal = strVal;
		}
	}
	@Test
	public void test() throws IOException {
		ObjectMapper mpr = new ObjectMapper();
		try {
			mpr.readValue( "{ \"val\": \"foo\" }", NoSerdeConstructor.class );
		} catch ( JsonMappingException exc ) {
			mpr.writeValueAsString( exc );
		}
	}
}
