package com.fasterxml.jackson.databind.big;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BiggerDataTest
{
    static class Citm
    {
        public Map<Integer,String> areaNames;
        public Map<Integer,String> audienceSubCategoryNames;
        public Map<Integer,String> blockNames;
        public Map<Integer,String> seatCategoryNames;
        public Map<Integer,String> subTopicNames;
        public Map<Integer,String> subjectNames;
        public Map<Integer,String> topicNames;
        public Map<Integer,int[]> topicSubTopics;
        public Map<String,String> venueNames;

        public Map<Integer,Event> events;
        public List<Performance> performances;
    }

    static class Event
    {
        public int id;
        public String name;
        public String description;
        public String subtitle;
        public String logo;
        public Integer subjectCode; // nullable
        public int[] topicIds;
        public LinkedHashSet<Integer> subTopicIds;
    }

    static class Performance
    {
        public int id;
        public int eventId;
        public String name;
        public String description;
        public String logo;

        public List<Price> prices;
        public List<SeatCategory> seatCategories;

        public long start;
        public String seatMapImage;
        public String venueCode;
    }

    static class Price {
        public int amount;
        public int audienceSubCategoryId;
        public int seatCategoryId;
    }

    static class SeatCategory {
        public int seatCategoryId;
        public List<Area> areas;
    }

    static class Area {
        public int areaId;
        public int[] blockIds;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

	private final ObjectMapper MAPPER = JsonMapper.builder().build();

	@Test
	public void testReading() throws Exception
	{
		Citm citm = MAPPER.readValue(getClass().getResourceAsStream("/data/citm_catalog.json"),
				Citm.class);
		assertNotNull(citm);
		assertNotNull(citm.areaNames);
		assertEquals(17, citm.areaNames.size());
		assertNotNull(citm.events);
		assertEquals(184, citm.events.size());

		assertNotNull(citm.seatCategoryNames);
		assertEquals(64, citm.seatCategoryNames.size());
		assertNotNull(citm.subTopicNames);
		assertEquals(19, citm.subTopicNames.size());
		assertNotNull(citm.subjectNames);
		assertEquals(0, citm.subjectNames.size());
		assertNotNull(citm.topicNames);
		assertEquals(4, citm.topicNames.size());
		assertNotNull(citm.topicSubTopics);
		assertEquals(4, citm.topicSubTopics.size());
		assertNotNull(citm.venueNames);
		assertEquals(1, citm.venueNames.size());
	}

	@Test
	public void testRoundTrip() throws Exception
	{
		Citm citm = MAPPER.readValue(getClass().getResourceAsStream("/data/citm_catalog.json"),
				Citm.class);

		ObjectWriter w = MAPPER.writerWithDefaultPrettyPrinter();

		String json1 = w.writeValueAsString(citm);
		Citm citm2 = MAPPER.readValue(json1, Citm.class);
		String json2 = w.writeValueAsString(citm2);

		assertEquals(json1, json2);
	}
}
