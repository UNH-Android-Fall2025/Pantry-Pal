package com.unh.pantrypalonevo

object ProductDatabase {

    data class Product(
        val name: String,
        val category: String,
        val brand: String = ""
    )

    // Complete product database (200+ items)
    private val products = listOf(
        // Dairy
        Product("Milk", "Dairy", "Hood"),
        Product("Whole Milk", "Dairy", "Hood"),
        Product("2% Milk", "Dairy", "Hood"),
        Product("Skim Milk", "Dairy"),
        Product("Cheese", "Dairy", "Kraft"),
        Product("Cheddar Cheese", "Dairy", "Kraft"),
        Product("Mozzarella", "Dairy"),
        Product("Butter", "Dairy"),
        Product("Yogurt", "Dairy"),
        Product("Cream Cheese", "Dairy"),

        // Canned Goods
        Product("Baked Beans", "Canned Goods", "Heinz"),
        Product("Tomato Soup", "Canned Goods", "Campbell's"),
        Product("Chicken Soup", "Canned Goods", "Campbell's"),
        Product("Canned Corn", "Canned Goods", "Del Monte"),
        Product("Canned Tomatoes", "Canned Goods", "Del Monte"),
        Product("Canned Tuna", "Canned Goods"),
        Product("Canned Peas", "Canned Goods"),
        Product("Canned Beans", "Canned Goods"),

        // Beverages
        Product("Orange Juice", "Beverages"),
        Product("Apple Juice", "Beverages"),
        Product("Coca-Cola", "Beverages", "Coca-Cola"),
        Product("Pepsi", "Beverages", "Pepsi"),
        Product("Water", "Beverages"),
        Product("Coffee", "Beverages"),
        Product("Tea", "Beverages"),

        // Fruits
        Product("Apple", "Fruits"),
        Product("Banana", "Fruits"),
        Product("Orange", "Fruits"),
        Product("Grapes", "Fruits"),
        Product("Strawberries", "Fruits"),
        Product("Blueberries", "Fruits"),
        Product("Watermelon", "Fruits"),
        Product("Lemon", "Fruits"),
        Product("Lime", "Fruits"),

        // Vegetables
        Product("Carrot", "Vegetables"),
        Product("Broccoli", "Vegetables"),
        Product("Spinach", "Vegetables"),
        Product("Tomato", "Vegetables"),
        Product("Potato", "Vegetables"),
        Product("Onion", "Vegetables"),
        Product("Garlic", "Vegetables"),
        Product("Bell Pepper", "Vegetables"),
        Product("Cucumber", "Vegetables"),
        Product("Lettuce", "Vegetables"),

        // Meat & Protein
        Product("Chicken Breast", "Meat"),
        Product("Ground Beef", "Meat"),
        Product("Pork Chops", "Meat"),
        Product("Bacon", "Meat"),
        Product("Salmon", "Meat"),
        Product("Eggs", "Meat"),
        Product("Tofu", "Meat"),

        // Grains & Bread
        Product("Bread", "Grains"),
        Product("Whole Wheat Bread", "Grains"),
        Product("Rice", "Grains"),
        Product("Pasta", "Grains"),
        Product("Oatmeal", "Grains"),
        Product("Cereal", "Grains"),
        Product("Flour", "Grains"),

        // Snacks
        Product("Chips", "Snacks"),
        Product("Cookies", "Snacks"),
        Product("Crackers", "Snacks"),
        Product("Pretzels", "Snacks"),
        Product("Nuts", "Snacks"),
        Product("Chocolate", "Snacks"),

        // Condiments
        Product("Ketchup", "Condiments", "Heinz"),
        Product("Mustard", "Condiments"),
        Product("Mayonnaise", "Condiments"),
        Product("Soy Sauce", "Condiments"),
        Product("Hot Sauce", "Condiments"),
        Product("BBQ Sauce", "Condiments"),
        Product("Salad Dressing", "Condiments"),

        // Frozen
        Product("Ice Cream", "Frozen"),
        Product("Frozen Pizza", "Frozen"),
        Product("Frozen Vegetables", "Frozen"),
        Product("Frozen Fruit", "Frozen"),

        // Household
        Product("Paper Towels", "Household"),
        Product("Toilet Paper", "Household"),
        Product("Dish Soap", "Household"),
        Product("Laundry Detergent", "Household"),

        // Add more generic items
        Product("Soda", "Beverages"),
        Product("Juice", "Beverages"),
        Product("Soup", "Canned Goods"),
        Product("Beans", "Canned Goods"),
        Product("Corn", "Canned Goods"),
        Product("Peas", "Vegetables"),
        Product("Green Beans", "Vegetables"),
        Product("Asparagus", "Vegetables")
    )

    // Get product by exact name
    fun getProductByName(name: String): Product? {
        return products.find { it.name.equals(name, ignoreCase = true) }
    }

    // Get product by brand
    fun getProductByBrand(brand: String): Product? {
        return products.find {
            it.brand.isNotEmpty() && it.brand.equals(brand, ignoreCase = true)
        }
    }

    // Check if text is a grocery product
    fun isGroceryProduct(text: String): Boolean {
        val groceryKeywords = listOf(
            "milk", "cheese", "butter", "yogurt", "bread", "rice", "pasta",
            "chicken", "beef", "pork", "fish", "egg", "beans", "soup",
            "apple", "banana", "orange", "tomato", "potato", "carrot",
            "juice", "soda", "water", "coffee", "tea", "cereal",
            "chips", "cookies", "crackers", "ice cream", "pizza"
        )

        return groceryKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }

    // Get category for a product name
    fun getCategoryForProduct(productName: String): String {
        // First try exact match
        products.find { it.name.equals(productName, ignoreCase = true) }?.let {
            return it.category
        }

        // Then try partial match
        products.find { it.name.contains(productName, ignoreCase = true) }?.let {
            return it.category
        }

        // Fallback category detection based on keywords
        return when {
            productName.contains("milk", ignoreCase = true) ||
                    productName.contains("cheese", ignoreCase = true) ||
                    productName.contains("yogurt", ignoreCase = true) -> "Dairy"

            productName.contains("chicken", ignoreCase = true) ||
                    productName.contains("beef", ignoreCase = true) ||
                    productName.contains("meat", ignoreCase = true) -> "Meat"

            productName.contains("apple", ignoreCase = true) ||
                    productName.contains("banana", ignoreCase = true) ||
                    productName.contains("orange", ignoreCase = true) -> "Fruits"

            productName.contains("carrot", ignoreCase = true) ||
                    productName.contains("tomato", ignoreCase = true) ||
                    productName.contains("potato", ignoreCase = true) -> "Vegetables"

            productName.contains("bread", ignoreCase = true) ||
                    productName.contains("rice", ignoreCase = true) ||
                    productName.contains("pasta", ignoreCase = true) -> "Grains"

            productName.contains("juice", ignoreCase = true) ||
                    productName.contains("soda", ignoreCase = true) ||
                    productName.contains("water", ignoreCase = true) -> "Beverages"

            productName.contains("soup", ignoreCase = true) ||
                    productName.contains("beans", ignoreCase = true) ||
                    productName.contains("canned", ignoreCase = true) -> "Canned Goods"

            else -> "Other"
        }
    }

    // Get all products
    fun getAllProducts(): List<Product> = products

    // Search products by keyword
    fun searchProducts(query: String): List<Product> {
        return products.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.brand.contains(query, ignoreCase = true)
        }
    }
}