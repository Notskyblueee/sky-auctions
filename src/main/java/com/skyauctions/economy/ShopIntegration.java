package com.skyauctions.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Purely OPTIONAL, cosmetic integration with ShopGUIPlus and
 * EconomyShopGUI. It never adds a compile-time dependency on either
 * plugin's jar - everything is done through reflection, so if the
 * plugin isn't installed (or a method signature changes on their
 * side) SkyAuctions simply skips the price hint instead of breaking.
 *
 * This is used to show the player what those shop plugins would
 * pay for the item they're about to list, as a pricing reference -
 * it never blocks or overrides the player's chosen auction price.
 */
public class ShopIntegration {

    private final JavaPlugin plugin;
    private final boolean shopGuiPlusEnabled;
    private final boolean economyShopGuiEnabled;

    public ShopIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopGuiPlusEnabled = plugin.getConfig().getBoolean("integrations.shopguiplus", true)
                && Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus");
        this.economyShopGuiEnabled = plugin.getConfig().getBoolean("integrations.economyshopgui", true)
                && Bukkit.getPluginManager().isPluginEnabled("EconomyShopGUI");
    }

    /**
     * Returns a suggested sell price for the given item, checked in order
     * ShopGUIPlus -> EconomyShopGUI, or null if neither has a price for it.
     */
    public Double getReferencePrice(Player player, ItemStack item) {
        if (shopGuiPlusEnabled) {
            Double price = tryShopGuiPlus(player, item);
            if (price != null && price > 0) {
                return price;
            }
        }
        if (economyShopGuiEnabled) {
            Double price = tryEconomyShopGui(item);
            if (price != null && price > 0) {
                return price;
            }
        }
        return null;
    }

    private Double tryShopGuiPlus(Player player, ItemStack item) {
        try {
            Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
            for (Method m : apiClass.getMethods()) {
                if (m.getName().equals("getItemStackPriceSell")) {
                    Class<?>[] params = m.getParameterTypes();
                    Object result;
                    if (params.length == 2 && params[0] == Player.class) {
                        result = m.invoke(null, player, item);
                    } else if (params.length == 1) {
                        result = m.invoke(null, item);
                    } else {
                        continue;
                    }
                    if (result instanceof Number number) {
                        return number.doubleValue();
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("ShopGUIPlus price lookup skipped: " + t.getMessage());
        }
        return null;
    }

    private Double tryEconomyShopGui(ItemStack item) {
        try {
            Class<?> apiClass = Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook");
            String[] candidateMethods = {"getSellPriceOfItem", "getSellPrice", "getItemStackPriceSell"};
            for (String name : candidateMethods) {
                for (Method m : apiClass.getMethods()) {
                    if (m.getName().equalsIgnoreCase(name) && m.getParameterTypes().length == 1) {
                        Object result = m.invoke(null, item);
                        if (result instanceof Number number) {
                            return number.doubleValue();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("EconomyShopGUI price lookup skipped: " + t.getMessage());
        }
        return null;
    }

    public boolean isShopGuiPlusEnabled() {
        return shopGuiPlusEnabled;
    }

    public boolean isEconomyShopGuiEnabled() {
        return economyShopGuiEnabled;
    }
}
