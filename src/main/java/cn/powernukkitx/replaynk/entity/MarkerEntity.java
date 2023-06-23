package cn.powernukkitx.replaynk.entity;

import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author daoge_cmd
 * @date 2023/6/11
 * ReplayNK Project
 */
public class MarkerEntity extends ReplayNKEntity {
    public static final EntityDefinition DEF =
            EntityDefinition
                    .builder()
                    .identifier("replaynk:marker")
                    //.summonable(true)
                    .spawnEgg(false)
                    .implementation(MarkerEntity.class)
                    .build();
    private static final String MARKER_INDEX_KEY = "MarkerIndex";

    public MarkerEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public EntityDefinition getEntityDefinition() {
        return DEF;
    }

    //@Override
    public String getOriginalName() {
        return "Marker";
    }

    public int getMarkerIndex() {
        return namedTag.getInt(MARKER_INDEX_KEY);
    }

    public void setMarkerIndex(int index) {
        namedTag.putInt(MARKER_INDEX_KEY, index);
        setNameTag("§a" + index);
    }
}
