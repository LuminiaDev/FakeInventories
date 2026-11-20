package me.iwareq.fakeinventories;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.inventory.BaseInventory;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.inventory.PlayerUIComponent;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.ContainerClosePacket;
import cn.nukkit.network.protocol.ContainerOpenPacket;
import cn.nukkit.network.protocol.InventorySlotPacket;
import cn.nukkit.network.protocol.types.ContainerIds;
import cn.nukkit.network.protocol.types.inventory.ContainerSlotType;
import cn.nukkit.network.protocol.types.inventory.ContainerType;
import cn.nukkit.network.protocol.types.inventory.FullContainerName;
import lombok.Getter;
import lombok.Setter;
import me.iwareq.fakeinventories.block.FakeBlock;
import me.iwareq.fakeinventories.block.offset.FakeBlockOffset;
import me.iwareq.fakeinventories.util.ItemHandler;
import me.iwareq.fakeinventories.util.SimplePlayerHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Setter
public class FakeInventory extends BaseInventory {

    private static final int WORKBENCH_INPUT_OFFSET = 32;
    private static final int WORKBENCH_RESULT_SLOT = 9;

    @Getter
    @Setter
    private String title;

    private ItemHandler defaultItemHandler;
    private SimplePlayerHandler openHandler;
    private SimplePlayerHandler closeHandler;

    private final Map<Integer, ItemHandler> handlers = new HashMap<>();

    private final FakeBlock fakeBlock;
    private FakeBlockOffset fakeBlockOffset;

    public FakeInventory(InventoryType inventoryType) {
        this(inventoryType, null);
    }

    public FakeInventory(InventoryType inventoryType, String title) {
        super(null, inventoryType);
        this.title = title == null ? inventoryType.getDefaultTitle() : title;
        this.fakeBlock = FakeInventories.getFakeBlock(inventoryType);
        this.fakeBlockOffset = FakeInventories.getFakeBlockOffset();
    }

    @Override
    public void onOpen(Player player) {
        int windowId = player.getWindowId(this);
        this.fakeBlock.create(player, this.fakeBlockOffset.getOffset(player), this.getTitle());

        Server.getInstance().getScheduler().scheduleDelayedTask(FakeInventories.getInstance(), () -> {
            if (windowId == -1 || player.getWindowId(this) != windowId) {
                return;
            }

            ContainerOpenPacket packet = new ContainerOpenPacket();
            packet.windowId = windowId;
            packet.type = this.getType().getNetworkType();

            Optional<Vector3> first = this.fakeBlock.getLastPositions(player).stream().findFirst();
            if (first.isPresent()) {
                Vector3 position = first.get();
                packet.x = position.getFloorX();
                packet.y = position.getFloorY();
                packet.z = position.getFloorZ();
                player.dataPacket(packet);

                super.onOpen(player);
                this.sendContents(player);
            } else {
                this.fakeBlock.remove(player);
            }
        }, 2);

        if (this.openHandler != null) {
            this.openHandler.handle(player);
        }
    }

    @Override
    public void onClose(Player player) {
        ContainerClosePacket packet = new ContainerClosePacket();
        packet.windowId = player.getWindowId(this);
        packet.wasServerInitiated = player.getClosingWindowId() != packet.windowId;
        packet.type = ContainerType.from(this.getType().getNetworkType());
        player.dataPacket(packet);

        super.onClose(player);
        this.fakeBlock.remove(player);

        if (this.closeHandler != null) {
            this.closeHandler.handle(player);
        }
    }

    @Override
    public void sendContents(Player... players) {
        if (this.getType() != InventoryType.WORKBENCH) {
            super.sendContents(players);
            return;
        }

        for (Player player : players) {
            if (FakeInventories.isServerAuthoritative(player) && this.getViewers().contains(player)) {
                for (int slot = 0; slot < this.getSize() && slot <= WORKBENCH_RESULT_SLOT; slot++) {
                    this.sendWorkbenchSlot(slot, player);
                }
            } else {
                super.sendContents(player);
            }
        }
    }

    @Override
    public void sendSlot(int index, Player player) {
        if (this.getType() == InventoryType.WORKBENCH && FakeInventories.isServerAuthoritative(player)
                && this.getViewers().contains(player)) {
            this.sendWorkbenchSlot(index, player);
        } else {
            super.sendSlot(index, player);
        }
    }

    @Override
    public void sendSlot(int index, Player... players) {
        if (this.getType() != InventoryType.WORKBENCH) {
            super.sendSlot(index, players);
            return;
        }

        for (Player player : players) {
            this.sendSlot(index, player);
        }
    }

    private void sendWorkbenchSlot(int index, Player player) {
        if (index < 0 || index > WORKBENCH_RESULT_SLOT) {
            return;
        }

        boolean result = index == WORKBENCH_RESULT_SLOT;
        InventorySlotPacket packet = new InventorySlotPacket();
        packet.inventoryId = ContainerIds.UI;
        packet.slot = result ? PlayerUIComponent.CREATED_ITEM_OUTPUT_UI_SLOT : WORKBENCH_INPUT_OFFSET + index;
        packet.containerNameData = new FullContainerName(
                result ? ContainerSlotType.CRAFTING_OUTPUT : ContainerSlotType.CRAFTING_INPUT, null);
        packet.item = this.getItem(index);
        player.dataPacket(packet);
    }

    boolean isProtectedItemInventory(Inventory target) {
        if (!(target.getHolder() instanceof Item)) {
            return false;
        }

        Set<Inventory> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<Integer, Item> entry : this.getContents().entrySet()) {
            if (this.handlers.getOrDefault(entry.getKey(), this.defaultItemHandler) != null
                    && containsItemInventory(entry.getValue(), target, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsItemInventory(Item item, Inventory target, Set<Inventory> visited) {
        if (!(item instanceof InventoryHolder holder)) {
            return false;
        }

        Inventory inventory = holder.getInventory();
        if (inventory == target) {
            return true;
        }
        if (inventory != null && visited.add(inventory)) {
            for (Item nested : inventory.getContents().values()) {
                if (containsItemInventory(nested, target, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setItem(int index, Item item, ItemHandler handler) {
        if (super.setItem(index, item)) {
            this.handlers.put(index, handler);
        }
    }

    public void setItemHandler(int index, ItemHandler handler) {
        this.handlers.put(index, handler);
    }

    public void handle(int index, Item item, InventoryTransactionEvent event) {
        ItemHandler handler = this.handlers.getOrDefault(index, this.defaultItemHandler);
        if (handler == null) {
            return;
        }
        handler.handle(item, event);
    }
}
