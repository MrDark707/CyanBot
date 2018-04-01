package me.xjcyan1de.cyanbot;

import me.xjcyan1de.cyanbot.utils.Schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PlayerManager {
    private ExecutorService service = Executors.newFixedThreadPool(2);

    private Map<String, Player> playerMap = new HashMap<>();
    private Logger logger;

    public PlayerManager(Logger logger) {
        this.logger = logger;

        Schedule.timer(() -> {
            final Map<String, Player> playerMap = this.getPlayerMap();
            for (Player player : new ArrayList<>(playerMap.values())) {
                if (player.isClose()) {
                    playerMap.remove(player.getUsername());
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void connectPlayer(Player player) {
        if (!playerMap.containsKey(player.getUsername())) {
            playerMap.put(player.getUsername(), player);
            service.submit(player::startBot);
        }
    }

    public Player getPlayer(String nameText) {
        return playerMap.get(nameText);
    }

    public void disconnectPlayer(Player player) {
        playerMap.remove(player.getUsername());
        service.submit(()-> player.getClient().getSession().disconnect("Final"));
    }

    public boolean isConnected(String name) {
        return playerMap.containsKey(name) && !playerMap.get(name).isClose();
    }

    public Collection<Player> getPlayers() {
        return playerMap.values();
    }

    public Map<String, Player> getPlayerMap() {
        return playerMap;
    }
}
