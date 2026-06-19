package tools.jackson.databind.jref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class JRefSimpleNestedTypeTest extends JRefAbstractTest {

	record TreeNode(TreeNode parent, String name, String data) {
	}
	private static final String ADDRESS = "Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.\r\n"
			+ "\r\n"
			+ "Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war. We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this.\r\n"
			+ "\r\n"
			+ "But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom -- and that government of the people, by the people, for the people, shall not perish from the earth.";
			
	TreeNode[] buildTwoLevelThreeChildrenArray() {
		TreeNode topNode = new TreeNode(null, "top", ADDRESS);
		// Create three children
		TreeNode firstChild = new TreeNode(topNode, "child1", "data1");
		TreeNode secondChild = new TreeNode(topNode, "child2", "data2");
		TreeNode thirdChild = new TreeNode(topNode, "child3" , "data3");
		// Put top and all nodes in array
		return new TreeNode[] { topNode, firstChild, secondChild, thirdChild };
	}
	@Test 
	void testSerializeSingleLevelTreeThreeChildrenNoJRef() {
		
		ObjectMapper mapper = buildObjectMapperWithoutJRefSupport();
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildTwoLevelThreeChildrenArray());
		// No jrefs
		assertJRefCount(json, 0);
	}
	
	@Test 
	void testSerializeSingleLevelTreeThreeChildrenJRef() {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildTwoLevelThreeChildrenArray());
		assertJRefCount(json, 3);
	}

	static String JREF_JSON = "[ {\r\n"
			+ "  \"parent\" : null,\r\n"
			+ "  \"name\" : \"top\",\r\n"
			+ "  \"data\" : \"Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.\\r\\n\\r\\nNow we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war. We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this.\\r\\n\\r\\nBut, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom -- and that government of the people, by the people, for the people, shall not perish from the earth.\"\r\n"
			+ "}, {\r\n"
			+ "  \"parent\" : {\r\n"
			+ "    \"$ref\" : \"#/0\"\r\n"
			+ "  },\r\n"
			+ "  \"name\" : \"child1\",\r\n"
			+ "  \"data\" : \"data1\"\r\n"
			+ "}, {\r\n"
			+ "  \"parent\" : {\r\n"
			+ "    \"$ref\" : \"#/0\"\r\n"
			+ "  },\r\n"
			+ "  \"name\" : \"child2\",\r\n"
			+ "  \"data\" : \"data2\"\r\n"
			+ "}, {\r\n"
			+ "  \"parent\" : {\r\n"
			+ "    \"$ref\" : \"#/0\"\r\n"
			+ "  },\r\n"
			+ "  \"name\" : \"child3\",\r\n"
			+ "  \"data\" : \"data3\"\r\n"
			+ "} ]";
	@Test 
	void testDeerializeSingleLevelTreeThreeChildrenJRef() {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		TreeNode[] nodes = mapper.readValue(JREF_JSON, TreeNode[].class);
		assertEquals(nodes[0],nodes[1].parent);
		assertEquals(nodes[0],nodes[2].parent);
		assertEquals(nodes[0],nodes[3].parent);
	}

}
