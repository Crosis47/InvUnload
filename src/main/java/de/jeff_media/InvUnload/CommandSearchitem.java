package de.jeff_media.InvUnload;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.jeff_media.InvUnload.utils.NumberUtils;

public class CommandSearchitem implements CommandExecutor {

    private static final long TAKE_REQUEST_TTL_MILLIS = 2 * 60 * 1000;

    private final Main main;
    private final HashMap<UUID, HashMap<String, SearchTakeRequest>> takeRequests = new HashMap<>();

    CommandSearchitem(Main main) {
        this.main=main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if(!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to run this command.");
            return true;
        }

        Player p = (Player) sender;

        if(command.getName().equalsIgnoreCase("searchtake")) {
            return handleTakeCommand(p, args.length == 1 ? args[0] : null);
        }

        if(args.length > 0 && args[0].equalsIgnoreCase("take")) {
            return handleTakeCommand(p, args.length == 2 ? args[1] : null);
        }

        SearchRequest searchRequest = parseSearchRequest(p, args);

        if(!searchRequest.isValid()) {
            p.sendMessage(searchRequest.error);
            return true;
        }

        int radius = searchRequest.radius;
        SearchItemQuery query = searchRequest.query;

        int maxSearchRadius = main.groupUtils.getMaxSearchRadiusPerPlayer(p);
        if (radius > maxSearchRadius) {
            p.sendMessage(String.format(main.messages.MSG_RADIUS_TOO_HIGH, maxSearchRadius));
            return true;
        }

        List<Block> chests = BlockUtils.findChestsInRadius(p.getLocation(), radius);
        BlockUtils.sortBlockListByDistance(chests, p.getLocation());

        ArrayList<Block> useableChests = new ArrayList<>();
        for(Block block : chests) {
            if(PlayerUtils.canPlayerUseChest(block, p, main)) {
                useableChests.add(block);
            }
        }

        if(useableChests.size()==0) {
            p.sendMessage(String.format(main.messages.MSG_NOTHING_FOUND, query.getDescription()));
            return true;
        }

        chests = null;

        ArrayList<Block> affectedChests = new ArrayList<>();
        ArrayList<InventoryHolder> doubleChests = new ArrayList<>();
        UnloadSummary summary = new UnloadSummary();
        for(Block block : useableChests) {

            Inventory inv = ((Container) block.getState()).getInventory();

            if(inv.getHolder() instanceof DoubleChest) {
                DoubleChest dc = (DoubleChest) inv.getHolder();
                if(doubleChests.contains(dc.getLeftSide())) continue;
                doubleChests.add(dc.getLeftSide());
            }

            boolean found = query.hasEnchantmentFilter()
                    ? InvUtils.searchItemInContainers(query, inv, block.getLocation(), summary, this, p)
                    : InvUtils.searchItemInContainers(query.getMaterial(), inv, block.getLocation(), summary, this, p);

            if(found) {
                affectedChests.add(block);
            }
        }

        summary.print(UnloadSummary.PrintRecipient.PLAYER, p);

        if(affectedChests.size()==0) {
            p.sendMessage(String.format(main.messages.MSG_NOTHING_FOUND, query.getDescription()));
            return true;
        }

        main.visualizer.save(p, affectedChests, summary);
        
        for(Block block : affectedChests) {
            main.visualizer.chestAnimation(block,p);
        }
        main.visualizer.play(affectedChests, p);

        return true;
    }

    String createTakeCommand(Player player, Location containerLocation, int slot, ItemStack item) {
        if(!main.getConfig().getBoolean("search-take-links", true)) return null;
        if(!player.hasPermission("invunload.search.take")) return null;

        UUID playerId = player.getUniqueId();
        cleanupTakeRequests(playerId);

        String token = UUID.randomUUID().toString();
        takeRequests.computeIfAbsent(playerId, key -> new HashMap<>())
                .put(token, new SearchTakeRequest(containerLocation, slot, item));
        return "/searchtake " + token;
    }

    private boolean handleTakeCommand(Player player, String token) {
        if(!main.getConfig().getBoolean("search-take-links", true)) {
            player.sendMessage("Taking items from search results is disabled.");
            return true;
        }
        if(!player.hasPermission("invunload.search.take")) {
            String permissionMessage = main.getCommand("searchitem").getPermissionMessage();
            player.sendMessage(permissionMessage == null ? "You do not have permission to take items from search results." : permissionMessage);
            return true;
        }
        if(token == null) {
            player.sendMessage("That search result is no longer available.");
            return true;
        }

        UUID playerId = player.getUniqueId();
        cleanupTakeRequests(playerId);

        HashMap<String, SearchTakeRequest> requests = takeRequests.get(playerId);
        SearchTakeRequest request = requests == null ? null : requests.get(token);
        if(request == null) {
            player.sendMessage("That search result is no longer available.");
            return true;
        }

        Block block = request.containerLocation.getBlock();
        BlockState state = block.getState();
        if(!(state instanceof Container)) {
            requests.remove(token);
            player.sendMessage("That chest is no longer available.");
            return true;
        }
        int takeRadius = main.groupUtils.getSearchTakeRadiusPerPlayer(player);
        if(!isWithinTakeRadius(player, request.containerLocation, takeRadius)) {
            player.sendMessage(String.format("You are too far away from that chest. Move within %d blocks and try again.", takeRadius));
            return true;
        }
        if(!PlayerUtils.canPlayerUseChest(block, player, main)) {
            player.sendMessage("You cannot access that chest.");
            return true;
        }

        Inventory inventory = ((Container) state).getInventory();
        if(request.slot < 0 || request.slot >= inventory.getSize()) {
            requests.remove(token);
            player.sendMessage("That chest slot is no longer available.");
            return true;
        }

        ItemStack current = inventory.getItem(request.slot);
        if(current == null || current.getType() == Material.AIR || !current.isSimilar(request.snapshot)) {
            requests.remove(token);
            player.sendMessage("That item is no longer in the reported slot.");
            return true;
        }

        int startingAmount = current.getAmount();
        ItemStack moving = current.clone();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(moving);
        int leftoverAmount = countLeftovers(leftovers);
        int movedAmount = startingAmount - leftoverAmount;

        if(movedAmount <= 0) {
            player.sendMessage("Your inventory is full.");
            return true;
        }

        if(leftoverAmount == 0) {
            inventory.clear(request.slot);
        } else {
            ItemStack remaining = current.clone();
            remaining.setAmount(leftoverAmount);
            inventory.setItem(request.slot, remaining);
        }

        main.coreProtectHook.logCoreProtect(player.getName(), inventory);
        requests.remove(token);
        player.updateInventory();
        player.sendMessage(String.format("Took %dx %s from chest slot %d.", movedAmount, current.getType().name(), request.slot + 1));
        return true;
    }

    private boolean isWithinTakeRadius(Player player, Location containerLocation, int takeRadius) {
        if(player.getWorld() == null || containerLocation.getWorld() == null) return false;
        if(!player.getWorld().equals(containerLocation.getWorld())) return false;
        if(takeRadius < 0) return true;
        return player.getLocation().distanceSquared(containerLocation) <= (double) takeRadius * takeRadius;
    }

    private int countLeftovers(Map<Integer, ItemStack> leftovers) {
        int amount = 0;
        for(ItemStack leftover : leftovers.values()) {
            if(leftover != null) amount += leftover.getAmount();
        }
        return amount;
    }

    private void cleanupTakeRequests(UUID playerId) {
        HashMap<String, SearchTakeRequest> requests = takeRequests.get(playerId);
        if(requests == null) return;

        long now = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> now - entry.getValue().createdAt > TAKE_REQUEST_TTL_MILLIS);
        if(requests.isEmpty()) {
            takeRequests.remove(playerId);
        }
    }

    private SearchRequest parseSearchRequest(Player player, String[] args) {
        Integer radius = null;
        Material material = null;
        Enchantment enchantment = null;
        boolean expectingEnchantment = false;

        for(String arg : args) {
            if(NumberUtils.isPositiveInteger(arg)) {
                if(radius != null) {
                    return SearchRequest.invalid("Invalid radius.");
                }
                radius = Integer.parseInt(arg);
                continue;
            }

            if(isEnchantmentKeyword(arg)) {
                expectingEnchantment = true;
                continue;
            }

            if(expectingEnchantment) {
                Enchantment parsedEnchantment = SearchItemQuery.parseEnchantment(arg);
                if(parsedEnchantment == null) {
                    return SearchRequest.invalid(String.format("%s is not a valid enchantment.", arg));
                }
                if(enchantment != null) {
                    return SearchRequest.invalid("You can only search for one enchantment at a time.");
                }
                enchantment = parsedEnchantment;
                expectingEnchantment = false;
                continue;
            }

            Material parsedMaterial = Material.getMaterial(arg.toUpperCase(Locale.ROOT));
            if(parsedMaterial != null) {
                if(material != null) {
                    return SearchRequest.invalid("You can only search for one material at a time.");
                }
                material = parsedMaterial;
                continue;
            }

            Enchantment parsedEnchantment = SearchItemQuery.parseEnchantment(arg);
            if(parsedEnchantment != null) {
                if(enchantment != null) {
                    return SearchRequest.invalid("You can only search for one enchantment at a time.");
                }
                enchantment = parsedEnchantment;
                continue;
            }

            return SearchRequest.invalid(String.format("%s is not a valid material or enchantment.", arg));
        }

        if(expectingEnchantment) {
            return SearchRequest.invalid("You must specify a valid enchantment.");
        }

        if(radius == null) {
            radius = main.groupUtils.getDefaultSearchRadiusPerPlayer(player);
        }

        if(material == null && enchantment == null) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if(hand != null && hand.getType() != null && hand.getType() != Material.AIR) {
                material = hand.getType();
            }
        }

        if(material == null && enchantment == null) {
            return SearchRequest.invalid("You must specify a valid material, enchantment, or hold something in your hand.");
        }

        SearchItemQuery query;
        if(enchantment == null) {
            query = SearchItemQuery.material(material);
        } else if(material == null) {
            query = SearchItemQuery.enchantment(enchantment);
        } else {
            query = SearchItemQuery.materialAndEnchantment(material, enchantment);
        }

        return SearchRequest.valid(query, radius);
    }

    private boolean isEnchantmentKeyword(String input) {
        String normalized = SearchItemQuery.normalize(input);
        return normalized.equals("enchant") || normalized.equals("enchantment") || normalized.equals("enchanted");
    }

    private static class SearchRequest {
        private final SearchItemQuery query;
        private final int radius;
        private final String error;

        private SearchRequest(SearchItemQuery query, int radius, String error) {
            this.query = query;
            this.radius = radius;
            this.error = error;
        }

        private static SearchRequest valid(SearchItemQuery query, int radius) {
            return new SearchRequest(query, radius, null);
        }

        private static SearchRequest invalid(String error) {
            return new SearchRequest(null, 0, error);
        }

        private boolean isValid() {
            return error == null;
        }
    }

    private static class SearchTakeRequest {
        private final Location containerLocation;
        private final int slot;
        private final ItemStack snapshot;
        private final long createdAt;

        private SearchTakeRequest(Location containerLocation, int slot, ItemStack item) {
            this.containerLocation = containerLocation.clone();
            this.slot = slot;
            this.snapshot = item.clone();
            this.createdAt = System.currentTimeMillis();
        }
    }
}
