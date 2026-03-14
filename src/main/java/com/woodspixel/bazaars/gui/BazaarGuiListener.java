package com.woodspixel.bazaars.gui;

import com.woodspixel.bazaars.WoodsPixelBazaarsPlugin;
import com.woodspixel.bazaars.economy.EconomyService;
import com.woodspixel.bazaars.market.BazaarMarketService;
import com.woodspixel.bazaars.model.BazaarCategory;
import com.woodspixel.bazaars.util.FormatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BazaarGuiListener implements Listener {

    private final WoodsPixelBazaarsPlugin plugin;
    private final BazaarMarketService marketService;
    private final EconomyService economyService;

    public BazaarGuiListener(WoodsPixelBazaarsPlugin plugin, BazaarMarketService marketService, EconomyService economyService) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.economyService = economyService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BazaarHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (holder instanceof MainMenuHolder) {
            handleMainMenuClick(player, clicked.getType());
            return;
        }

        if (holder instanceof CategoryMenuHolder categoryMenuHolder) {
            handleCategoryClick(player, categoryMenuHolder, clicked, event.getClick());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BazaarHolder) {
            marketService.save();
        }
    }

    private void handleMainMenuClick(Player player, Material clickedType) {
        for (BazaarCategory category : marketService.categories()) {
            if (category.icon() == clickedType) {
                player.openInventory(BazaarGuiBuilder.categoryMenu(plugin, marketService, economyService, player, category));
                return;
            }
        }
    }

    private void handleCategoryClick(Player player, CategoryMenuHolder holder, ItemStack clicked, ClickType clickType) {
        Material material = clicked.getType();

        if (material == Material.ARROW) {
            player.openInventory(BazaarGuiBuilder.mainMenu(plugin, marketService, economyService, player));
            return;
        }

        BazaarCategory category = marketService.category(holder.categoryKey());
        if (category == null || !category.basePrices().containsKey(material)) {
            return;
        }

        int amount = (clickType.isShiftClick() ? 64 : 1);

        if (clickType.isLeftClick()) {
            processBuy(player, material, amount);
        } else if (clickType.isRightClick()) {
            processSell(player, material, amount);
        }

        BazaarCategory refreshed = marketService.category(holder.categoryKey());
        if (refreshed != null) {
            player.openInventory(BazaarGuiBuilder.categoryMenu(plugin, marketService, economyService, player, refreshed));
        }
    }

    private void processBuy(Player player, Material material, int amount) {
        double buyPrice = marketService.getBuyPrice(material);
        if (buyPrice <= 0) {
            player.sendMessage(FormatUtil.error("This item is not tradable in bazaar."));
            return;
        }

        double total = buyPrice * amount;
        if (!economyService.has(player, total)) {
            player.sendMessage(FormatUtil.error("You don't have enough " + economyService.currencyNamePlural() + "."));
            return;
        }

        Map<Integer, ItemStack> remaining = player.getInventory().addItem(new ItemStack(material, amount));
        int delivered = amount;
        if (!remaining.isEmpty()) {
            delivered = amount - remaining.values().stream().mapToInt(ItemStack::getAmount).sum();
            for (ItemStack rem : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }
        }

        if (delivered <= 0) {
            player.sendMessage(FormatUtil.error("No inventory space to receive items."));
            return;
        }

        double charged = buyPrice * delivered;
        if (!economyService.withdraw(player, charged)) {
            player.sendMessage(FormatUtil.error("Payment failed. Try again."));
            return;
        }

        marketService.applyBuyPressure(material, delivered);
        player.sendMessage(FormatUtil.success("Bought " + delivered + "x " + pretty(material) + " for " + economyService.format(charged) + "."));
    }

    private void processSell(Player player, Material material, int amount) {
        int available = countInInventory(player, material);
        if (available <= 0) {
            player.sendMessage(FormatUtil.error("You don't have that item to sell."));
            return;
        }

        int selling = Math.min(available, amount);
        removeItems(player, material, selling);

        double sellPrice = marketService.getSellPrice(material);
        double total = sellPrice * selling;
        if (!economyService.deposit(player, total)) {
            player.sendMessage(FormatUtil.error("Could not pay you. Contact staff."));
            player.getInventory().addItem(new ItemStack(material, selling));
            return;
        }

        marketService.applySellPressure(material, selling);
        player.sendMessage(FormatUtil.success("Sold " + selling + "x " + pretty(material) + " for " + economyService.format(total) + "."));
    }

    private int countInInventory(Player player, Material material) {
        int total = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null && content.getType() == material) {
                total += content.getAmount();
            }
        }
        return total;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }

            if (remaining <= 0) {
                break;
            }
        }

        player.getInventory().setContents(contents);
    }

    private String pretty(Material material) {
        String[] split = material.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : split) {
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}
