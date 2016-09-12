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
	// we know we can't serialize this dumb class above
	@Test( expected = JsonMappingException.class )
	public void testNoSerdeConstructor() throws IOException {
		ObjectMapper mpr = new ObjectMapper();
		mpr.readValue( "{ \"val\": \"foo\" }", NoSerdeConstructor.class );
	}
	// we should, however, be able to serialize the exception arising from the above test
	@Test
	public void testJsonMappingExceptionIsJacksonSerializable() throws IOException {
		ObjectMapper mpr = new ObjectMapper();
		try {
			testNoSerdeConstructor();
		} catch ( JsonMappingException exc ) {
			// without @JsonIgnore on getProcessor() this causes an error
			mpr.writeValueAsString( exc );
		}
	}
}
