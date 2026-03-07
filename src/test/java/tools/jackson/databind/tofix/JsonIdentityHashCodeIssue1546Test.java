package tools.jackson.databind.tofix;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#1546]: Forward references with collections result in corrupt HashSet behavior
 * when objects are added to HashSet before their ID is fully deserialized.
 * <p>
 * The issue occurs when:
 * <ul>
 * <li>Objects have ID fields that are part of hashCode/equals</li>
 * <li>Objects are deserialized using @JsonIdentityInfo or @JsonBackReference</li>
 * <li>Objects are stored in HashSets</li>
 * </ul>
 * <p>
 * The problem: Objects are added to HashSets BEFORE their ID/reference fields are fully
 * set from JSON, causing the hashCode to be calculated with incomplete data. When the
 * ID/reference is later updated, the hashCode changes, breaking the HashSet contract.
 * This causes contains() and remove() operations to fail.
 */
public class JsonIdentityHashCodeIssue1546Test extends DatabindTestUtil
{
    // Base class with UUID-initialized id field
    static abstract class AbstractIdBase {
        protected String id = UUID.randomUUID().toString();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            AbstractIdBase other = (AbstractIdBase) obj;
            return Objects.equals(id, other.id);
        }
    }

    @JsonIdentityInfo(
        property = "id",
        generator = ObjectIdGenerators.PropertyGenerator.class
    )
    static class Product extends AbstractIdBase {
        private String name;

        public Product() { }

        public Product(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public String toString() {
            return "Product{id='" + id + "', name='" + name + "'}";
        }
    }

    static class Order extends AbstractIdBase {
        @JsonIdentityReference(alwaysAsId = true)
        private Set<Product> items = new HashSet<>();

        public Order() { }

        public Order(String id) {
            this.id = id;
        }

        public Set<Product> getItems() { return items; }
        public void setItems(Set<Product> items) { this.items = items; }

        @Override
        public String toString() {
            return "Order{id='" + id + "', items=" + items.size() + "}";
        }
    }

    static class Root {
        private Set<Order> orders = new HashSet<>();

        @JsonIdentityInfo(
            property = "id",
            generator = ObjectIdGenerators.PropertyGenerator.class
        )
        private Set<Product> products = new HashSet<>();

        public Set<Order> getOrders() { return orders; }
        public void setOrders(Set<Order> orders) { this.orders = orders; }

        public Set<Product> getProducts() { return products; }
        public void setProducts(Set<Product> products) { this.products = products; }
    }

    // Test case demonstrating the HashSet corruption issue with @JsonIdentityReference
    // This test case is based on the original issue report and comments
    @Test
    public void testHashSetCorruptionWithIdentityReferences() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

        // Create test data
        Product p1 = new Product("product-1", "Apple");
        Product p2 = new Product("product-2", "Cherry");
        Product p3 = new Product("product-3", "Strawberry");

        Order order1 = new Order("order-1");
        order1.getItems().add(p1);
        order1.getItems().add(p2);
        order1.getItems().add(p3);

        Order order2 = new Order("order-2");
        order2.getItems().add(p2);
        order2.getItems().add(p3);

        Root root = new Root();
        root.getProducts().add(p1);
        root.getProducts().add(p2);
        root.getProducts().add(p3);
        root.getOrders().add(order1);
        root.getOrders().add(order2);

        // Serialize
        String json = mapper.writeValueAsString(root);
        // System.out.println("JSON: " + json);

        // Deserialize
        Root deserialized = mapper.readValue(json, Root.class);

        // Verify products were deserialized
        assertEquals(3, deserialized.getProducts().size());
        assertEquals(2, deserialized.getOrders().size());

        // Find an order with items
        Order deserializedOrder = deserialized.getOrders().stream()
            .filter(o -> !o.getItems().isEmpty())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected at least one order with items"));

        // Find a product that should be in the order
        Product someProduct = deserializedOrder.getItems().iterator().next();

        // Would fail if: contains() returns false even though the product is in the set
        // because the product was added to the HashSet with a different hashCode
        // (before its id field was properly set from JSON)
        assertTrue(deserializedOrder.getItems().contains(someProduct),
            "HashSet should contain the product - but fails due to hashCode corruption");

        // Workaround: using stream().anyMatch() works because it doesn't rely on hashCode
        assertTrue(deserializedOrder.getItems().stream()
            .anyMatch(p -> p.equals(someProduct)),
            "Stream-based equality check works");

        // Could also fail
        Set<Product> itemsCopy = new HashSet<>(deserializedOrder.getItems());
        assertTrue(itemsCopy.remove(someProduct),
            "Should be able to remove product from HashSet - but fails due to hashCode corruption");
    }

    // ========================================================================
    // Simpler test case with parent-child back references
    // This demonstrates the issue when hashCode includes the parent reference
    // ========================================================================

    @JsonIdentityInfo(
        property = "id",
        generator = ObjectIdGenerators.PropertyGenerator.class
    )
    static class Parent extends AbstractIdBase {
        private String name;

        @JsonManagedReference
        private Set<Child> children = new HashSet<>();

        public Parent() { }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Set<Child> getChildren() { return children; }
        public void setChildren(Set<Child> children) { this.children = children; }
    }

    @JsonIdentityInfo(
        property = "id",
        generator = ObjectIdGenerators.PropertyGenerator.class
    )
    static class Child extends AbstractIdBase {
        private String name;

        @JsonBackReference
        private Parent parent;

        public Child() { }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Parent getParent() { return parent; }
        public void setParent(Parent parent) { this.parent = parent; }

        @Override
        public int hashCode() {
            // Include parent in hashCode - this is what triggers the issue
            return Objects.hash(id, parent);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Child other = (Child) obj;
            return Objects.equals(id, other.id) && Objects.equals(parent, other.parent);
        }
    }

    // This test currently FAILS, demonstrating the bug
    // Child objects are added to HashSet before parent back-reference is set,
    // causing hashCode to change after insertion
    @JacksonTestFailureExpected
    @Test
    public void testHashSetCorruptionWithBackReferences() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

        // Create parent with children
        Parent parent = new Parent();
        parent.setId("parent-1");
        parent.setName("Parent");

        Child child1 = new Child();
        child1.setId("child-1");
        child1.setName("Child 1");
        child1.setParent(parent);

        Child child2 = new Child();
        child2.setId("child-2");
        child2.setName("Child 2");
        child2.setParent(parent);

        parent.getChildren().add(child1);
        parent.getChildren().add(child2);

        // Serialize
        String json = mapper.writeValueAsString(parent);

        // Deserialize
        Parent deserialized = mapper.readValue(json, Parent.class);

        // Verify children were deserialized
        assertEquals(2, deserialized.getChildren().size());

        // Get a child from the set
        Child deserializedChild = deserialized.getChildren().iterator().next();

        // THIS IS THE BUG: contains() fails because child's parent reference
        // was set AFTER the child was added to the HashSet, changing its hashCode
        assertTrue(deserialized.getChildren().contains(deserializedChild),
            "HashSet should contain the child - but fails because parent back-reference " +
            "was set after child was added to the HashSet");

        // Remove should also fail
        Set<Child> childrenCopy = new HashSet<>(deserialized.getChildren());
        assertTrue(childrenCopy.remove(deserializedChild),
            "Should be able to remove child from HashSet");
    }
}
