package com.skyauctions.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;

public class EconomyManager {

    private Economy economy;
    private boolean enabled = false;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public EconomyManager(JavaPlugin plugin) {
        setup(plugin);
    }

    private void setup(JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found - economy features are disabled!");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy plugin hooked into Vault - economy features are disabled!");
            return;
        }
        economy = rsp.getProvider();
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBalance(OfflinePlayer player) {
        if (!enabled) return 0;
        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return format.format(amount);
    }
}
