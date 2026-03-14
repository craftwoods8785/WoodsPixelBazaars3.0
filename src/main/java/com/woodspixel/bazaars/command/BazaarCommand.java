package com.woodspixel.bazaars.command;

import com.woodspixel.bazaars.WoodsPixelBazaarsPlugin;
import com.woodspixel.bazaars.economy.EconomyService;
import com.woodspixel.bazaars.gui.BazaarGuiBuilder;
import com.woodspixel.bazaars.market.BazaarMarketService;
import com.woodspixel.bazaars.util.FormatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BazaarCommand implements CommandExecutor, TabCompleter {

    private final WoodsPixelBazaarsPlugin plugin;
    private final BazaarMarketService marketService;
    private final EconomyService economyService;

    public BazaarCommand(WoodsPixelBazaarsPlugin plugin, BazaarMarketService marketService, EconomyService economyService) {
        this.plugin = plugin;
        this.marketService = marketService;
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open bazaar GUI.");
            return true;
        }

        if (!player.hasPermission("woodspixelbazaars.use")) {
            player.sendMessage(FormatUtil.error("You do not have permission to use /bz."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("woodspixelbazaars.admin")) {
                player.sendMessage(FormatUtil.error("You do not have permission to reload."));
                return true;
            }
            plugin.reloadConfig();
            marketService.load();
            player.sendMessage(FormatUtil.success("WoodsPixelBazaars config reloaded."));
            return true;
        }

        player.openInventory(BazaarGuiBuilder.mainMenu(plugin, marketService, economyService, player));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
            return Collections.singletonList("reload");
        }
        return new ArrayList<>();
    }
}
