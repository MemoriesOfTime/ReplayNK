package cn.powernukkitx.replaynk.trail;

import cn.nukkit.Player;
import cn.powernukkitx.replaynk.ReplayNK;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TrailAgent {
    private final Player operator;
    private final Trail trail;
    private static final Map<Player, TrailAgent> OPERATING_TRAIL_AGENT = new HashMap<>();


    public TrailAgent(Player operator, Trail trail) {
        this.operator = operator;
        this.trail = trail;
    }

    public static TrailAgent init(Player operator, String trailName) {
        if (OPERATING_TRAIL_AGENT.containsKey(operator)) {
            return OPERATING_TRAIL_AGENT.get(operator);
        }

        var basePath = ReplayNK.getInstance().getDataFolder().toPath().resolve("trails").resolve(trailName + ".json");

        ReplayNK.getInstance().getLogger().info("Base Path: " + basePath);
        if (!Files.exists(basePath)) {
            return null;
        }

        String json;
        try {
            json = Files.readString(basePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var trail = fromJson(json);
        OPERATING_TRAIL_AGENT.put(operator, new TrailAgent(operator, trail));
        return OPERATING_TRAIL_AGENT.get(operator);
    }

    public static Trail fromJson(String json) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create().fromJson(json, Trail.class);
    }


    public void play(boolean showMessage) {
        trail.play(operator, showMessage);
    }
}