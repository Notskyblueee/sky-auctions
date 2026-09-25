package com.skyauctions;

import com.skyauctions.commands.SkyAuctionsCommand;
import com.skyauctions.config.ConfigManager;
import com.skyauctions.data.AuctionManager;
import com.skyauctions.economy.EconomyManager;
import com.skyauctions.economy.ShopIntegration;
import com.skyauctions.gui.GuiManager;
import com.skyauctions.listeners.GuiListener;
import com.skyauctions.placeholder.SkyAuctionsExpansion;
import com.skyauctions.util.ColorUtil;
import com.skyauctions.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyAuctions extends JavaPlugin {

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private ShopIntegration shopIntegration;
    private AuctionManager auctionManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.economyManager = new EconomyManager(this);
        this.shopIntegration = new ShopIntegration(this);
        this.auctionManager = new AuctionManager(this);
        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        SkyAuctionsCommand executor = new SkyAuctionsCommand(this);
        var command = getCommand("skyauctions");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        if (getConfig().getBoolean("integrations.placeholderapi", true)
                && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SkyAuctionsExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        if (economyManager.isEnabled()) {
            getLogger().info("Hooked into Vault economy.");
        }
        if (shopIntegration.isShopGuiPlusEnabled()) {
            getLogger().info("Hooked into ShopGUIPlus for price hints.");
        }
        if (shopIntegration.isEconomyShopGuiEnabled()) {
            getLogger().info("Hooked into EconomyShopGUI for price hints.");
        }

        // Expire listings + autosave, once a minute
        int autosaveTicks = 20 * 60; // 1 minute
        Bukkit.getScheduler().runTaskTimer(this, () -> auctionManager.processExpirations(), autosaveTicks, autosaveTicks);

        getLogger().info("SkyAuctions has been enabled.");
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.save();
        }
        getLogger().info("SkyAuctions has been disabled.");
    }

    /**
     * Broadcasts (action bar or chat, per config) that a player just listed an item.
     */
    public void broadcastSale(Player seller, ItemStack item, double price) {
        if (!getConfig().getBoolean("broadcast.enabled", true)) {
            return;
        }
        String currency = getConfig().getString("settings.currency-symbol", "$");
        String format = getConfig().getString("broadcast.format",
                "%player% listed %amount%x %item% for %price%%currency%");
        format = format.replace("%player%", seller.getName())
                .replace("%amount%", String.valueOf(item.getAmount()))
                .replace("%item%", ItemUtil.niceName(item))
                .replace("%price%", economyManager.format(price))
                .replace("%currency%", currency);

        Component component = ColorUtil.color(format);
        boolean actionBar = getConfig().getBoolean("broadcast.action-bar", true);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("skyauctions.notify.sale")) {
                continue;
            }
            if (actionBar) {
                online.sendActionBar(component);
            } else {
                online.sendMessage(component);
            }
        }
    }

    // ---------------------------------------------------------------
    //  Accessors
    // ---------------------------------------------------------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopIntegration getShopIntegration() {
        return shopIntegration;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
