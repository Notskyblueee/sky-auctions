package com.skyauctions.gui;

import com.skyauctions.SkyAuctions;
import com.skyauctions.data.Auction;
import com.skyauctions.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 45;

    // Main GUI bottom row
    public static final int SOLD_SLOT = 45;       // Ender Chest
    public static final int EXPIRED_SLOT = 46;    // Glowstone
    public static final int PREV_SLOT = 48;       // Red Shulker
    public static final int REFRESH_SLOT = 49;    // Bell
    public static final int NEXT_SLOT = 50;       // Green Shulker
    public static final int INFO_SLOT = 53;       // Book

    private final SkyAuctions plugin;

    public GuiManager(SkyAuctions plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player viewer, int page) {
        List<Auction> active = plugin.getAuctionManager().getActiveListings();

        int maxPage = Math.max(0, (active.size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, maxPage));

        SkyAuctionsHolder holder =
                new SkyAuctionsHolder(SkyAuctionsHolder.Type.MAIN);

        String title = plugin.getConfig().getString(
                "settings.gui-title",
                "&0Auction House"
        );

        Inventory inv = Bukkit.createInventory(
                holder,
                ROWS * 9,
                ColorUtil.color(title + " &8#" + (page + 1))
        );

        holder.setInventory(inv);
        holder.setPage(page);

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            inv.setItem(slot, filler());
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, active.size());

        for (int i = start; i < end; i++) {
            Auction auction = active.get(i);

            int slot = i - start;

            inv.setItem(
                    slot,
                    buildListingIcon(auction, viewer)
            );

            holder.mapSlot(
                    slot,
                    auction.getId()
            );
        }

        fillNavigation(inv, page, maxPage);

        viewer.openInventory(inv);
    }

    private void fillNavigation(
            Inventory inv,
            int page,
            int maxPage
    ) {

        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, filler());
        }

        /*
         * END CHEST
         * Items You are Selling
         */
        inv.setItem(
                SOLD_SLOT,
                namedItem(
                        Material.ENDER_CHEST,
                        "&6Items You are Selling",
                        List.of(
                                "&aClick here to view all the items you",
                                "&aare currently selling on the auction."
                        )
                )
        );

        /*
         * GLOWSTONE
         * Collect Expired / Canceled Items
         */
        inv.setItem(
                EXPIRED_SLOT,
                namedItem(
                        Material.GLOWSTONE,
                        "&6Collect Expired / Canceled Items",
                        List.of(
                                "&aClick here to view and collect all of the",
                                "&aitems you have canceled or has expired."
                        )
                )
        );

        /*
         * PREVIOUS PAGE
         */
        inv.setItem(
                PREV_SLOT,
                namedItem(
                        Material.RED_SHULKER_BOX,
                        "&6Previous Page",
                        List.of(
                                "&7Empty"
                        )
                )
        );

        /*
         * REFRESH PAGE
         */
        inv.setItem(
                REFRESH_SLOT,
                namedItem(
                        Material.BELL,
                        "&6Refresh Page",
                        List.of()
                )
        );

        /*
         * NEXT PAGE
         */
        inv.setItem(
                NEXT_SLOT,
                namedItem(
                        Material.LIME_SHULKER_BOX,
                        "&6Next Page",
                        List.of(
                                "&7Empty"
                        )
                )
        );

        /*
         * INFORMATION BOOK
         */
        inv.setItem(
                INFO_SLOT,
                namedItem(
                        Material.BOOK,
                        "&a&lINFORMATION",
                        List.of(
                                "&fThis is the AuctionHouse, here you can",
                                "&fput items for sale, and buy items",
                                "&fthat others have put for sale.",
                                "",
                                "&fThe auction is also a great place to make",
                                "&fmoney by selling items that others",
                                "&fmay be interested in buying."
                        )
                )
        );
    }

    /*
     * AUCTION ITEM LORE
     */
    private ItemStack buildListingIcon(
            Auction auction,
            Player viewer
    ) {

        ItemStack display = auction.getItem().clone();

        ItemMeta meta = display.getItemMeta();

        List<String> loreLines = new ArrayList<>();

        String currency = plugin.getConfig().getString(
                "settings.currency-symbol",
                "$"
        );

        loreLines.add("&8&m--------------------");

        loreLines.add("&#A78BFA&lSELLER");
        loreLines.add("&f" + auction.getSellerName());

        loreLines.add("");

        loreLines.add("&#A78BFA&lPRICE");
        loreLines.add(
                "&f"
                        + plugin.getEconomyManager()
                        .format(auction.getPrice())
                        + currency
        );

        loreLines.add("&#A78BFA&lAMOUNT");
        loreLines.add("&f" + display.getAmount());

        loreLines.add("");

        loreLines.add("&#A78BFA&lTIME LEFT");
        loreLines.add(
                "&f" + formatTimeLeft(
                        auction.getExpiresAt()
                )
        );

        loreLines.add("");

        if (auction.getSellerId().equals(viewer.getUniqueId())) {

            loreLines.add(
                    "&c✦ Click to cancel your listing"
            );

        } else {

            loreLines.add(
                    "&a✦ Click to purchase"
            );
        }

        loreLines.add("&8&m--------------------");

        meta.lore(
                ColorUtil.colorList(loreLines)
        );

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES
        );

        display.setItemMeta(meta);

        return display;
    }

    /*
     * SOLD ITEMS
     */
    public void openSoldItems(Player viewer) {

        List<Auction> sold =
                plugin.getAuctionManager()
                        .getCollectableMoney(
                                viewer.getUniqueId()
                        );

        SkyAuctionsHolder holder =
                new SkyAuctionsHolder(
                        SkyAuctionsHolder.Type.SOLD_ITEMS
                );

        Inventory inv =
                Bukkit.createInventory(
                        holder,
                        54,
                        ColorUtil.color("&0Sold Items")
                );

        holder.setInventory(inv);

        for (int slot = 0; slot < 45; slot++) {
            inv.setItem(slot, filler());
        }

        for (int i = 0; i < Math.min(45, sold.size()); i++) {

            Auction auction = sold.get(i);

            ItemStack display =
                    auction.getItem().clone();

            ItemMeta meta =
                    display.getItemMeta();

            String currency =
                    plugin.getConfig().getString(
                            "settings.currency-symbol",
                            "$"
                    );

            meta.lore(
                    ColorUtil.colorList(
                            List.of(
                                    "&8&m--------------------",

                                    "&#A78BFA&lSOLD TO",

                                    "&f"
                                            + (
                                            auction.getBuyerName() == null
                                                    ? "Unknown"
                                                    : auction.getBuyerName()
                                    ),

                                    "",

                                    "&#A78BFA&lEARNED",

                                    "&f"
                                            + plugin
                                            .getEconomyManager()
                                            .format(
                                                    auction.getPendingBalance()
                                            )
                                            + currency,

                                    "",

                                    "&a✦ Click to collect",

                                    "&8&m--------------------"
                            )
                    )
            );

            display.setItemMeta(meta);

            inv.setItem(i, display);

            holder.mapSlot(
                    i,
                    auction.getId()
            );
        }

        inv.setItem(
                45,
                namedItem(
                        Material.ARROW,
                        "&7◀ Back",
                        List.of(
                                "&7Return to auction house"
                        )
                )
        );

        inv.setItem(
                49,
                namedItem(
                        Material.BELL,
                        "&b&lREFRESH",
                        List.of(
                                "&7Refresh sold items"
                        )
                )
        );

        inv.setItem(
                53,
                namedItem(
                        Material.BOOK,
                        "&a&lINFORMATION",
                        List.of(
                                "&7These are successful sales.",
                                "&7Click a sold item to collect its money."
                        )
                )
        );

        viewer.openInventory(inv);
    }

    /*
     * EXPIRED / CANCELLED ITEMS
     */
    public void openExpiredItems(Player viewer) {

        List<Auction> expired =
                plugin.getAuctionManager()
                        .getCollectableItems(
                                viewer.getUniqueId()
                        );

        SkyAuctionsHolder holder =
                new SkyAuctionsHolder(
                        SkyAuctionsHolder.Type.EXPIRED_ITEMS
                );

        Inventory inv =
                Bukkit.createInventory(
                        holder,
                        54,
                        ColorUtil.color(
                                "&0Expired & Returned Items"
                        )
                );

        holder.setInventory(inv);

        for (int slot = 0; slot < 45; slot++) {
            inv.setItem(slot, filler());
        }

        for (
                int i = 0;
                i < Math.min(45, expired.size());
                i++
        ) {

            Auction auction =
                    expired.get(i);

            ItemStack display =
                    auction.getItem().clone();

            ItemMeta meta =
                    display.getItemMeta();

            meta.lore(
                    ColorUtil.colorList(
                            List.of(
                                    "&8&m--------------------",

                                    "&#FFD166&lSTATUS",

                                    "&f"
                                            + (
                                            auction.getStatus()
                                                    == Auction.Status.EXPIRED
                                                    ? "Expired"
                                                    : "Cancelled"
                                    ),

                                    "",

                                    "&7This item is waiting to be returned to you.",

                                    "",

                                    "&a✦ Click to collect item",

                                    "&8&m--------------------"
                            )
                    )
            );

            display.setItemMeta(meta);

            inv.setItem(i, display);

            holder.mapSlot(
                    i,
                    auction.getId()
            );
        }

        inv.setItem(
                45,
                namedItem(
                        Material.ARROW,
                        "&7◀ Back",
                        List.of(
                                "&7Return to auction house"
                        )
                )
        );

        inv.setItem(
                49,
                namedItem(
                        Material.BELL,
                        "&b&lREFRESH",
                        List.of(
                                "&7Refresh returned items"
                        )
                )
        );

        inv.setItem(
                53,
                namedItem(
                        Material.BOOK,
                        "&a&lINFORMATION",
                        List.of(
                                "&7Expired or cancelled listings",
                                "&7can be collected here."
                        )
                )
        );

        viewer.openInventory(inv);
    }

    /*
     * MY ACTIVE LISTINGS
     */
    public void openMyListings(Player viewer) {

        List<Auction> mine =
                plugin.getAuctionManager()
                        .getActiveListingsOf(
                                viewer.getUniqueId()
                        );

        SkyAuctionsHolder holder =
                new SkyAuctionsHolder(
                        SkyAuctionsHolder.Type.MY_LISTINGS
                );

        Inventory inv =
                Bukkit.createInventory(
                        holder,
                        54,
                        ColorUtil.color(
                                "&0My Active Listings"
                        )
                );

        holder.setInventory(inv);

        for (int slot = 0; slot < 45; slot++) {
            inv.setItem(slot, filler());
        }

        for (
                int i = 0;
                i < Math.min(45, mine.size());
                i++
        ) {

            Auction auction =
                    mine.get(i);

            inv.setItem(
                    i,
                    buildListingIcon(
                            auction,
                            viewer
                    )
            );

            holder.mapSlot(
                    i,
                    auction.getId()
            );
        }

        inv.setItem(
                45,
                namedItem(
                        Material.ARROW,
                        "&7◀ Back",
                        List.of(
                                "&7Return to auction house"
                        )
                )
        );

        inv.setItem(
                49,
                namedItem(
                        Material.BELL,
                        "&b&lREFRESH",
                        List.of(
                                "&7Refresh your listings"
                        )
                )
        );

        inv.setItem(
                53,
                namedItem(
                        Material.BOOK,
                        "&a&lINFORMATION",
                        List.of(
                                "&7Your active auction listings.",
                                "&7Click a listing to cancel it."
                        )
                )
        );

        viewer.openInventory(inv);
    }

    /*
     * INFORMATION GUI
     */
    public void openInfo(Player viewer) {

        SkyAuctionsHolder holder =
                new SkyAuctionsHolder(
                        SkyAuctionsHolder.Type.INFO
                );

        Inventory inv =
                Bukkit.createInventory(
                        holder,
                        27,
                        ColorUtil.color(
                                "&0Auction Information"
                        )
                );

        holder.setInventory(inv);

        for (int slot = 0; slot < 27; slot++) {
            inv.setItem(slot, filler());
        }

        inv.setItem(
                10,
                namedItem(
                        Material.PAPER,
                        "&a&lHOW IT WORKS",
                        List.of(
                                "&7Hold an item and use:",
                                "&f/ah sell <price>",
                                "",
                                "&7Your item is listed for other players.",
                                "&7Click a listing to purchase it."
                        )
                )
        );

        inv.setItem(
                12,
                namedItem(
                        Material.ENDER_CHEST,
                        "&d&lSOLD ITEMS",
                        List.of(
                                "&7Successful sales are stored here.",
                                "&7Collect your earnings anytime."
                        )
                )
        );

        inv.setItem(
                14,
                namedItem(
                        Material.GLOWSTONE,
                        "&e&lEXPIRED ITEMS",
                        List.of(
                                "&7Expired/cancelled items are returned",
                                "&7here until you collect them."
                        )
                )
        );

        inv.setItem(
                16,
                namedItem(
                        Material.CHEST,
                        "&b&lYOUR LISTINGS",
                        List.of(
                                "&7Use &f/ah listed &7to view them."
                        )
                )
        );

        inv.setItem(
                22,
                namedItem(
                        Material.ARROW,
                        "&7◀ Back",
                        List.of(
                                "&7Return to auction house"
                        )
                )
        );

        viewer.openInventory(inv);
    }

    /*
     * SELL PRICE ANVIL
     */
    public void openSellAnvil(
            Player player,
            ItemStack itemToSell
    ) {

        SkyAuctionsHolder holder =
                new SkyAuctionsHolder(
                        SkyAuctionsHolder.Type.SELL_ANVIL
                );

        Inventory inv =
                Bukkit.createInventory(
                        holder,
                        org.bukkit.event.inventory.InventoryType.ANVIL,
                        ColorUtil.color(
                                "&0Enter Auction Price"
                        )
                );

        holder.setInventory(inv);

        holder.setOriginalItem(
                itemToSell.clone()
        );

        inv.setItem(
                0,
                itemToSell.clone()
        );

        player.openInventory(inv);
    }

    /*
     * CREATE GUI ITEM
     */
    private ItemStack namedItem(
            Material material,
            String name,
            List<String> lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        meta.displayName(
                ColorUtil.color(name)
        );

        if (!lore.isEmpty()) {
            meta.lore(
                    ColorUtil.colorList(lore)
            );
        }

        item.setItemMeta(meta);

        return item;
    }

    /*
     * GUI BACKGROUND
     */
    private ItemStack filler() {

        ItemStack item =
                new ItemStack(
                        Material.GRAY_STAINED_GLASS_PANE
                );

        ItemMeta meta =
                item.getItemMeta();

        meta.displayName(
                Component.text(" ")
        );

        item.setItemMeta(meta);

        return item;
    }

    /*
     * FORMAT AUCTION TIME
     */
    private String formatTimeLeft(
            long expiresAt
    ) {

        long seconds =
                Math.max(
                        0,
                        (
                                expiresAt
                                        - System.currentTimeMillis()
                        ) / 1000L
                );

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }
}
