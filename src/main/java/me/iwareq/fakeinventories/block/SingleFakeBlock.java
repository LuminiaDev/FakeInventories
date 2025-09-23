package me.iwareq.fakeinventories.block;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.BlockEntityDataPacket;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class SingleFakeBlock implements FakeBlock {

    protected final int blockId;
    protected final String tileId;
    protected final Map<Player, Set<Vector3>> lastPositions = new Object2ObjectArrayMap<>();

    @Override
    public void create(Player player, String title) {
        this.createAndGetLastPositions(player).addAll(this.getPlacePositions(player));
        this.getLastPositions(player).forEach(position -> {
            UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
            updateBlockPacket.blockRuntimeId = GlobalBlockPalette.getOrCreateRuntimeId(player.protocol, this.blockId, 0);
            updateBlockPacket.flags = UpdateBlockPacket.FLAG_NETWORK;
            updateBlockPacket.x = position.getFloorX();
            updateBlockPacket.y = position.getFloorY();
            updateBlockPacket.z = position.getFloorZ();
            player.dataPacket(updateBlockPacket);

            BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
            blockEntityDataPacket.x = position.getFloorX();
            blockEntityDataPacket.y = position.getFloorY();
            blockEntityDataPacket.z = position.getFloorZ();
            try {
                blockEntityDataPacket.namedTag = NBTIO.write(this.getBlockEntityDataAt(position, title), ByteOrder.LITTLE_ENDIAN, true);
            } catch (IOException e) {
                log.error("Failed to create an NBT for the block entity", e);
            }

            player.dataPacket(blockEntityDataPacket);
        });
    }

    @Override
    public void remove(Player player) {
        this.getLastPositions(player).forEach(position -> {
            UpdateBlockPacket packet = new UpdateBlockPacket();
            packet.blockRuntimeId = GlobalBlockPalette.getOrCreateRuntimeId(player.protocol, player.getLevel().getBlock(position).getFullId());
            packet.flags = UpdateBlockPacket.FLAG_NETWORK;
            packet.x = position.getFloorX();
            packet.y = position.getFloorY();
            packet.z = position.getFloorZ();
            player.dataPacket(packet);
        });
        this.lastPositions.remove(player);
    }

    @Override
    public Set<Vector3> getLastPositions(Player player) {
        return lastPositions.getOrDefault(player, new HashSet<>());
    }

    protected Set<Vector3> createAndGetLastPositions(Player player) {
        if (!lastPositions.containsKey(player)) {
            lastPositions.put(player, new HashSet<>());
        }
        return lastPositions.get(player);
    }

    protected CompoundTag getBlockEntityDataAt(Vector3 position, String title) {
        return BlockEntity.getDefaultCompound(position, title)
                .putString("id", tileId)
                .putBoolean("isMovable", true)
                .putString("CustomName", title);
    }
}