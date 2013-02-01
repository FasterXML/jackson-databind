package com.fasterxml.jackson.databind.util;

import org.junit.Assert;
import org.junit.Test;

public class ArrayBuildersTest {

	@Test
	public void testInsertInListNoDup() {
		String [] arr = new String[]{"me", "you", "him"};
		ArrayBuilders.insertInListNoDup(arr, "you");
		Assert.assertArrayEquals(arr, new String[]{"me", "you", "him"});
	}

}
