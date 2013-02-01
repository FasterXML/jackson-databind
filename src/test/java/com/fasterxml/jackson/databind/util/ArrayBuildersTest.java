package com.fasterxml.jackson.databind.util;

import org.junit.Assert;
import org.junit.Test;

public class ArrayBuildersTest {

	@Test
	public void testInsertInListNoDup() {
        String [] arr = new String[]{"me", "you", "him"};
        String [] newarr = ArrayBuilders.insertInListNoDup(arr, "you");
        Assert.assertArrayEquals(newarr, arr);

        newarr = ArrayBuilders.insertInListNoDup(arr, "me");
        Assert.assertArrayEquals(newarr, arr);

        newarr = ArrayBuilders.insertInListNoDup(arr, "him");
        Assert.assertArrayEquals(newarr, arr);
	}

}
