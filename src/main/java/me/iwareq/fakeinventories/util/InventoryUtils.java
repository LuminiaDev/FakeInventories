package me.iwareq.fakeinventories.util;

import cn.nukkit.Player;
import cn.nukkit.block.BlockEntityHolder;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.block.Block;

public class InventoryUtils {

    /**
     * Finds a usable offset relative to the player's position for placing a fake inventory block.
     * Returns null if no valid offset is found.
     */
    public static Vector3 findAvailableOffset(Player player) {
        Level level = player.getLevel();

        int minY = level.getDimensionData().getMinHeight();
        int maxY = level.getDimensionData().getMaxHeight();

        Vector3 playerPos = player.getPosition();

        // Offset above the player
        Vector3 offsetAbove = new Vector3(0, 2, 0);
        Vector3 posAbove = playerPos.add(offsetAbove);
        if (posAbove.getY() >= minY && posAbove.getY() < maxY && canUseWorldSpace(level, posAbove)) {
            return offsetAbove;
        }

        // Offset 4 blocks below the player
        Vector3 offsetBelow = new Vector3(0, -4, 0);
        Vector3 posBelow = playerPos.add(offsetBelow);
        if (posBelow.getY() >= minY && posBelow.getY() < maxY && canUseWorldSpace(level, posBelow)) {
            return offsetBelow;
        }

        return null;
    }

    /**
     * Checks if the block space can be used as a "fake" block location.
     */
    private static boolean canUseWorldSpace(Level level, Vector3 pos) {
        Block block = level.getBlock(pos);
        // Valid if the block is air or a simple block without a BlockEntity
        return block.isAir() || !(block instanceof BlockEntityHolder<?>);
    }
}
