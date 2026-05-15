package de.jeff_media.InvUnload;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaterialTabCompleter implements TabCompleter {

    final Main main;

    final ArrayList<String> mats;

    MaterialTabCompleter(Main main) {
        this.main = main;

        mats = new ArrayList<>();
        for(Material mat : Material.values()) {
            mats.add(mat.name());
        }
        mats.add("enchant");
        mats.add("enchantment");
        mats.add("inv");
        mats.add("inventory");
        mats.add("hotbar");
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if(args.length==0) return null;

        ArrayList<String> results = new ArrayList<>();
        String lastArg = args[args.length-1];
        String normalizedLastArg = lastArg.toUpperCase(Locale.ROOT);
        List<String> candidates = getCandidates(args);

        for(String mat : candidates) {
            String normalizedMat = mat.toUpperCase(Locale.ROOT);
            if(main.getConfig().getBoolean("strict-tabcomplete")) {
                if(normalizedMat.startsWith(normalizedLastArg)) results.add(mat);
            } else {
                if (normalizedMat.contains(normalizedLastArg)) results.add(mat);
            }
        }

        return results;
    }

    private List<String> getCandidates(String[] args) {
        ArrayList<String> enchantments = SearchItemQuery.getEnchantmentNames();
        if(args.length >= 2 && isEnchantmentKeyword(args[args.length - 2])) {
            return enchantments;
        }

        ArrayList<String> candidates = new ArrayList<>(mats);
        candidates.addAll(enchantments);
        return candidates;
    }

    private boolean isEnchantmentKeyword(String input) {
        String normalized = SearchItemQuery.normalize(input);
        return normalized.equals("enchant") || normalized.equals("enchantment") || normalized.equals("enchanted");
    }
}
