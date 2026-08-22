package net.mcreator.blackbox.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.init.BlackboxModMenus;

import java.util.ArrayList;
import java.util.List;

public class AdminBookMenu extends AbstractContainerMenu {
	public static final int GHOST_SLOT_COUNT = 18;
	private final SimpleContainer ghostInventory = new SimpleContainer(GHOST_SLOT_COUNT);
	private final DataSlot measurementSeconds = DataSlot.standalone();

	public AdminBookMenu(int id, Inventory inventory) {
		this(id, inventory, true);
	}

	public AdminBookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
		this(id, inventory, false);
	}

	private AdminBookMenu(int id, Inventory inventory, boolean loadConfig) {
		super(BlackboxModMenus.ADMIN_BOOK.get(), id);
		if (loadConfig) {
			this.measurementSeconds.set(BlackboxConfig.MEASUREMENT_SECONDS.get());
			int slot = 0;
			for (Block block : BlackboxConfig.deniedBlocks()) {
				if (slot >= GHOST_SLOT_COUNT) {
					break;
				}
				this.ghostInventory.setItem(slot++, new ItemStack(block));
			}
		}
		for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
			this.addSlot(new GhostSlot(this.ghostInventory, slot, 8 + (slot % 9) * 18, 77 + (slot / 9) * 18));
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 130 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(inventory, column, 8 + column * 18, 188));
		}
		this.addDataSlot(this.measurementSeconds);
	}

	public int getMeasurementSeconds() {
		return this.measurementSeconds.get();
	}

	public void setMeasurementSeconds(int seconds) {
		this.measurementSeconds.set(seconds);
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (slotId >= 0 && slotId < GHOST_SLOT_COUNT) {
			ItemStack carried = getCarried();
			if (carried.getItem() instanceof BlockItem) {
				this.ghostInventory.setItem(slotId, carried.copyWithCount(1));
			} else if (carried.isEmpty()) {
				this.ghostInventory.setItem(slotId, ItemStack.EMPTY);
			}
			return;
		}
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public void removed(Player player) {
		if (!player.level().isClientSide() && player.hasPermissions(2)) {
			List<Block> blocks = new ArrayList<>();
			for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
				if (this.ghostInventory.getItem(slot).getItem() instanceof BlockItem blockItem) {
					blocks.add(blockItem.getBlock());
				}
			}
			BlackboxConfig.setDeniedBlocks(blocks);
		}
		super.removed(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < GHOST_SLOT_COUNT || index >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = this.slots.get(index).getItem();
		if (!(stack.getItem() instanceof BlockItem)) {
			return ItemStack.EMPTY;
		}
		for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
			if (this.ghostInventory.getItem(slot).isEmpty()) {
				this.ghostInventory.setItem(slot, stack.copyWithCount(1));
				return stack.copy();
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.hasPermissions(2);
	}

	private static final class GhostSlot extends Slot {
		private GhostSlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
