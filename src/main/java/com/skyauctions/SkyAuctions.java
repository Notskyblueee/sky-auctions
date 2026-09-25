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

        // ---------------------------------------------------------
        // Initialize managers
        // ---------------------------------------------------------

        this.configManager = new ConfigManager(this);
        this.economyManager = new EconomyManager(this);
        this.shopIntegration = new ShopIntegration(this);
        this.auctionManager = new AuctionManager(this);
        this.guiManager = new GuiManager(this);

        // ---------------------------------------------------------
        // Register listeners
        // ---------------------------------------------------------

        getServer().getPluginManager().registerEvents(
                new GuiListener(this),
                this
        );

        // ---------------------------------------------------------
        // Register /skyauctions
        // Aliases:
        // /ah
        // /auction
        // ---------------------------------------------------------

        SkyAuctionsCommand executor = new SkyAuctionsCommand(this);

        var command = getCommand("skyauctions");

        if (command == null) {
            getLogger().severe(
                    "Command 'skyauctions' could not be found in plugin.yml!"
            );
        } else {
            command.setExecutor(executor);
            command.setTabCompleter(executor);

            getLogger().info(
                    "Registered /skyauctions, /ah and /auction."
            );
        }

        // ---------------------------------------------------------
        // PlaceholderAPI
        // ---------------------------------------------------------

        if (getConfig().getBoolean(
                "integrations.placeholderapi",
                true
        )
                && Bukkit.getPluginManager()
                .getPlugin("PlaceholderAPI") != null) {

            new SkyAuctionsExpansion(this).register();

            getLogger().info(
                    "Hooked into PlaceholderAPI."
            );
        }

        // ---------------------------------------------------------
        // Vault Economy
        // ---------------------------------------------------------

        if (economyManager.isEnabled()) {
            getLogger().info(
                    "Hooked into Vault economy."
            );
        } else {
            getLogger().warning(
                    "Vault economy is not available."
            );
        }

        // ---------------------------------------------------------
        // ShopGUIPlus
        // ---------------------------------------------------------

        if (shopIntegration.isShopGuiPlusEnabled()) {
            getLogger().info(
                    "Hooked into ShopGUIPlus for price hints."
            );
        }

        // ---------------------------------------------------------
        // EconomyShopGUI
        // ---------------------------------------------------------

        if (shopIntegration.isEconomyShopGuiEnabled()) {
            getLogger().info(
                    "Hooked into EconomyShopGUI for price hints."
            );
        }

        // ---------------------------------------------------------
        // Expiration processor + autosave
        // Runs once every minute
        // ---------------------------------------------------------

        int autosaveTicks = 20 * 60;

        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> auctionManager.processExpirations(),
                autosaveTicks,
                autosaveTicks
        );

        // ---------------------------------------------------------
        // Plugin enabled
        // ---------------------------------------------------------

        getLogger().info(
                "SkyAuctions has been enabled."
        );
    }

    // =============================================================
    // DISABLE
    // =============================================================

    @Override
    public void onDisable() {

        if (auctionManager != null) {
            auctionManager.save();
        }

        getLogger().info(
                "SkyAuctions has been disabled."
        );
    }

    // =============================================================
    // AUCTION SALE BROADCAST
    // =============================================================

    /**
     * Broadcasts a message when a player lists an item.
     *
     * Supports:
     * - Action bar
     * - Chat
     * - Permissions
     * - Configurable currency
     * - Configurable message format
     */
    public void broadcastSale(
            Player seller,
            ItemStack item,
            double price
    ) {

        // Broadcast disabled
        if (!getConfig().getBoolean(
                "broadcast.enabled",
                true
        )) {
            return;
        }

        // Currency symbol
        String currency = getConfig().getString(
                "settings.currency-symbol",
                "$"
        );

        // Message format
        String format = getConfig().getString(
                "broadcast.format",
                "%player% listed %amount%x %item% for %price%%currency%"
        );

        // Replace placeholders
        format = format
                .replace(
                        "%player%",
                        seller.getName()
                )
                .replace(
                        "%amount%",
                        String.valueOf(item.getAmount())
                )
                .replace(
                        "%item%",
                        ItemUtil.niceName(item)
                )
                .replace(
                        "%price%",
                        economyManager.format(price)
                )
                .replace(
                        "%currency%",
                        currency
                );

        // Convert colors
        Component component = ColorUtil.color(format);

        // Action bar or chat
        boolean actionBar = getConfig().getBoolean(
                "broadcast.action-bar",
                true
        );

        // Send to online players
        for (Player online : Bukkit.getOnlinePlayers()) {

            // Only players with notification permission receive it
            if (!online.hasPermission(
                    "skyauctions.notify.sale"
            )) {
                continue;
            }

            if (actionBar) {

                online.sendActionBar(component);

            } else {

                online.sendMessage(component);

            }
        }
    }

    // =============================================================
    // ACCESSORS
    // =============================================================

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
