package com.skyauctions.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Lightweight holder for the "Confirm Purchase" / "Confirm Listing" popup GUIs.
 * Kept completely separate from SkyAuctionsHolder so the existing GUI flow
 * (main menu, sold items, expired items, sell anvil, etc.) is untouched.
 */
public class ConfirmGuiHolder implements InventoryHolder {

    public enum Action {
        PURCHASE,
        SELL
    }

    private final Action action;
    private Inventory inventory;

    // Used when action == PURCHASE
    private UUID auctionId;
    private int returnPage;

    // Used when action == SELL
    private ItemStack pendingItem;
    private double pendingPrice;

    // True once the player has clicked Confirm or Cancel.
    // Used so we don't accidentally return/duplicate the item if the
    // close event fires after the click has already been handled.
    private boolean resolved;

    public ConfirmGuiHolder(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public int getReturnPage() {
        return returnPage;
    }

    public void setReturnPage(int returnPage) {
        this.returnPage = returnPage;
    }

    public ItemStack getPendingItem() {
        return pendingItem;
    }

    public void setPendingItem(ItemStack pendingItem) {
        this.pendingItem = pendingItem;
    }

    public double getPendingPrice() {
        return pendingPrice;
    }

    public void setPendingPrice(double pendingPrice) {
        this.pendingPrice = pendingPrice;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
