package com.fasterxml.jackson.databind.util;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.util.ArrayBuilders.BooleanBuilder;
import com.fasterxml.jackson.databind.util.ArrayBuilders.ByteBuilder;
import com.fasterxml.jackson.databind.util.ArrayBuilders.DoubleBuilder;
import com.fasterxml.jackson.databind.util.ArrayBuilders.FloatBuilder;
import com.fasterxml.jackson.databind.util.ArrayBuilders.IntBuilder;
import com.fasterxml.jackson.databind.util.ArrayBuilders.LongBuilder;
import com.fasterxml.jackson.databind.util.ArrayBuilders.ShortBuilder;

public class ArrayBuildersTest extends BaseMapTest
{
	// [databind#157]
	public void testInsertInListNoDup()
	{
        String [] arr = new String[]{"me", "you", "him"};
        String [] newarr;

        newarr = ArrayBuilders.insertInListNoDup(arr, "you");
        Assert.assertArrayEquals(new String[]{"you", "me", "him"}, newarr);

        newarr = ArrayBuilders.insertInListNoDup(arr, "me");
        Assert.assertArrayEquals(new String[]{"me", "you","him"}, newarr);

        newarr = ArrayBuilders.insertInListNoDup(arr, "him");
        Assert.assertArrayEquals(new String[]{"him", "me", "you"}, newarr);

        newarr = ArrayBuilders.insertInListNoDup(arr, "foobar");
        Assert.assertArrayEquals(new String[]{"foobar", "me", "you", "him"}, newarr);
	}

     public void testBuilderAccess()
     {
         ArrayBuilders builders = new ArrayBuilders();

         BooleanBuilder bb = builders.getBooleanBuilder();
         assertNotNull(bb);
         assertSame(bb, builders.getBooleanBuilder());

         ByteBuilder b2 = builders.getByteBuilder();
         assertNotNull(b2);
         assertSame(b2, builders.getByteBuilder());

         ShortBuilder sb = builders.getShortBuilder();
         assertNotNull(sb);
         assertSame(sb, builders.getShortBuilder());

         IntBuilder ib = builders.getIntBuilder();
         assertNotNull(ib);
         assertSame(ib, builders.getIntBuilder());

         LongBuilder lb = builders.getLongBuilder();
         assertNotNull(lb);
         assertSame(lb, builders.getLongBuilder());

         FloatBuilder fb = builders.getFloatBuilder();
         assertNotNull(fb);
         assertSame(fb, builders.getFloatBuilder());

         DoubleBuilder db = builders.getDoubleBuilder();
         assertNotNull(db);
         assertSame(db, builders.getDoubleBuilder());
     }

     public void testArrayComparator()
     {
         final int[] INT3 = new int[] { 3, 4, 5 };
         Object comp = ArrayBuilders.getArrayComparator(INT3);
         assertFalse(comp.equals(null));
         assertTrue(comp.equals(INT3));
         assertTrue(comp.equals(new int[] { 3, 4, 5 }));
         assertFalse(comp.equals(new int[] { 5 }));
         assertFalse(comp.equals(new int[] { 3, 4 }));
         assertFalse(comp.equals(new int[] { 3, 5, 4 }));
         assertFalse(comp.equals(new int[] { 3, 4, 5, 6 }));
     }

     public void testArraySet()
     {
         HashSet<String> set = ArrayBuilders.arrayToSet(new String[] { "foo", "bar" });
         assertEquals(2, set.size());
         assertEquals(new HashSet<String>(Arrays.asList("bar", "foo")), set);
     }
}
