package net.tak7.fakeplayers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.PlayerInteractManager;
import net.minecraft.server.v1_12_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

public class PlayerUtil {

    public static EntityPlayer spawnFakePlayer(String playerName, String customName) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), customName == null ? playerName : customName.substring(0, Math.min(customName.length(), 16)));

        String[] name = getSkin(playerName);

        if (name != null) {
            gameProfile.getProperties().put("textures", new Property("textures", name[0], name[1]));

            return new EntityPlayer(server, world, gameProfile, new PlayerInteractManager(world));
        }
        return null;
    }

    public static String[] getSkin(String name) {
        ConfigurationSection cache = FakePlayers.getCacheConfig().cfg().getConfigurationSection("cache");
        if (cache != null) {
            for (String uuid : cache.getKeys(false)){
                ConfigurationSection cfg = cache.getConfigurationSection(uuid);
                if (cfg != null) {
                    if (cfg.getString("username").equals(name)) {
                        return new String[]{cfg.getString("texture"), cfg.getString("signature")};
                    }
                }
            }
        }
        return null;
    }

    public static String[] getSkinData(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            String uuid = new JsonParser().parse(reader).getAsJsonObject().get("id").getAsString();

            URL url2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            InputStreamReader reader2 = new InputStreamReader(url2.openStream());
            JsonObject property = new JsonParser().parse(reader2).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();

            String texture = property.get("value").getAsString();
            String signature = property.get("signature").getAsString();

            return new String[]{texture, signature};
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error loading FakePlayer: " + name.replace(";", "") + " - removing from config!");
        }
        return null;
    }
}