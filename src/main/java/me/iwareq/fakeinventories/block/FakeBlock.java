package me.iwareq.fakeinventories.block;

import cn.nukkit.Player;
import cn.nukkit.level.DimensionData;
import cn.nukkit.math.Vector3;
import me.iwareq.fakeinventories.FakeInventories;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface FakeBlock {

    void create(Player player, String title);

    void remove(Player player);

    Set<Vector3> getLastPositions(Player player);

    default List<Vector3> getPlacePositions(Player player) {
        Vector3 position = player.getPosition().add(this.getOffset(player)).floor();
        DimensionData dimension = player.getLevel().getDimensionData();
        if (position.getFloorY() >= dimension.getMinHeight() && position.getFloorY() < dimension.getMaxHeight()) {
            return Collections.singletonList(position);
        }
        return Collections.emptyList();
    }

    default Vector3 getOffset(Player player) {
        return FakeInventories.getFakeBlockOffset().getOffset(player);
    }
}
