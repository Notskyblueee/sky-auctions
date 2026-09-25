package com.skyauctions.commands;

import com.skyauctions.SkyAuctions;
import com.skyauctions.data.Auction;
import com.skyauctions.data.AuctionManager;
import com.skyauctions.util.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkyAuctionsCommand implements CommandExecutor, TabCompleter {

    private final SkyAuctions plugin;

    public SkyAuctionsCommand(SkyAuctions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
                return true;
            }
            if (!player.hasPermission("skyauctions.use")) {
                player.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
                return true;
            }
            plugin.getGuiManager().openMain(player, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(sender);
            case "sell" -> handleSell(sender, args);
            case "listed", "my" -> handleMy(sender);
            case "collect" -> handleCollect(sender);
            case "expired" -> handleExpired(sender);
            case "remove" -> handleRemove(sender, args);
            case "reload" -> handleReload(sender);
            case "force_end_all" -> handleForceEndAll(sender);
            case "bid" -> handleBid(sender, args);
            case "view" -> handleView(sender, args);
            case "test" -> handleTest(sender);
            default -> sender.sendMessage(plugin.getConfigManager().msg("general.unknown-command"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().msg("help.header"));
        for (String path : List.of(
                "help.line-open",
                "help.line-sell",
                "help.line-listed",
                "help.line-collect",
                "help.line-expired",
                "help.line-bid",
                "help.line-view"
        )) {
            sender.sendMessage(plugin.getConfigManager().msg(path));
        }
        if (sender.hasPermission("skyauctions.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().msg("help.line-reload"));
        }
        if (sender.hasPermission("skyauctions.admin.force-end")) {
            sender.sendMessage(plugin.getConfigManager().msg("help.line-force-end"));
        }
        if (sender.hasPermission("skyauctions.admin.test")) {
            sender.sendMessage(plugin.getConfigManager().msg("help.line-test"));
        }
    }

    private void handleSell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (!player.hasPermission("skyauctions.sell")) {
            player.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().msg("sell.usage"));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1].replace(",", ""));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().msg("general.invalid-price"));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        AuctionManager.ValidationResult validation = plugin.getAuctionManager().validateNewListing(player, hand, price);
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

        ItemStack toList = hand.clone();
        player.getInventory().setItemInMainHand(null);
        plugin.getAuctionManager().createListing(player, toList, price);

        String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
        player.sendMessage(plugin.getConfigManager().msg("sell.success", Map.of(
                "amount", String.valueOf(toList.getAmount()),
                "item", ItemUtil.niceName(toList),
                "price", plugin.getEconomyManager().format(price),
                "currency", currency
        )));
        plugin.broadcastSale(player, toList, price);
    }

    private void handleMy(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        plugin.getGuiManager().openMyListings(player);
    }

    private void handleCollect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (!player.hasPermission("skyauctions.collect")) {
            player.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
            return;
        }

        double money = plugin.getAuctionManager().collectMoney(player);
        int items = plugin.getAuctionManager().collectItems(player);

        String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
        if (money > 0) {
            player.sendMessage(plugin.getConfigManager().msg("collect.money-collected", Map.of(
                    "price", plugin.getEconomyManager().format(money),
                    "currency", currency
            )));
        }
        if (items > 0) {
            player.sendMessage(plugin.getConfigManager().msg("collect.items-collected",
                    Map.of("amount", String.valueOf(items))));
        }
        if (money <= 0 && items <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("collect.empty"));
        }
    }

    private void handleExpired(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (!player.hasPermission("skyauctions.collect")) {
            player.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
            return;
        }
        plugin.getGuiManager().openExpiredItems(player);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().msg("remove.usage"));
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfigManager().msg("remove.not-found"));
            return;
        }
        Auction auction = plugin.getAuctionManager().get(id);
        if (auction == null) {
            player.sendMessage(plugin.getConfigManager().msg("remove.not-found"));
            return;
        }
        boolean isAdmin = player.hasPermission("skyauctions.admin.remove.others");
        if (!isAdmin && !auction.getSellerId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().msg("remove.not-yours"));
            return;
        }
        if (plugin.getAuctionManager().cancel(auction, player.getUniqueId(), isAdmin)) {
            player.sendMessage(plugin.getConfigManager().msg("remove.success"));
        } else {
            player.sendMessage(plugin.getConfigManager().msg("remove.not-found"));
        }
    }

    private void handleBid(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().msg("bid.usage"));
            return;
        }

        Auction auction;
        try {
            auction = plugin.getAuctionManager().get(UUID.fromString(args[1]));
        } catch (IllegalArgumentException e) {
            auction = null;
        }

        if (auction == null || auction.getStatus() != Auction.Status.ACTIVE) {
            player.sendMessage(plugin.getConfigManager().msg("buy.already-sold"));
            return;
        }
        if (auction.getSellerId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().msg("buy.cannot-buy-own"));
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

        if (!plugin.getAuctionManager().buy(player, auction)) {
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
    }

    private void handleView(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (args.length < 2) {
            plugin.getGuiManager().openMain(player, 0);
            return;
        }
        try {
            Auction auction = plugin.getAuctionManager().get(UUID.fromString(args[1]));
            if (auction == null || auction.getStatus() != Auction.Status.ACTIVE) {
                player.sendMessage(plugin.getConfigManager().msg("buy.already-sold"));
                return;
            }
            String currency = plugin.getConfig().getString("settings.currency-symbol", "$");
            player.sendMessage(plugin.getConfigManager().msg("view.details", Map.of(
                    "item", ItemUtil.niceName(auction.getItem()),
                    "seller", auction.getSellerName(),
                    "price", plugin.getEconomyManager().format(auction.getPrice()),
                    "currency", currency,
                    "id", auction.getId().toString()
            )));
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfigManager().msg("remove.not-found"));
        }
    }

    private void handleForceEndAll(CommandSender sender) {
        if (!sender.hasPermission("skyauctions.admin.force-end")) {
            sender.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
            return;
        }
        int ended = plugin.getAuctionManager().forceExpireAll();
        sender.sendMessage(plugin.getConfigManager().msg("admin.force-end", Map.of(
                "amount", String.valueOf(ended)
        )));
    }

    private void handleTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().msg("general.player-only"));
            return;
        }
        if (!player.hasPermission("skyauctions.admin.test")) {
            player.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
            return;
        }
        player.sendActionBar(plugin.getConfigManager().msg("test.actionbar"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("skyauctions.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().msg("general.no-permission"));
            return;
        }
        plugin.getConfigManager().reload();
        sender.sendMessage(plugin.getConfigManager().msg("general.reloaded"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = List.of(
                    "help", "sell", "bid", "collect", "expired", "force_end_all",
                    "listed", "view", "reload", "remove", "test"
            );
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) options.add(sub);
            }
        }
        return options;
    }
}
