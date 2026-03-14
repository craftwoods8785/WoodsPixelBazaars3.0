package com.woodspixel.bazaars.market;

import com.woodspixel.bazaars.WoodsPixelBazaarsPlugin;
import com.woodspixel.bazaars.model.BazaarCategory;
import com.woodspixel.bazaars.model.MarketState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class BazaarMarketService {

    private final WoodsPixelBazaarsPlugin plugin;
    private final Map<String, BazaarCategory> categories = new LinkedHashMap<>();
    private final Map<Material, MarketState> marketStates = new LinkedHashMap<>();

    private File marketFile;
    private YamlConfiguration marketData;

    public BazaarMarketService(WoodsPixelBazaarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadCategories();
        loadMarketData();
    }

    private void loadCategories() {
        categories.clear();
        marketStates.clear();

        ConfigurationSection categoriesSection = plugin.getConfig().getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("No categories in config.yml");
            return;
        }

        for (String key : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(key);
            if (categorySection == null) {
                continue;
            }

            Material icon = Material.matchMaterial(categorySection.getString("icon", "CHEST"));
            if (icon == null) {
                icon = Material.CHEST;
            }

            String displayName = categorySection.getString("display-name", key);
            List<String> description = categorySection.getStringList("description");

            Map<Material, Double> basePrices = new LinkedHashMap<>();
            ConfigurationSection itemsSection = categorySection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String materialName : itemsSection.getKeys(false)) {
                    Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
                    if (material == null || !material.isItem()) {
                        plugin.getLogger().warning("Invalid material in config: " + materialName);
                        continue;
                    }
                    double basePrice = itemsSection.getDouble(materialName, 1.0D);
                    basePrices.put(material, Math.max(0.01D, basePrice));
                    marketStates.put(material, MarketState.fresh(Math.max(0.01D, basePrice)));
                }
            }

            categories.put(key.toLowerCase(Locale.ROOT), new BazaarCategory(key, displayName, icon, description, basePrices));
        }
    }

    private void loadMarketData() {
        marketFile = new File(plugin.getDataFolder(), "markets.yml");
        if (!marketFile.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                marketFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Unable to create markets.yml: " + e.getMessage());
            }
        }

        marketData = YamlConfiguration.loadConfiguration(marketFile);

        int historySize = plugin.getConfig().getInt("settings.history-size", 20);
        for (Map.Entry<Material, MarketState> entry : marketStates.entrySet()) {
            Material material = entry.getKey();
            MarketState state = entry.getValue();

            String path = "items." + material.name();
            double multiplier = marketData.getDouble(path + ".multiplier", 1.0D);
            state.setMultiplier(clampMultiplier(multiplier));

            List<Double> history = marketData.getDoubleList(path + ".history");
            Deque<Double> deque = state.priceHistory();
            deque.clear();
            for (Double value : history) {
                if (value != null) {
                    state.pushHistory(Math.max(0.01D, value), historySize);
                }
            }

            if (deque.isEmpty()) {
                state.pushHistory(getBuyPrice(material), historySize);
            }
        }
    }

    public void save() {
        if (marketData == null) {
            marketData = new YamlConfiguration();
        }

        int historySize = plugin.getConfig().getInt("settings.history-size", 20);

        for (Map.Entry<Material, MarketState> entry : marketStates.entrySet()) {
            Material material = entry.getKey();
            MarketState state = entry.getValue();

            String path = "items." + material.name();
            marketData.set(path + ".multiplier", state.multiplier());
            List<Double> history = new ArrayList<>(state.priceHistory());
            if (history.size() > historySize) {
                history = history.subList(history.size() - historySize, history.size());
            }
            marketData.set(path + ".history", history);
        }

        try {
            marketData.save(marketFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save markets.yml: " + e.getMessage());
        }
    }

    public void startRecoveryTask() {
        long periodTicks = 20L * 60L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double recovery = plugin.getConfig().getDouble("settings.market-recovery-per-minute", 0.003D);
            int historySize = plugin.getConfig().getInt("settings.history-size", 20);

            for (Map.Entry<Material, MarketState> entry : marketStates.entrySet()) {
                MarketState state = entry.getValue();
                double next = state.multiplier() + ((1.0D - state.multiplier()) * recovery);
                state.setMultiplier(clampMultiplier(next));
                state.pushHistory(getBuyPrice(entry.getKey()), historySize);
            }

            save();
        }, periodTicks, periodTicks);
    }

    public Collection<BazaarCategory> categories() {
        return categories.values();
    }

    public BazaarCategory category(String key) {
        return categories.get(key.toLowerCase(Locale.ROOT));
    }

    public double getBuyPrice(Material material) {
        MarketState state = marketStates.get(material);
        if (state == null) {
            return -1;
        }
        return round(state.basePrice() * state.multiplier());
    }

    public double getSellPrice(Material material) {
        MarketState state = marketStates.get(material);
        if (state == null) {
            return -1;
        }
        return round(state.basePrice() * state.multiplier() * 0.92D);
    }

    public void applyBuyPressure(Material material, int amount) {
        MarketState state = marketStates.get(material);
        if (state == null) {
            return;
        }

        double risePerUnit = plugin.getConfig().getDouble("settings.buy-price-rise-per-unit", 0.0035D);
        double updated = state.multiplier() + (risePerUnit * amount);
        state.setMultiplier(clampMultiplier(updated));
        state.pushHistory(getBuyPrice(material), plugin.getConfig().getInt("settings.history-size", 20));
    }

    public void applySellPressure(Material material, int amount) {
        MarketState state = marketStates.get(material);
        if (state == null) {
            return;
        }

        double dropPerUnit = plugin.getConfig().getDouble("settings.sell-price-drop-per-unit", 0.0025D);
        double updated = state.multiplier() - (dropPerUnit * amount);
        state.setMultiplier(clampMultiplier(updated));
        state.pushHistory(getBuyPrice(material), plugin.getConfig().getInt("settings.history-size", 20));
    }

    public String historySummary(Material material) {
        MarketState state = marketStates.get(material);
        if (state == null || state.priceHistory().isEmpty()) {
            return "No history";
        }

        StringJoiner joiner = new StringJoiner(" → ");
        for (Double value : state.priceHistory()) {
            joiner.add(String.format(Locale.US, "%.2f", value));
        }
        return joiner.toString();
    }

    public boolean hasItem(Material material) {
        return marketStates.containsKey(material);
    }

    private double clampMultiplier(double value) {
        double min = plugin.getConfig().getDouble("settings.default-min-multiplier", 0.45D);
        double max = plugin.getConfig().getDouble("settings.default-max-multiplier", 3.0D);
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
