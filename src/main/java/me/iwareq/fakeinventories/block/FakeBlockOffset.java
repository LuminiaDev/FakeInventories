package me.iwareq.fakeinventories.block;

import cn.nukkit.Player;
import cn.nukkit.math.Vector3;
import me.iwareq.fakeinventories.util.InventoryUtils;

public enum FakeBlockOffset {
    STANDARD {
        @Override
        public Vector3 getOffset(Player player) {
            Vector3 offset = player.getDirectionVector();
            offset.x *= -(1 + player.getWidth());
            offset.y *= -(1 + player.getHeight());
            offset.z *= -(1 + player.getWidth());
            return offset;
        }
    },
    GEYSER {
        @Override
        public Vector3 getOffset(Player player) {
            return InventoryUtils.findAvailableOffset(player);
        }
    };

    public abstract Vector3 getOffset(Player player);
}
