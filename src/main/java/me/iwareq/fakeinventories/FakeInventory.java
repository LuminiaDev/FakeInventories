package me.iwareq.fakeinventories;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.inventory.BaseInventory;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.ContainerClosePacket;
import cn.nukkit.network.protocol.ContainerOpenPacket;
import lombok.Getter;
import lombok.Setter;
import me.iwareq.fakeinventories.block.FakeBlock;
import me.iwareq.fakeinventories.util.ItemHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeInventory extends BaseInventory {

    @Getter
    @Setter
    private String title;

    @Setter
    private ItemHandler defaultItemHandler;
    private final Map<Integer, ItemHandler> handlers = new HashMap<>();

    private final FakeBlock fakeBlock;

    public FakeInventory(InventoryType inventoryType) {
        this(inventoryType, null);
    }

    public FakeInventory(InventoryType inventoryType, String title) {
        super(null, inventoryType);
        this.title = title == null ? inventoryType.getDefaultTitle() : title;
        this.fakeBlock = FakeInventories.getFakeBlock(inventoryType);
    }

    @Override
    public void onOpen(Player player) {
        this.fakeBlock.create(player, this.getTitle());

        Server.getInstance().getScheduler().scheduleDelayedTask(FakeInventories.getInstance(), () -> {
            ContainerOpenPacket packet = new ContainerOpenPacket();
            packet.windowId = player.getWindowId(this);
            packet.type = this.getType().getNetworkType();

            List<Vector3> positions = this.fakeBlock.getPlacePositions(player);
            if (positions.isEmpty()) {
                return;
            }

            Vector3 position = positions.get(0);
            packet.x = position.getFloorX();
            packet.y = position.getFloorY();
            packet.z = position.getFloorZ();
            player.dataPacket(packet);

            super.onOpen(player);

            this.sendContents(player);
        }, 2);
    }

    @Override
    public void onClose(Player player) {
        ContainerClosePacket packet = new ContainerClosePacket();
        packet.windowId = player.getWindowId(this);
        packet.wasServerInitiated = player.getClosingWindowId() != packet.windowId;
        player.dataPacket(packet);

        super.onClose(player);
        this.fakeBlock.remove(player);
    }

    public void setItem(int index, Item item, ItemHandler handler) {
        if (super.setItem(index, item)) {
            this.handlers.put(index, handler);
        }
    }

    public void handle(int index, Item item, InventoryTransactionEvent event) {
        ItemHandler handler = this.handlers.getOrDefault(index, this.defaultItemHandler);
        if (handler == null) {
            return;
        }
        handler.handle(item, event);
    }
}
