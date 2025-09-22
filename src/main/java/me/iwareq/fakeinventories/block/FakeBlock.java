package me.iwareq.fakeinventories.block;

import cn.nukkit.Player;
import cn.nukkit.level.DimensionData;
import cn.nukkit.math.Vector3;
import me.iwareq.fakeinventories.FakeInventories;

import java.util.Collections;
import java.util.List;

public abstract class FakeBlock {

    public abstract void create(Player player, String title);

    public abstract void remove(Player player);

    public List<Vector3> getPositions(Player player) {
        Vector3 position = player.getPosition().add(this.getOffset(player)).floor();
        DimensionData dimension = player.getLevel().getDimensionData();
        if (position.getFloorY() >= dimension.getMinHeight() && position.getFloorY() < dimension.getMaxHeight()) {
            return Collections.singletonList(position);
        }
        return Collections.emptyList();
    }

    protected Vector3 getOffset(Player player) {
        return FakeInventories.getFakeBlockOffset().getOffset(player);
    }
}
