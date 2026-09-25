package com.skyauctions.listeners;

import com.skyauctions.SkyAuctions;
import com.skyauctions.data.Auction;
import com.skyauctions.data.AuctionManager;
import com.skyauctions.gui.GuiManager;
import com.skyauctions.gui.SkyAuctionsHolder;
import com.skyauctions.util.ColorUtil;
import com.skyauctions.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    private static final int ANVIL_RESULT_SLOT = 2;
    private final SkyAuctions plugin;

    public GuiListener(SkyAuctions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkyAuctionsHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        switch (holder.getType()) {
            case MAIN -> handleMainClick(event, player, holder);
            case MY_LISTINGS -> handleMyListingsClick(event, player, holder);
            case SOLD_ITEMS -> handleSoldClick(event, player, holder);
            case EXPIRED_ITEMS -> handleExpiredClick(event, player, holder);
            case INFO -> handleInfoClick(event, player);
            case COLLECTION -> handleCollectionClick(event, player, holder);
            case SELL_ANVIL -> handleAnvilClick(event, player, holder);
        }
    }

    private void handleMainClick(InventoryClickEvent event, Player player, SkyAuctionsHolder holder) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (slot == GuiManager.SOLD_SLOT) {
            plugin.getGuiManager().openSoldItems(player);
            return;
        }
        if (slot == GuiManager.EXPIRED_SLOT) {
            plugin.getGuiManager().openExpiredItems(player);
            return;
        }
        if (slot == GuiManager.PREV_SLOT) {
            if (holder.getPage() > 0) plugin.getGuiManager().openMain(player, holder.getPage() - 1);
            return;
        }
        if (slot == GuiManager.REFRESH_SLOT) {
            plugin.getGuiManager().openMain(player, holder.getPage());
            return;
        }
        if (slot == GuiManager.NEXT_SLOT) {
            plugin.getGuiManager().openMain(player, holder.getPage() + 1);
            return;
        }
        if (slot == GuiManager.INFO_SLOT) {
            plugin.getGuiManager().openInfo(player);
            return;
        }

        UUID auctionId = holder.getAuctionAt(slot);
        if (auctionId == null) return;

        Auction auction = plugin.getAuctionManager().get(auctionId);
        if (auction == null || auction.getStatus() != Auction.Status.ACTIVE) {
            player.sendMessage(plugin.getConfigManager().msg("buy.already-sold"));
            plugin.getGuiManager().openMain(player, holder.getPage());
            return;
        }

        if (auction.getSellerId().equals(player.getUniqueId())) {
            if (plugin.getAuctionManager().cancel(auction, player.getUniqueId(), false)) {
                player.sendMessage(plugin.getConfigManager().msg("remove.success"));
            }
            plugin.getGuiManager().openMain(player, holder.getPage());
            return;
        }

        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage(plugin.getConfigManager().msg("buy.economy-disabled"));
            return;
        }

        if (!plugin.getEconomyManager().has(player, auction.getPrice())) {
            player.sendMessage(plugin.getConfigManager().msg("buy.not-enough-money"));
            return;
        }

        boolean success = plugin.getAuctionManager().buy(player, auction);
        if (!success) {
            player.sendMessage(plugin.getConfigManager().msg("buy.inventory-full"));
            return;
        }

        String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
        player.sendMessage(plugin.getConfigManager().msg("buy.success-buyer", Map.of(
                "amount", String.valueOf(auction.getItem().getAmount()),
                "item", ItemUtil.niceName(auction.getItem()),
                "price", plugin.getEconomyManager().format(auction.getPrice()),
                "currency", currency
        )));

        Player seller = org.bukkit.Bukkit.getPlayer(auction.getSellerId());
        if (seller != null) {
            seller.sendMessage(plugin.getConfigManager().msg("buy.success-seller", Map.of(
                    "buyer", player.getName(),
                    "amount", String.valueOf(auction.getItem().getAmount()),
                    "item", ItemUtil.niceName(auction.getItem()),
                    "price", plugin.getEconomyManager().format(auction.getPendingBalance()),
                    "currency", currency
            )));
        }

        plugin.getGuiManager().openMain(player, holder.getPage());
    }

    private void handleMyListingsClick(InventoryClickEvent event, Player player, SkyAuctionsHolder holder) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (slot == 45) {
            plugin.getGuiManager().openMain(player, 0);
            return;
        }
        if (slot == 49) {
            plugin.getGuiManager().openMyListings(player);
            return;
        }

        UUID auctionId = holder.getAuctionAt(slot);
        if (auctionId == null) return;
        Auction auction = plugin.getAuctionManager().get(auctionId);
        if (auction != null && plugin.getAuctionManager().cancel(auction, player.getUniqueId(), false)) {
            player.sendMessage(plugin.getConfigManager().msg("remove.success"));
        }
        plugin.getGuiManager().openMyListings(player);
    }

    private void handleSoldClick(InventoryClickEvent event, Player player, SkyAuctionsHolder holder) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (slot == 45) {
            plugin.getGuiManager().openMain(player, 0);
            return;
        }
        if (slot == 49) {
            plugin.getGuiManager().openSoldItems(player);
            return;
        }
        if (slot == 53) return;

        UUID auctionId = holder.getAuctionAt(slot);
        if (auctionId == null) return;

        Auction auction = plugin.getAuctionManager().get(auctionId);
        double collected = plugin.getAuctionManager().collectMoney(auction, player);
        if (collected > 0) {
            String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
            player.sendMessage(plugin.getConfigManager().msg("collect.money-collected", Map.of(
                    "price", plugin.getEconomyManager().format(collected),
                    "currency", currency
            )));
        } else {
            player.sendMessage(plugin.getConfigManager().msg("collect.empty"));
        }
        plugin.getGuiManager().openSoldItems(player);
    }

    private void handleExpiredClick(InventoryClickEvent event, Player player, SkyAuctionsHolder holder) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (slot == 45) {
            plugin.getGuiManager().openMain(player, 0);
            return;
        }
        if (slot == 49) {
            plugin.getGuiManager().openExpiredItems(player);
            return;
        }
        if (slot == 53) return;

        UUID auctionId = holder.getAuctionAt(slot);
        if (auctionId == null) return;

        Auction auction = plugin.getAuctionManager().get(auctionId);
        if (plugin.getAuctionManager().collectItems(auction, player)) {
            player.sendMessage(plugin.getConfigManager().msg("collect.item-collected"));
        } else {
            player.sendMessage(plugin.getConfigManager().msg("collect.full-inventory"));
        }
        plugin.getGuiManager().openExpiredItems(player);
    }

    private void handleInfoClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() == 22) {
            plugin.getGuiManager().openMain(player, 0);
        }
    }

    private void handleCollectionClick(InventoryClickEvent event, Player player, SkyAuctionsHolder holder) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 45) {
            plugin.getGuiManager().openMain(player, 0);
            return;
        }
        if (slot == 49) {
            plugin.getGuiManager().openSoldItems(player);
        }
    }

    private void startSellFlow(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().msg("sell.hand-empty"));
            return;
        }
        ItemStack toSell = hand.clone();
        player.getInventory().setItemInMainHand(null);
        plugin.getGuiManager().openSellAnvil(player, toSell);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkyAuctionsHolder holder)
                || holder.getType() != SkyAuctionsHolder.Type.SELL_ANVIL) return;

        AnvilInventory anvil = event.getInventory();
        String renameText = anvil.getRenameText();
        Double price = parsePrice(renameText);

        if (price == null || price <= 0 || anvil.getItem(0) == null) {
            event.setResult(null);
            holder.setPendingPrice(-1);
            return;
        }

        ItemStack preview = holder.getOriginalItem().clone();
        ItemMeta meta = preview.getItemMeta();
        String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
        meta.lore(ColorUtil.colorList(List.of(
                "",
                "&#A78BFAList for &f" + plugin.getEconomyManager().format(price) + currency,
                "&eClick to confirm"
        )));
        preview.setItemMeta(meta);
        event.setResult(preview);
        holder.setPendingPrice(price);
    }

    private void handleAnvilClick(InventoryClickEvent event, Player player, SkyAuctionsHolder holder) {
        event.setCancelled(true);
        if (event.getRawSlot() != ANVIL_RESULT_SLOT) return;

        double price = holder.getPendingPrice();
        if (price <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("general.invalid-price"));
            return;
        }

        ItemStack item = holder.getOriginalItem();
        AuctionManager.ValidationResult validation = plugin.getAuctionManager().validateNewListing(player, item, price);
        if (!validation.valid()) {
            player.sendMessage(plugin.getConfigManager().msg(validation.failMessagePath(), Map.of(
                    "min", plugin.getEconomyManager().format(plugin.getConfig().getDouble("settings.min-price")),
                    "max", plugin.getEconomyManager().format(plugin.getConfig().getDouble("settings.max-price")),
                    "currency", plugin.getConfig().getString("settings.currency-symbol", "$"),
                    "current", String.valueOf(plugin.getAuctionManager().countActiveListings(player.getUniqueId())),
                    "maxlimit", String.valueOf(plugin.getAuctionManager().getMaxListingSlots(player))
            )));
            return;
        }

        plugin.getAuctionManager().createListing(player, item, price);
        holder.setConfirmed(true);
        player.closeInventory();

        String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
        player.sendMessage(plugin.getConfigManager().msg("sell.success", Map.of(
                "amount", String.valueOf(item.getAmount()),
                "item", ItemUtil.niceName(item),
                "price", plugin.getEconomyManager().format(price),
                "currency", currency
        )));

        plugin.broadcastSale(player, item, price);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkyAuctionsHolder holder)
                || holder.getType() != SkyAuctionsHolder.Type.SELL_ANVIL
                || holder.isConfirmed()
                || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (holder.getOriginalItem() != null) returnItem(player, holder.getOriginalItem());
    }

    private void returnItem(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }

    private Double parsePrice(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Double.parseDouble(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
