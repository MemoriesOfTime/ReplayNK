package cn.powernukkitx.replaynk.item;

import cn.nukkit.Player;
import cn.powernukkitx.replaynk.ReplayNK;
import cn.powernukkitx.replaynk.trail.Trail;

/**
 * @author daoge_cmd
 * @date 2023/6/11
 * ReplayNK Project
 */
public class PauseItem extends ReplayNKItem {
    public static final int CUSTOM_ITEM_ID = 12105;
    public PauseItem() {
        super(/*"replaynk:pause"*/ CUSTOM_ITEM_ID, 0, 1, "Pause", "replaynk_pause");
    }

    @Override
    public void onInteract(Player player) {
        if (!Trail.isOperatingTrail(player)) {
            player.sendMessage(ReplayNK.getI18n().tr(player.getLanguageCode(), "replaynk.trail.notoperatingtrail"));
            return;
        }
        var trail = Trail.getOperatingTrail(player);
        if (trail.pause()) {
            player.sendMessage(ReplayNK.getI18n().tr(player.getLanguageCode(), "replaynk.trail.paused"));
        } else {
            player.sendMessage(ReplayNK.getI18n().tr(player.getLanguageCode(), "replaynk.trail.notplayingtrail"));
        }
    }
}


