package cn.powernukkitx.replaynk.item;

import cn.lanink.customitemapi.item.CustomItemDefinition;
import cn.lanink.customitemapi.item.ItemCustom;
import cn.lanink.customitemapi.item.data.ItemCreativeCategory;
import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author daoge_cmd
 * @date 2023/6/11
 * ReplayNK Project
 */
public abstract class ReplayNKItem extends ItemCustom {

    public ReplayNKItem(@NotNull int id, Integer meta, int count, @Nullable String name) {
        super(id, meta, count, name);
    }

    public ReplayNKItem(@NotNull int id, Integer meta, int count, @Nullable String name, @NotNull String textureName) {
        super(id, meta, count, name, textureName);
    }

    @Override
    public CustomItemDefinition getDefinition() {
        return CustomItemDefinition
                .simpleBuilder(this, ItemCreativeCategory.NATURE)
                .allowOffHand(false)
                .build();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public void onInteract(Player player) {
        //Do nothing
    }

    public void onClickEntity(Player player, Entity entity) {
        //Do nothing
    }
}
