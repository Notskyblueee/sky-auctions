package com.skyauctions.listeners;

import com.skyauctions.SkyAuctions;
import com.skyauctions.data.Auction;
import com.skyauctions.gui.SkyAuctionsHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GuiListener implements Listener {

    private final SkyAuctions plugin;

    public GuiListener(SkyAuctions plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // INVENTORY CLICK
    // ============================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getView().getTopInventory();

        if (!(inventory.getHolder() instanceof SkyAuctionsHolder holder)) {
            return;
        }

        // Prevent taking / moving GUI items
        event.setCancelled(true);

        int slot = event.getRawSlot();

        // Click was outside the plugin GUI
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        // ========================================================
        // MAIN AUCTION HOUSE
        // ========================================================

        if (holder.getType() == SkyAuctionsHolder.Type.MAIN) {

            handleMainGuiClick(player, holder, slot);

            return;
        }

        // ========================================================
        // SOLD ITEMS
        // ========================================================

        if (holder.getType() == SkyAuctionsHolder.Type.SOLD_ITEMS) {

            handleSoldItemsClick(player, holder, slot);

            return;
        }

        // ========================================================
        // EXPIRED ITEMS
        // ========================================================

        if (holder.getType() == SkyAuctionsHolder.Type.EXPIRED_ITEMS) {

            handleExpiredItemsClick(player, holder, slot);

            return;
        }

        // ========================================================
        // MY LISTINGS
        // ========================================================

        if (holder.getType() == SkyAuctionsHolder.Type.MY_LISTINGS) {

            handleMyListingsClick(player, holder, slot);

            return;
        }

        // ========================================================
        // INFORMATION
        // ========================================================

        if (holder.getType() == SkyAuctionsHolder.Type.INFO) {

            handleInfoClick(player, slot);

            return;
        }
    }

    // ============================================================
    // MAIN GUI
    // ============================================================

    private void handleMainGuiClick(
            Player player,
            SkyAuctionsHolder holder,
            int slot
    ) {

        // --------------------------------------------------------
        // PREVIOUS PAGE
        // Slot 48 - RED SHULKER BOX
        // --------------------------------------------------------

        if (slot == com.skyauctions.gui.GuiManager.PREV_SLOT) {

            int currentPage = holder.getPage();

            if (currentPage <= 0) {
                player.sendMessage("§7You are already on the first page.");
                return;
            }

            int previousPage = currentPage - 1;

            plugin.getGuiManager().openMain(
                    player,
                    previousPage
            );

            return;
        }

        // --------------------------------------------------------
        // NEXT PAGE
        // Slot 50 - LIME SHULKER BOX
        // --------------------------------------------------------

        if (slot == com.skyauctions.gui.GuiManager.NEXT_SLOT) {

            int currentPage = holder.getPage();

            int totalListings =
                    plugin.getAuctionManager()
                            .getActiveListings()
                            .size();

            final int pageSize = 45;

            int maxPage = Math.max(
                    0,
                    (totalListings - 1) / pageSize
            );

            if (currentPage >= maxPage) {
                player.sendMessage("§7You are already on the last page.");
                return;
            }

            int nextPage = currentPage + 1;

            plugin.getGuiManager().openMain(
                    player,
                    nextPage
            );

            return;
        }

        // --------------------------------------------------------
        // REFRESH
        // Slot 49 - BELL
        // --------------------------------------------------------

        if (slot == com.skyauctions.gui.GuiManager.REFRESH_SLOT) {

            int currentPage = holder.getPage();

            plugin.getGuiManager().openMain(
                    player,
                    currentPage
            );

            return;
        }

        // --------------------------------------------------------
        // SOLD ITEMS
        // Slot 45
        // --------------------------------------------------------

        if (slot == com.skyauctions.gui.GuiManager.SOLD_SLOT) {

            if (!player.hasPermission("skyauctions.collect")) {
                player.sendMessage(
                        "§cYou don't have permission to collect auction earnings."
                );
                return;
            }

            plugin.getGuiManager().openSoldItems(player);

            return;
        }

        // --------------------------------------------------------
        // EXPIRED ITEMS
        // Slot 46
        // --------------------------------------------------------

        if (slot == com.skyauctions.gui.GuiManager.EXPIRED_SLOT) {

            if (!player.hasPermission("skyauctions.collect")) {
                player.sendMessage(
                        "§cYou don't have permission to collect auction items."
                );
                return;
            }

            plugin.getGuiManager().openExpiredItems(player);

            return;
        }

        // --------------------------------------------------------
        // INFORMATION
        // Slot 53
        // --------------------------------------------------------

        if (slot == com.skyauctions.gui.GuiManager.INFO_SLOT) {

            plugin.getGuiManager().openInfo(player);

            return;
        }

        // --------------------------------------------------------
        // LISTING CLICK
        // Slots 0-44
        // --------------------------------------------------------

        if (slot >= 0 && slot < 45) {

            String auctionId = holder.getAuctionId(slot);

            if (auctionId == null) {
                return;
            }

            handleAuctionClick(player, auctionId);

            return;
        }
    }

    // ============================================================
    // AUCTION LISTING CLICK
    // ============================================================

    private void handleAuctionClick(
            Player player,
            String auctionId
    ) {

        Auction auction =
                plugin.getAuctionManager()
                        .getAuction(auctionId);

        if (auction == null) {
            player.sendMessage(
                    "§cThis auction no longer exists."
            );

            plugin.getGuiManager().openMain(player, 0);

            return;
        }

        UUID playerId = player.getUniqueId();

        // --------------------------------------------------------
        // SELLER CLICKS OWN AUCTION
        // --------------------------------------------------------

        if (auction.getSellerId().equals(playerId)) {

            player.sendMessage(
                    "§eYou clicked your own auction listing."
            );

            return;
        }

        // --------------------------------------------------------
        // PURCHASE
        // --------------------------------------------------------

        boolean success =
                plugin.getAuctionManager()
                        .purchaseAuction(
                                auctionId,
                                player
                        );

        if (success) {

            player.sendMessage(
                    "§aYou successfully purchased the item."
            );

            plugin.getGuiManager().openMain(
                    player,
                    0
            );
        }
    }

    // ============================================================
    // SOLD ITEMS
    // ============================================================

    private void handleSoldItemsClick(
            Player player,
            SkyAuctionsHolder holder,
            int slot
    ) {

        // Back
        if (slot == 45) {

            plugin.getGuiManager().openMain(
                    player,
                    0
            );

            return;
        }

        // Refresh
        if (slot == 49) {

            plugin.getGuiManager().openSoldItems(
                    player
            );

            return;
        }

        // Item
        if (slot >= 0 && slot < 45) {

            String auctionId =
                    holder.getAuctionId(slot);

            if (auctionId == null) {
                return;
            }

            boolean success =
                    plugin.getAuctionManager()
                            .collectMoney(
                                    player,
                                    auctionId
                            );

            if (success) {

                plugin.getGuiManager()
                        .openSoldItems(player);
            }
        }
    }

    // ============================================================
    // EXPIRED / CANCELLED ITEMS
    // ============================================================

    private void handleExpiredItemsClick(
            Player player,
            SkyAuctionsHolder holder,
            int slot
    ) {

        // Back
        if (slot == 45) {

            plugin.getGuiManager().openMain(
                    player,
                    0
            );

            return;
        }

        // Refresh
        if (slot == 49) {

            plugin.getGuiManager()
                    .openExpiredItems(player);

            return;
        }

        // Item
        if (slot >= 0 && slot < 45) {

            String auctionId =
                    holder.getAuctionId(slot);

            if (auctionId == null) {
                return;
            }

            boolean success =
                    plugin.getAuctionManager()
                            .collectItem(
                                    player,
                                    auctionId
                            );

            if (success) {

                plugin.getGuiManager()
                        .openExpiredItems(player);
            }
        }
    }

    // ============================================================
    // MY LISTINGS
    // ============================================================

    private void handleMyListingsClick(
            Player player,
            SkyAuctionsHolder holder,
            int slot
    ) {

        // Back
        if (slot == 45) {

            plugin.getGuiManager().openMain(
                    player,
                    0
            );

            return;
        }

        // Refresh
        if (slot == 49) {

            plugin.getGuiManager()
                    .openMyListings(player);

            return;
        }

        // Listing
        if (slot >= 0 && slot < 45) {

            String auctionId =
                    holder.getAuctionId(slot);

            if (auctionId == null) {
                return;
            }

            Auction auction =
                    plugin.getAuctionManager()
                            .getAuction(auctionId);

            if (auction == null) {
                plugin.getGuiManager()
                        .openMyListings(player);
                return;
            }

            if (!auction.getSellerId().equals(
                    player.getUniqueId()
            )) {
                return;
            }

            player.sendMessage(
                    "§eUse §f/ah remove "
                            + auctionId
                            + " §eto cancel this listing."
            );
        }
    }

    // ============================================================
    // INFORMATION GUI
    // ============================================================

    private void handleInfoClick(
            Player player,
            int slot
    ) {

        if (slot == 22) {

            plugin.getGuiManager().openMain(
                    player,
                    0
            );
        }
    }

    // ============================================================
    // INVENTORY DRAG
    // ============================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof SkyAuctionsHolder)) {
            return;
        }

        event.setCancelled(true);
    }

    // ============================================================
    // INVENTORY CLOSE
    // ============================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder()
                instanceof SkyAuctionsHolder holder)) {
            return;
        }

        // Currently nothing needs to be done when closing
        // the main auction GUI.
    }
}
