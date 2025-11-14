package com.unh.pantrypalonevo

object OCRProductDetector {

    data class ProductMatch(
        val displayName: String,
        val category: String,
        val confidence: Float
    )

    /**
     * Find product match based on ML Kit label and OCR text
     */
    fun findProductMatch(labelText: String, ocrText: String): ProductMatch? {
        // First check if label matches known product
        val productFromLabel = ProductDatabase.getProductByName(labelText)
        if (productFromLabel != null) {
            return ProductMatch(
                displayName = productFromLabel.name,
                category = productFromLabel.category,
                confidence = 0.9f
            )
        }

        // Then check OCR text for brand names
        val detectedBrand = detectBrandFromText(ocrText)
        if (detectedBrand != null) {
            val product = ProductDatabase.getProductByBrand(detectedBrand)
            if (product != null) {
                return ProductMatch(
                    displayName = product.name,
                    category = product.category,
                    confidence = 0.85f
                )
            }
        }

        // Fallback: check if label is grocery-related
        if (ProductDatabase.isGroceryProduct(labelText)) {
            return ProductMatch(
                displayName = labelText,
                category = ProductDatabase.getCategoryForProduct(labelText),
                confidence = 0.7f
            )
        }

        return null
    }

    private fun detectBrandFromText(text: String): String? {
        val brandKeywords = mapOf(
            "HEINZ" to "Heinz",
            "HOOD" to "Hood",
            "CAMPBELLS" to "Campbell's",
            "DELMONTE" to "Del Monte",
            "KRAFT" to "Kraft",
            "NESTLE" to "Nestle",
            "COCA-COLA" to "Coca-Cola",
            "PEPSI" to "Pepsi"
        )

        val upperText = text.uppercase()
        for ((keyword, brand) in brandKeywords) {
            if (upperText.contains(keyword)) {
                return brand
            }
        }

        return null
    }
}