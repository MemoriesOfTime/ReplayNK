package cn.powernukkitx.replaynk.item;

import cn.nukkit.Player;
import cn.powernukkitx.replaynk.ReplayNK;
import cn.powernukkitx.replaynk.trail.Trail;

/**
 * @author daoge_cmd
 * @date 2023/6/16
 * ReplayNK Project
 */
public class SettingItem extends ReplayNKItem {

    public static final int CUSTOM_ITEM_ID = 12107;

    public SettingItem() {
        super(/*"replaynk:setting"*/ CUSTOM_ITEM_ID, 0, 1, "Setting", "replaynk_setting");
    }

    @Override
    public void onInteract(Player player) {
        if (!Trail.isOperatingTrail(player)) {
            player.sendMessage(ReplayNK.getI18n().tr(player.getLanguageCode(), "replaynk.trail.notoperatingtrail"));
            return;
        }
        var trail = Trail.getOperatingTrail(player);
        trail.showEditorForm(player);
    }
}
