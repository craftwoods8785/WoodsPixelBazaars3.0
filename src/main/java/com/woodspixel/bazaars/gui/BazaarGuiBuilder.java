package com.woodspixel.bazaars.gui;

import com.woodspixel.bazaars.WoodsPixelBazaarsPlugin;
import com.woodspixel.bazaars.economy.EconomyService;
import com.woodspixel.bazaars.market.BazaarMarketService;
import com.woodspixel.bazaars.model.BazaarCategory;
import com.woodspixel.bazaars.util.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BazaarGuiBuilder {

    private BazaarGuiBuilder() {
    }

    public static Inventory mainMenu(WoodsPixelBazaarsPlugin plugin, BazaarMarketService marketService, EconomyService economyService, Player player) {
        String title = plugin.getConfig().getString("settings.gui.title-main", "WoodsPixel Bazaar");
        Inventory inventory = Bukkit.createInventory(new MainMenuHolder(), 54, FormatUtil.color("&8" + title));

        int slot = 10;
        for (BazaarCategory category : marketService.categories()) {
            ItemStack icon = new ItemStack(category.icon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(FormatUtil.color(category.displayName()));

            List<Component> lore = new ArrayList<>();
            for (String line : category.description()) {
                lore.add(FormatUtil.color(line));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click to open category", NamedTextColor.YELLOW));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);

            slot += 2;
            if (slot % 9 == 0) {
                slot += 2;
            }
        }

        ItemStack balance = new ItemStack(Material.SUNFLOWER);
        ItemMeta balanceMeta = balance.getItemMeta();
        balanceMeta.displayName(Component.text("Your Balance", NamedTextColor.GOLD));
        balanceMeta.lore(List.of(
                Component.text(economyService.format(economyService.getBalance(player)) + " " + economyService.currencyNamePlural(), NamedTextColor.YELLOW),
                Component.text("Use this to buy bazaar items", NamedTextColor.GRAY)
        ));
        balance.setItemMeta(balanceMeta);
        inventory.setItem(49, balance);

        fillGlass(inventory);
        return inventory;
    }

    public static Inventory categoryMenu(WoodsPixelBazaarsPlugin plugin, BazaarMarketService marketService,
                                         EconomyService economyService, Player player, BazaarCategory category) {
        String titlePrefix = plugin.getConfig().getString("settings.gui.title-category-prefix", "Bazaar » ");
        Inventory inventory = Bukkit.createInventory(new CategoryMenuHolder(category.key()), 54,
                FormatUtil.color("&8" + titlePrefix + category.displayName()));

        int slot = 10;
        for (Map.Entry<Material, Double> entry : category.basePrices().entrySet()) {
            Material material = entry.getKey();
            ItemStack stack = new ItemStack(material);
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(Component.text(pretty(material), NamedTextColor.AQUA));

            double buy = marketService.getBuyPrice(material);
            double sell = marketService.getSellPrice(material);
            int ownAmount = countItems(player, material);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Buy Price: " + FormatUtil.number(buy), NamedTextColor.GREEN));
            lore.add(Component.text("Sell Price: " + FormatUtil.number(sell), NamedTextColor.RED));
            lore.add(Component.text("Owned: " + ownAmount, NamedTextColor.YELLOW));
            lore.add(Component.text("History: " + marketService.historySummary(material), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Left Click: Buy 1", NamedTextColor.GREEN));
            lore.add(Component.text("Shift Left: Buy 64", NamedTextColor.GREEN));
            lore.add(Component.text("Right Click: Sell 1", NamedTextColor.RED));
            lore.add(Component.text("Shift Right: Sell 64", NamedTextColor.RED));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);

            slot++;
            if ((slot + 1) % 9 == 0) {
                slot += 2;
            }
            if (slot > 43) {
                break;
            }
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back to Categories", NamedTextColor.YELLOW));
        back.setItemMeta(backMeta);
        inventory.setItem(45, back);

        ItemStack balance = new ItemStack(Material.SUNFLOWER);
        ItemMeta balanceMeta = balance.getItemMeta();
        balanceMeta.displayName(Component.text("Your Balance", NamedTextColor.GOLD));
        balanceMeta.lore(List.of(
                Component.text(economyService.format(economyService.getBalance(player)) + " " + economyService.currencyNamePlural(), NamedTextColor.YELLOW)
        ));
        balance.setItemMeta(balanceMeta);
        inventory.setItem(49, balance);

        fillGlass(inventory);
        return inventory;
    }

    private static void fillGlass(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
        filler.setItemMeta(meta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                    inventory.setItem(i, filler);
                }
            }
        }
    }

    private static int countItems(Player player, Material material) {
        int amount = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null || content.getType() != material) {
                continue;
            }
            amount += content.getAmount();
        }
        return amount;
    }

    private static String pretty(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }
        return builder.toString().trim();
    }
}
