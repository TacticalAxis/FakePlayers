package net.tak7.fakeplayers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class FakePlayerEvents implements Listener {

    @EventHandler
    public void join(PlayerJoinEvent event) {
        FakePlayers.getInstance().sendFakeTablist(event.getPlayer(), "JN send");
    }

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event) {
        FakePlayers.getInstance().removeFakeTablist(event.getPlayer(), "CW remove");
        FakePlayers.getInstance().sendFakeTablist(event.getPlayer(), "CW send");
    }
}
