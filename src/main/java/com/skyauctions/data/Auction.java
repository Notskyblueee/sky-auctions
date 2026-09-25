package com.skyauctions.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {

    public enum Status {
        ACTIVE,     // listed, waiting for a buyer
        SOLD,       // bought - money/notice waiting in seller's collection box
        EXPIRED,    // ran out of time - item waiting in seller's collection box
        CANCELLED   // manually removed - item waiting in seller's collection box
    }

    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private ItemStack item;
    private double price;
    private final long listedAt;
    private final long expiresAt;
    private Status status;

    // Filled in once sold
    private UUID buyerId;
    private String buyerName;
    private double pendingBalance; // money waiting to be collected by the seller

    public Auction(UUID id, UUID sellerId, String sellerName, ItemStack item,
                   double price, long listedAt, long expiresAt, Status status) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getListedAt() {
        return listedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return status == Status.ACTIVE && System.currentTimeMillis() >= expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public double getPendingBalance() {
        return pendingBalance;
    }

    public void setPendingBalance(double pendingBalance) {
        this.pendingBalance = pendingBalance;
    }
}
