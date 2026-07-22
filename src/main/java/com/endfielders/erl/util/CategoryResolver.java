package com.endfielders.erl.util;

import java.util.Map;

/**
 * Maps frontend goods type selections to database carrier categories.
 * Known types are resolved instantly; unknown/custom types return null
 * so the caller can fall back to AI-based resolution via Gemini.
 */
public class CategoryResolver {

    private static final Map<String, String> GOODS_TO_CATEGORY = Map.ofEntries(
            // Category titles
            Map.entry("commercial b2b & bulk freight", "B2B_FREIGHT"),
            Map.entry("b2b freight", "B2B_FREIGHT"),
            Map.entry("e-commerce & retail small parcels", "E_COMMERCE"),
            Map.entry("e-commerce", "E_COMMERCE"),
            Map.entry("household shifting & packers movers", "HOUSEHOLD"),
            Map.entry("household shifting", "HOUSEHOLD"),
            Map.entry("cold chain, exim & special cargo", "COLD_CHAIN"),
            Map.entry("cold chain", "COLD_CHAIN"),

            // E-Commerce & Retail
            Map.entry("electronics", "E_COMMERCE"),
            Map.entry("documents", "E_COMMERCE"),
            Map.entry("clothing", "E_COMMERCE"),
            Map.entry("accessories", "E_COMMERCE"),
            Map.entry("books", "E_COMMERCE"),
            Map.entry("gadgets", "E_COMMERCE"),

            // B2B & Bulk Freight
            Map.entry("bulk", "B2B_FREIGHT"),
            Map.entry("heavy", "B2B_FREIGHT"),
            Map.entry("machinery", "B2B_FREIGHT"),
            Map.entry("industrial", "B2B_FREIGHT"),
            Map.entry("raw materials", "B2B_FREIGHT"),
            Map.entry("construction", "B2B_FREIGHT"),
            Map.entry("auto parts", "B2B_FREIGHT"),
            Map.entry("vehicles", "B2B_FREIGHT"),

            // Household Shifting
            Map.entry("household", "HOUSEHOLD"),
            Map.entry("furniture", "HOUSEHOLD"),
            Map.entry("home appliances", "HOUSEHOLD"),
            Map.entry("personal belongings", "HOUSEHOLD"),
            Map.entry("relocation", "HOUSEHOLD"),

            // Cold Chain & Special Cargo
            Map.entry("pharma", "COLD_CHAIN"),
            Map.entry("pharma & medical", "COLD_CHAIN"),
            Map.entry("food", "COLD_CHAIN"),
            Map.entry("food & perishables", "COLD_CHAIN"),
            Map.entry("perishable", "COLD_CHAIN"),
            Map.entry("frozen", "COLD_CHAIN"),
            Map.entry("dairy", "COLD_CHAIN"),
            Map.entry("chemicals", "COLD_CHAIN"),
            Map.entry("medical", "COLD_CHAIN"),
            Map.entry("vaccines", "COLD_CHAIN")
    );

    /**
     * Resolves a goods type string to a carrier category.
     * Returns null if the goods type is unknown/custom — caller should
     * fall back to Gemini AI classification.
     */
    public static String resolve(String goodsType) {
        if (goodsType == null || goodsType.isBlank()) return null;
        return GOODS_TO_CATEGORY.get(goodsType.trim().toLowerCase());
    }
}
