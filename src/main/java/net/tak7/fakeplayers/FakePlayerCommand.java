package net.tak7.fakeplayers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakePlayerCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //  /fp <add|remove|reload|updatecache> <username> [<displayName>]
        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("add")) {
                String[] validity = PlayerUtil.getSkinData(args[1]);
                if (validity != null) {
                    String displayName = "";
                    if (args.length >= 3) {
                        displayName = args[2];
                    }
                    List<String> current = FakePlayers.getMainConfig().cfg().getStringList("tablist");
                    current.add(args[1] + ";" + displayName.replace(";", ""));
                    FakePlayers.getMainConfig().cfg().set("tablist", current);
                    FakePlayers.getMainConfig().saveConfiguration();
                    sender.sendMessage(ChatColor.GREEN + "FakePlayer " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " was successfully added! Tablist reloading.");
                    FakePlayers.getInstance().addToCache(args[1]);
                    FakePlayers.getInstance().refresh();
                } else {
                    sender.sendMessage(ChatColor.RED + "The player " + ChatColor.GOLD + args[1] + ChatColor.RED + " does not exist! (Usernames are case-sensitive)");
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                List<String> current = new ArrayList<>();
                boolean valid = false;
                for (String fakePlayer : FakePlayers.getMainConfig().cfg().getStringList("tablist")) {
                    if (!(fakePlayer.split(";")[0].equalsIgnoreCase(args[1]))) {
                        current.add(fakePlayer);
                    } else {
                        valid = true;
                    }
                }
                FakePlayers.getMainConfig().cfg().set("tablist", current);
                FakePlayers.getMainConfig().saveConfiguration();
                if (!valid) {
                    sender.sendMessage(ChatColor.RED + "The player " + ChatColor.GOLD + args[1] + ChatColor.RED + " was never added!");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "FakePlayer " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " was successfully removed! Tablist reloading.");
                    FakePlayers.getInstance().refresh();
                }
            }
        } else {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    FakePlayers.getMainConfig().reloadConfiguration();
                    sender.sendMessage(ChatColor.GREEN + "Successfully reloaded FakePlayerTab");
                    sender.sendMessage(ChatColor.GOLD + "Checking if cache is synced...");
                    if (!FakePlayers.getInstance().isSynced()) {
                        sendUpdate(sender);
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Cache was synced!");
                    }
                } else if (args[0].equalsIgnoreCase("updatecache")) {
                    sendUpdate(sender);
                } else {
                    sendHelp(sender);
                }
            } else {
                sendHelp(sender);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> argList = new ArrayList<>();

        if (args.length == 1 && sender.hasPermission("cmd.fakeplayer")) {
            argList.add("add");
            argList.add("remove");
            argList.add("reload");
            argList.add("updatecache");
            return argList.stream().filter(a -> a.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                for (String fakePlayer : FakePlayers.getMainConfig().cfg().getStringList("tablist")) {
                    if (!argList.contains(fakePlayer.split(";")[0])) {
                        argList.add(fakePlayer.split(";")[0]);
                    }
                }
            }
            return argList.stream().filter(a -> a.startsWith(args[1])).collect(Collectors.toList());
        }
        return argList; // returns an empty list
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Invalid input. Do " + ChatColor.GOLD + "/fakeplayer <add|remove> <username> [<displayname>]" + ChatColor.RED + ". Remember the username is case-sensitive.");
    }

    private void sendUpdate(CommandSender sender) {
        // send remove
        for (Player player : Bukkit.getOnlinePlayers()) {
            FakePlayers.getInstance().removeFakeTablist(player, "actual send remove from updatecache");
        }

        // start sync
        sender.sendMessage(ChatColor.GOLD + "Syncing FakePlayer Cache! (Tablist will be disabled until sync finishes)");
        new BukkitRunnable() {
            @Override
            public void run() {
                FakePlayers.getInstance().updateCache();
                FakePlayers.getInstance().addToList();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    FakePlayers.getInstance().sendFakeTablist(player, "cache was just synced");
                }
                sender.sendMessage(ChatColor.GREEN + "FakePlayer Cache Sync Completed!");
            }
        }.runTaskAsynchronously(FakePlayers.getInstance());
    }
}