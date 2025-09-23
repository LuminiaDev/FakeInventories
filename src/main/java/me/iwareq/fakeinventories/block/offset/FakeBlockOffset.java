package me.iwareq.fakeinventories.block.offset;

import cn.nukkit.Player;
import cn.nukkit.math.Vector3;

/**
 * Interface for defining the offset of a fake block for specific inventory.
 */
public interface FakeBlockOffset {

    /**
     * Get an offset for the player.
     *
     * @param player the player
     * @return defined offset
     */
    Vector3 getOffset(Player player);
}
