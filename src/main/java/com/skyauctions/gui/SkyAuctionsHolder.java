package com.skyauctions.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A single holder class used for every SkyAuctions inventory so the
 * listener can tell which GUI (and which page / mapped auction) a
 * click happened in.
 */
public class SkyAuctionsHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        MY_LISTINGS,
        COLLECTION,
        SOLD_ITEMS,
        EXPIRED_ITEMS,
        INFO,
        SELL_ANVIL
    }

    private final Type type;
    private Inventory inventory;
    private int page = 0;

    // MAIN / MY_LISTINGS: maps a slot number -> the auction shown there
    private final Map<Integer, UUID> slotToAuction = new HashMap<>();

    // SELL_ANVIL only
    private ItemStack originalItem;
    private boolean confirmed = false;
    private double pendingPrice = -1;

    public SkyAuctionsHolder(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void mapSlot(int slot, UUID auctionId) {
        slotToAuction.put(slot, auctionId);
    }

    public UUID getAuctionAt(int slot) {
        return slotToAuction.get(slot);
    }

    public void clearMappings() {
        slotToAuction.clear();
    }

    public ItemStack getOriginalItem() {
        return originalItem;
    }

    public void setOriginalItem(ItemStack originalItem) {
        this.originalItem = originalItem;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public double getPendingPrice() {
        return pendingPrice;
    }

    public void setPendingPrice(double pendingPrice) {
        this.pendingPrice = pendingPrice;
    }
}
