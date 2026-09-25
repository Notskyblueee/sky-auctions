package com.skyauctions.placeholder;

import com.skyauctions.SkyAuctions;
import com.skyauctions.util.ColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers:
 *   %skyauctions_prefix%            -> the plugin prefix (with hex colors)
 *   %skyauctions_active_listings%   -> total active listings on the server
 *   %skyauctions_my_listings%       -> the requesting player's active listings
 *   %skyauctions_max_listings%      -> the requesting player's max listing slots
 */
public class SkyAuctionsExpansion extends PlaceholderExpansion {

    private final SkyAuctions plugin;

    public SkyAuctionsExpansion(SkyAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "skyauctions";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SkyAuctions";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "prefix" -> legacyPrefix();
            case "active_listings" -> String.valueOf(plugin.getAuctionManager().getActiveListings().size());
            case "my_listings" -> offlinePlayer == null ? "0"
                    : String.valueOf(plugin.getAuctionManager().countActiveListings(offlinePlayer.getUniqueId()));
            case "max_listings" -> {
                if (offlinePlayer != null && offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                    yield String.valueOf(plugin.getAuctionManager().getMaxListingSlots(offlinePlayer.getPlayer()));
                }
                yield String.valueOf(plugin.getConfig().getInt("settings.default-listing-limit", 5));
            }
            default -> null;
        };
    }

    private String legacyPrefix() {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(ColorUtil.color(plugin.getConfigManager().getPrefix()));
    }
}
