package com.skyauctions.data;

import com.skyauctions.SkyAuctions;
import com.skyauctions.util.ItemUtil;
import com.skyauctions.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private final SkyAuctions plugin;
    private final File file;
    private final Map<UUID, Auction> auctions = new ConcurrentHashMap<>();

    public AuctionManager(SkyAuctions plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auctions.yml");
        load();
    }

    // ---------------------------------------------------------------
    //  Persistence
    // ---------------------------------------------------------------

    public synchronized void load() {
        if (!file.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("auctions");
        if (section == null) {
            return;
        }
        auctions.clear();
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String path = "auctions." + key + ".";
                UUID seller = UUID.fromString(data.getString(path + "seller"));
                String sellerName = data.getString(path + "seller-name", "Unknown");
                ItemStack item = ItemUtil.fromBase64(data.getString(path + "item"));
                double price = data.getDouble(path + "price");
                long listedAt = data.getLong(path + "listed-at");
                long expiresAt = data.getLong(path + "expires-at");
                Auction.Status status = Auction.Status.valueOf(data.getString(path + "status", "ACTIVE"));

                Auction auction = new Auction(id, seller, sellerName, item, price, listedAt, expiresAt, status);
                if (data.contains(path + "buyer")) {
                    auction.setBuyerId(UUID.fromString(data.getString(path + "buyer")));
                    auction.setBuyerName(data.getString(path + "buyer-name"));
                }
                auction.setPendingBalance(data.getDouble(path + "pending-balance", 0));
                auctions.put(id, auction);
            } catch (Exception e) {
                plugin.getLogger().warning("Skipped a corrupt auction entry: " + key);
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Auction auction : auctions.values()) {
            String path = "auctions." + auction.getId() + ".";
            data.set(path + "seller", auction.getSellerId().toString());
            data.set(path + "seller-name", auction.getSellerName());
            data.set(path + "item", ItemUtil.toBase64(auction.getItem()));
            data.set(path + "price", auction.getPrice());
            data.set(path + "listed-at", auction.getListedAt());
            data.set(path + "expires-at", auction.getExpiresAt());
            data.set(path + "status", auction.getStatus().name());
            if (auction.getBuyerId() != null) {
                data.set(path + "buyer", auction.getBuyerId().toString());
                data.set(path + "buyer-name", auction.getBuyerName());
            }
            data.set(path + "pending-balance", auction.getPendingBalance());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save auctions.yml: " + e.getMessage());
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    // ---------------------------------------------------------------
    //  Queries
    // ---------------------------------------------------------------

    public List<Auction> getActiveListings() {
        List<Auction> list = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == Auction.Status.ACTIVE) {
                list.add(a);
            }
        }
        list.sort(Comparator.comparingLong(Auction::getListedAt).reversed());
        return list;
    }

    public List<Auction> getActiveListingsOf(UUID seller) {
        List<Auction> list = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == Auction.Status.ACTIVE && a.getSellerId().equals(seller)) {
                list.add(a);
            }
        }
        list.sort(Comparator.comparingLong(Auction::getListedAt).reversed());
        return list;
    }

    /** Auctions with an item still waiting to be collected (expired/cancelled). */
    public List<Auction> getCollectableItems(UUID player) {
        List<Auction> list = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getSellerId().equals(player)
                    && (a.getStatus() == Auction.Status.EXPIRED || a.getStatus() == Auction.Status.CANCELLED)) {
                list.add(a);
            }
        }
        return list;
    }

    /** Auctions that sold, with money still waiting to be collected. */
    public List<Auction> getCollectableMoney(UUID player) {
        List<Auction> list = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getSellerId().equals(player) && a.getStatus() == Auction.Status.SOLD) {
                list.add(a);
            }
        }
        return list;
    }

    public int countActiveListings(UUID player) {
        return getActiveListingsOf(player).size();
    }

    public Auction get(UUID id) {
        return auctions.get(id);
    }

    // ---------------------------------------------------------------
    //  Validation
    // ---------------------------------------------------------------

    /** Simple pass/fail result carrying a messages.yml path to show on failure. */
    public record ValidationResult(boolean valid, String failMessagePath) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String path) {
            return new ValidationResult(false, path);
        }
    }

    public ValidationResult validateNewListing(Player player, ItemStack item, double price) {
        if (item == null || item.getType().isAir()) {
            return ValidationResult.fail("sell.hand-empty");
        }
        List<String> blocked = plugin.getConfig().getStringList("blocked-items");
        if (blocked.contains(item.getType().name())) {
            return ValidationResult.fail("sell.item-blocked");
        }
        if (!player.hasPermission("skyauctions.bypass.price")) {
            double min = plugin.getConfig().getDouble("settings.min-price");
            double max = plugin.getConfig().getDouble("settings.max-price");
            if (price < min) {
                return ValidationResult.fail("sell.price-too-low");
            }
            if (price > max) {
                return ValidationResult.fail("sell.price-too-high");
            }
        }
        if (!player.hasPermission("skyauctions.bypass.limit")) {
            int limit = getMaxListingSlots(player);
            if (countActiveListings(player.getUniqueId()) >= limit) {
                return ValidationResult.fail("sell.limit-reached");
            }
        }
        return ValidationResult.ok();
    }

    public int getMaxListingSlots(Player player) {
        int fallback = plugin.getConfig().getInt("settings.default-listing-limit", 5);
        return PermissionUtil.getMaxListingSlots(player, fallback);
    }

    // ---------------------------------------------------------------
    //  Mutations
    // ---------------------------------------------------------------

    public Auction createListing(Player seller, ItemStack item, double price) {
        long now = System.currentTimeMillis();
        long durationMs = plugin.getConfig().getLong("settings.listing-duration-hours", 48) * 3600_000L;
        Auction auction = new Auction(
                UUID.randomUUID(), seller.getUniqueId(), seller.getName(),
                item, price, now, now + durationMs, Auction.Status.ACTIVE
        );
        auctions.put(auction.getId(), auction);
        saveAsync();
        return auction;
    }

    /**
     * Attempts to sell the listing to the buyer. Returns true on success.
     * Handles economy transfer + tax and gives the item directly to the
     * buyer if they have space; the seller must collect the money via
     * the collection box.
     */
    public boolean buy(Player buyer, Auction auction) {
        if (auction.getStatus() != Auction.Status.ACTIVE) {
            return false;
        }
        if (auction.getSellerId().equals(buyer.getUniqueId())) {
            return false;
        }
        if (!plugin.getEconomyManager().has(buyer, auction.getPrice())) {
            return false;
        }
        if (!hasRoomFor(buyer, auction.getItem())) {
            return false;
        }

        plugin.getEconomyManager().withdraw(buyer, auction.getPrice());

        double tax = 0;
        Player sellerOnline = Bukkit.getPlayer(auction.getSellerId());
        boolean sellerBypassesTax = sellerOnline != null && sellerOnline.hasPermission("skyauctions.bypass.tax");
        if (!sellerBypassesTax) {
            double taxPercent = plugin.getConfig().getDouble("settings.sale-tax-percent", 0);
            tax = auction.getPrice() * (taxPercent / 100.0);
        }

        buyer.getInventory().addItem(auction.getItem());

        auction.setStatus(Auction.Status.SOLD);
        auction.setBuyerId(buyer.getUniqueId());
        auction.setBuyerName(buyer.getName());
        auction.setPendingBalance(auction.getPrice() - tax);

        saveAsync();
        return true;
    }

    /** Force-expire every active listing. Returns the number changed. */
    public int forceExpireAll() {
        int changed = 0;
        for (Auction auction : auctions.values()) {
            if (auction.getStatus() == Auction.Status.ACTIVE) {
                auction.setStatus(Auction.Status.EXPIRED);
                changed++;
            }
        }
        if (changed > 0) saveAsync();
        return changed;
    }

    public boolean cancel(Auction auction, UUID requester, boolean isAdmin) {
        if (auction.getStatus() != Auction.Status.ACTIVE) {
            return false;
        }
        if (!isAdmin && !auction.getSellerId().equals(requester)) {
            return false;
        }
        auction.setStatus(Auction.Status.CANCELLED);
        saveAsync();
        return true;
    }

    /** Collect one sold listing's pending balance. Returns the amount collected, or 0 if unavailable. */
    public double collectMoney(Auction auction, Player player) {
        if (auction == null
                || auction.getStatus() != Auction.Status.SOLD
                || !auction.getSellerId().equals(player.getUniqueId())) {
            return 0;
        }
        double amount = auction.getPendingBalance();
        if (amount <= 0 || !plugin.getEconomyManager().isEnabled()) {
            return 0;
        }
        if (!plugin.getEconomyManager().deposit(player, amount)) {
            return 0;
        }
        auctions.remove(auction.getId());
        saveAsync();
        return amount;
    }

    /** Return one expired/cancelled listing's item. */
    public boolean collectItems(Auction auction, Player player) {
        if (auction == null
                || (auction.getStatus() != Auction.Status.EXPIRED
                    && auction.getStatus() != Auction.Status.CANCELLED)
                || !auction.getSellerId().equals(player.getUniqueId())) {
            return false;
        }
        if (!hasRoomFor(player, auction.getItem())) {
            return false;
        }
        player.getInventory().addItem(auction.getItem());
        auctions.remove(auction.getId());
        saveAsync();
        return true;
    }

    /** Collects all pending money for a player, depositing it and clearing the entries. */
    public double collectMoney(Player player) {
        double total = 0;
        List<Auction> toRemove = new ArrayList<>();
        for (Auction a : getCollectableMoney(player.getUniqueId())) {
            total += a.getPendingBalance();
            toRemove.add(a);
        }
        if (total > 0) {
            plugin.getEconomyManager().deposit(player, total);
            for (Auction a : toRemove) {
                auctions.remove(a.getId());
            }
            saveAsync();
        }
        return total;
    }

    /** Collects all returnable items (expired/cancelled) into the player's inventory, if space allows. */
    public int collectItems(Player player) {
        int collected = 0;
        for (Auction a : new ArrayList<>(getCollectableItems(player.getUniqueId()))) {
            if (hasRoomFor(player, a.getItem())) {
                player.getInventory().addItem(a.getItem());
                auctions.remove(a.getId());
                collected++;
            }
        }
        if (collected > 0) {
            saveAsync();
        }
        return collected;
    }

    /** Runs periodically: flips ACTIVE listings whose time is up into EXPIRED. */
    public void processExpirations() {
        boolean changed = false;
        for (Auction a : auctions.values()) {
            if (a.isExpired()) {
                a.setStatus(Auction.Status.EXPIRED);
                changed = true;
                Player seller = Bukkit.getPlayer(a.getSellerId());
                if (seller != null) {
                    seller.sendMessage(plugin.getConfigManager().msg("expire.expired-notice",
                            Map.of("item", ItemUtil.niceName(a.getItem()))));
                }
            }
        }
        if (changed) {
            saveAsync();
        }
    }

    /** Non-mutating check: does the player have space for this item (empty slot or a stackable match)? */
    private boolean hasRoomFor(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            return true;
        }
        for (ItemStack existing : player.getInventory().getStorageContents()) {
            if (existing != null && existing.isSimilar(item) && existing.getAmount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
}
