package com.woodspixel.bazaars.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record BazaarCategory(
        String key,
        String displayName,
        Material icon,
        List<String> description,
        Map<Material, Double> basePrices
) {
}
