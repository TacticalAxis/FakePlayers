package net.tak7.fakeplayers;

import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_12_R1.PlayerConnection;
import net.tak7.api.CustomConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayers extends JavaPlugin implements Listener {

    private static FakePlayers instance;
    private static CustomConfiguration mainConfig;
    private static CustomConfiguration cacheConfig;
    private static ArrayList<EntityPlayer> fakePlayers;

    public static FakePlayers getInstance() {
        return instance;
    }

    public static CustomConfiguration getMainConfig() {
        return mainConfig;
    }

    public static CustomConfiguration getCacheConfig() {
        return cacheConfig;
    }

    @Override
    public void onEnable() {
        instance = this;
        fakePlayers = new ArrayList<>();
        mainConfig = new CustomConfiguration("config.yml", this);
        cacheConfig = new CustomConfiguration("cache.yml", this);

        getCommand("fakeplayer").setExecutor(new FakePlayerCommand());
        getCommand("fakeplayer").setTabCompleter(new FakePlayerCommand());

        // events
        getServer().getPluginManager().registerEvents(new FakePlayerEvents(), this);

        loadFakePlayers();
    }

    @Override
    public void onDisable() {
        instance = null;

        // send all currently online players the fake player remove packet
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeFakeTablist(player, "Plugin was disabling");
        }
    }

    public void sendFakeTablist(Player player, String reason) {
//        System.out.println("THERE ARE " + fakePlayers.size() + " PLAYERS TO SEND!! (" + reason + ")");
        for (EntityPlayer ep : fakePlayers) {
//            System.out.println(ep.getProfile().getName() + reason);
            PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, ep));
        }
    }

    public void removeFakeTablist(Player player, String reason) {
//        System.out.println("THERE ARE " + fakePlayers.size() + " PLAYERS TO REMOVE!!");// (" + reason + ")");
        for (EntityPlayer ep : fakePlayers) {
//            System.out.println(ep.getProfile().getName() + reason);
            PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, ep));
        }
    }

    public void loadFakePlayers() {
        // arrange all entries
        List<String> current = new ArrayList<>();
        for (String fakePlayer : mainConfig.cfg().getStringList("tablist")) {
            String[] data = fakePlayer.split(";");
            String username = data[0].replace(";", "");
            if (data.length > 1) {
                String displayName = data[1].replace(";", "");
                current.add(username + ";" + displayName);
            } else {
                current.add(username);
            }
        }
        mainConfig.cfg().set("tablist", current);
        mainConfig.saveConfiguration();

        // update cache if not synced
        if (!isSynced()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "Syncing FakePlayer Cache! (Tablist will be disabled until sync finishes). Depending on the amount of different usernames, this can take a while.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateCache();
                    addToList();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        sendFakeTablist(player, "cache was just synced");
                    }
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "FakePlayer Cache Sync Completed!");
                }
            }.runTaskAsynchronously(instance);
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "FakePlayer Cache up-to-date!");
            addToList();
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendFakeTablist(player, "cache was already synced");
            }
        }
    }

    public void refresh() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeFakeTablist(player, "refresh method");
        }

        loadFakePlayers();
    }

    public void updateCache() {
        List<String> usernames = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        cacheConfig.cfg().set("cache", null);

        for (String fakePlayer : mainConfig.cfg().getStringList("tablist")) {
            String username = fakePlayer.split(";")[0];
            if (!usernames.contains(username)) {
                usernames.add(username);
            }
        }
        for (String username : usernames) {
            addToCache(username);
        }


    }

    public void addToCache(String username) {
        boolean remove = false;

        String[] data = PlayerUtil.getSkinData(username);
        if (data != null) {
            String texture = data[0];
            String signature = data[1];
            ConfigurationSection cfg = cacheConfig.cfg().getConfigurationSection("cache");
            if (cfg == null) {
                cfg = cacheConfig.cfg().createSection("cache");
            }
            String newUUID = UUID.randomUUID().toString();
            while (cfg.getKeys(false).contains(newUUID)) {
                newUUID = UUID.randomUUID().toString();
            }
            ConfigurationSection entry = cfg.createSection(newUUID);

            entry.set("username", username);
            entry.set("texture", texture);
            entry.set("signature", signature);

            cacheConfig.saveConfiguration();
        } else  {
            remove = true;
        }

        if (remove) {
            List<String> finalData = new ArrayList<>();

            for (String entry : mainConfig.cfg().getStringList("tablist")) {
                if (!(username.equals(entry.split(";")[0]))) {
                    finalData.add(entry);
                }
            }

            mainConfig.cfg().set("tablist", finalData);
            mainConfig.saveConfiguration();
        }
    }

    public void addToList() {
        // add entries to global list
        fakePlayers = new ArrayList<>();
        for (String fakePlayer : mainConfig.cfg().getStringList("tablist")) {
            EntityPlayer ep;
            if (fakePlayer.split(";").length > 1) {
                String realName = fakePlayer.split(";")[0].replace(";", "");
                String displayName = ChatColor.translateAlternateColorCodes('&', fakePlayer.split(";")[1]).replace(";", "");
                ep = PlayerUtil.spawnFakePlayer(realName, displayName);
            } else {
                ep = PlayerUtil.spawnFakePlayer(fakePlayer.replace(";", ""), null);
            }
            if (ep != null) {
                fakePlayers.add(ep);
            }
        }
    }

    public boolean isSynced() {
        boolean synced = true;
        for (String fakePlayer : mainConfig.cfg().getStringList("tablist")) {
            boolean exists = false;
            if (cacheConfig.cfg().getConfigurationSection("cache") == null) {
                synced = false;
                break;
            }
            for (String uuid : cacheConfig.cfg().getConfigurationSection("cache").getKeys(false)) {
                ConfigurationSection entry = cacheConfig.cfg().getConfigurationSection("cache").getConfigurationSection(uuid);
                String name = entry.getString("username");
                String fpName = fakePlayer.split(";")[0].replace(";", "");
                if (name.equals(fpName)) {
                    exists = true;
                }
            }
            if (!exists) {
                synced = false;
                break;
            }
        }
        return synced;
    }
}