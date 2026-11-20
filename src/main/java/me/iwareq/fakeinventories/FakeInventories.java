package me.iwareq.fakeinventories;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import cn.nukkit.inventory.transaction.action.SlotChangeAction;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import me.iwareq.fakeinventories.block.DoubleFakeBlock;
import me.iwareq.fakeinventories.block.FakeBlock;
import me.iwareq.fakeinventories.block.SingleFakeBlock;
import me.iwareq.fakeinventories.block.offset.FakeBlockOffset;
import me.iwareq.fakeinventories.block.offset.FakeBlockOffsets;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EnumMap;
import java.util.Map;

public class FakeInventories extends PluginBase {

    @Getter
    private static FakeInventories instance;

    private static final Map<InventoryType, FakeBlock> FAKE_BLOCKS = new EnumMap<>(InventoryType.class);
    private static final MethodHandle INVENTORY_AUTHORITY = findInventoryAuthority();
    private static FakeBlockOffset FAKE_BLOCK_OFFSET;

    @Override
    public void onLoad() {
        FakeInventories.instance = this;
        this.saveDefaultConfig();
        FAKE_BLOCKS.put(InventoryType.CHEST, new SingleFakeBlock(BlockID.CHEST, BlockEntity.CHEST));
        FAKE_BLOCKS.put(InventoryType.ENDER_CHEST, new SingleFakeBlock(BlockID.ENDER_CHEST, BlockEntity.ENDER_CHEST));
        FAKE_BLOCKS.put(InventoryType.DOUBLE_CHEST, new DoubleFakeBlock(BlockID.CHEST, BlockEntity.CHEST));
        FAKE_BLOCKS.put(InventoryType.FURNACE, new SingleFakeBlock(BlockID.FURNACE, BlockEntity.FURNACE));
        FAKE_BLOCKS.put(InventoryType.WORKBENCH, new SingleFakeBlock(BlockID.WORKBENCH, InventoryType.WORKBENCH.getDefaultTitle()));
        FAKE_BLOCKS.put(InventoryType.BREWING_STAND, new SingleFakeBlock(BlockID.BREWING_STAND_BLOCK, BlockEntity.BREWING_STAND));
        FAKE_BLOCKS.put(InventoryType.DISPENSER, new SingleFakeBlock(BlockID.DISPENSER, InventoryType.DISPENSER.getDefaultTitle()));
        FAKE_BLOCKS.put(InventoryType.DROPPER, new SingleFakeBlock(BlockID.DROPPER, InventoryType.DROPPER.getDefaultTitle()));
        FAKE_BLOCKS.put(InventoryType.HOPPER, new SingleFakeBlock(BlockID.HOPPER_BLOCK, BlockEntity.HOPPER));
        FAKE_BLOCKS.put(InventoryType.SHULKER_BOX, new SingleFakeBlock(BlockID.SHULKER_BOX, BlockEntity.SHULKER_BOX));
        FAKE_BLOCK_OFFSET = FakeBlockOffsets.valueOf(this.getConfig().getString("fake-block-offset-mode").toUpperCase());
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().subscribeEvent(
                InventoryTransactionEvent.class, this::onInventoryTransaction, EventPriority.MONITOR, this);
    }

    private void onInventoryTransaction(InventoryTransactionEvent event) {
        Player player = event.getTransaction().getSource();
        boolean serverAuthoritative = isServerAuthoritative(player);
        if (serverAuthoritative && event.isCancelled()) {
            return;
        }

        Inventory topWindow = serverAuthoritative ? player.getTopWindow().orElse(null) : null;
        var actions = event.getTransaction().getActions();
        if (topWindow instanceof FakeInventory inventory) {
            for (InventoryAction action : actions) {
                if (action instanceof SlotChangeAction change && inventory.isProtectedItemInventory(change.getInventory())) {
                    event.setCancelled();
                    return;
                }
            }
        }

        for (InventoryAction action : actions) {
            if (!(action instanceof SlotChangeAction change) || !(change.getInventory() instanceof FakeInventory inventory)) {
                continue;
            }

            int windowId = serverAuthoritative ? player.getWindowId(inventory) : -1;
            if (serverAuthoritative && (inventory != topWindow || windowId == -1 || !inventory.getViewers().contains(player))) {
                event.setCancelled();
                return;
            }

            inventory.handle(change.getSlot(), action.getSourceItem(), event);
            if (serverAuthoritative && (player.getWindowId(inventory) != windowId || !inventory.getViewers().contains(player))) {
                event.setCancelled();
                return;
            }
        }
    }

    static boolean isServerAuthoritative(Player player) {
        if (INVENTORY_AUTHORITY == null) {
            return false;
        }
        try {
            return (boolean) INVENTORY_AUTHORITY.invokeExact(player);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to read the inventory authority mode", e);
        }
    }

    // Older Lumi builds only support CAI and lack the authority accessor.
    private static MethodHandle findInventoryAuthority() {
        try {
            return MethodHandles.publicLookup().findVirtual(Player.class, "isInventoryServerAuthoritative",
                    MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    public static FakeBlock getFakeBlock(InventoryType inventoryType) {
        FakeBlock fakeBlock = FAKE_BLOCKS.get(inventoryType);
        if (fakeBlock == null) {
            throw new NullPointerException("FakeBlock for " + inventoryType.name() + " inventory not found!");
        }
        return fakeBlock;
    }

    public static FakeBlockOffset getFakeBlockOffset() {
        return FAKE_BLOCK_OFFSET;
    }
}