package com.woodspixel.bazaars.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {

    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return economy.format(amount);
    }

    public String currencyNamePlural() {
        String configured = plugin.getConfig().getString("settings.currency-name", "Coins");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String fromEconomy = economy.currencyNamePlural();
        return fromEconomy != null ? fromEconomy : "Coins";
    }
}
