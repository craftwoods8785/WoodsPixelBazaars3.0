package com.woodspixel.bazaars;

import com.woodspixel.bazaars.command.BazaarCommand;
import com.woodspixel.bazaars.economy.EconomyService;
import com.woodspixel.bazaars.gui.BazaarGuiListener;
import com.woodspixel.bazaars.market.BazaarMarketService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class WoodsPixelBazaarsPlugin extends JavaPlugin {

    private EconomyService economyService;
    private BazaarMarketService marketService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economyService = new EconomyService(this);
        if (!economyService.setup()) {
            getLogger().severe("Vault economy provider (EssentialsX economy via Vault) not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        marketService = new BazaarMarketService(this);
        marketService.load();
        marketService.startRecoveryTask();

        BazaarCommand commandExecutor = new BazaarCommand(this, marketService, economyService);
        if (getCommand("bz") != null) {
            getCommand("bz").setExecutor(commandExecutor);
            getCommand("bz").setTabCompleter(commandExecutor);
        }

        Bukkit.getPluginManager().registerEvents(
                new BazaarGuiListener(this, marketService, economyService),
                this
        );

        getLogger().info("WoodsPixelBazaars enabled.");
    }

    @Override
    public void onDisable() {
        if (marketService != null) {
            marketService.save();
        }
    }
}
