package io.github.marcschlicker.blackbox.world.inventory;

import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.Vec3;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.init.BlackboxModMenus;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmDimensionRuntime;

public class CoreSplitterMenu extends AbstractContainerMenu {
	private final DataSlot confirmation = DataSlot.standalone();
	private final ContainerLevelAccess anvilAccess;
	private final SimpleContainer coreSlot = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			CoreSplitterMenu.this.confirmation.set(0);
		}
	};

	public CoreSplitterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		this(id, inventory, extraData.readBlockPos());
	}

	public CoreSplitterMenu(int id, Inventory inventory, BlockPos anvilPos) {
		super(BlackboxModMenus.CORE_SPLITTER.get(), id);
		this.anvilAccess = ContainerLevelAccess.create(inventory.player.level(), anvilPos);
		this.addDataSlot(this.confirmation);
		this.addSlot(new Slot(this.coreSlot, 0, 80, 27) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(BlackboxModItems.DIMENSION_CORE.get());
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 110 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(inventory, column, 8 + column * 18, 168));
		}
	}

	public ItemStack core() {
		return this.coreSlot.getItem(0);
	}

	public boolean isAwaitingConfirmation() {
		return this.confirmation.get() != 0;
	}

	public void handleAction(ServerPlayer player, int action) {
		if (!stillValid(player)) {
			return;
		}
		if (action == 0) {
			if (canSplit(player, true)) {
				this.confirmation.set(1);
				this.broadcastChanges();
			}
			return;
		}
		if (action == 1 && this.isAwaitingConfirmation() && canSplit(player, true)) {
			split(player);
		}
	}

	private boolean canSplit(ServerPlayer player, boolean notify) {
		ItemStack core = core();
		if (!hasSplitterUpgrade(player) || !core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core) || FarmCoreData.isVillageArchiveCore(core)
				|| FarmCoreData.isSplitOutputCore(core) || !FarmCoreData.canManage(core, player)) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.blackbox.core_splitter.invalid"));
			}
			return false;
		}
		FarmCoreData.Recipe recipe = FarmCoreData.read(core, player.level().registryAccess());
		if (!recipe.isValid() || recipe.outputTemplates().size() < 2) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.blackbox.core_splitter.single_output"));
			}
			return false;
		}
		return true;
	}

	private void split(ServerPlayer player) {
		if (!consumeSplitterUpgrade(player)) {
			return;
		}
		ItemStack source = core().copyWithCount(1);
		FarmCoreData.Recipe recipe = FarmCoreData.read(source, player.level().registryAccess());
		FarmCoreData.getCoreId(source).ifPresent(coreId -> FarmDimensionRuntime.deleteFarmCell(player.server, coreId));
		this.coreSlot.setItem(0, ItemStack.EMPTY);
		this.confirmation.set(0);
		for (FarmCoreData.StackAmount output : recipe.outputTemplates()) {
			ItemStack split = FarmCoreData.createSplitOutputCore(source, recipe, output.stack(), player.level().registryAccess());
			ItemHandlerHelper.giveItemToPlayer(player, split);
		}
		player.sendSystemMessage(Component.translatable("message.blackbox.core_splitter.completed", recipe.outputTemplates().size()));
		this.broadcastChanges();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();
		if (index == 0) {
			if (!this.moveItemStackTo(source, 1, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.DIMENSION_CORE.get())) {
			if (!this.moveItemStackTo(source, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}
		if (source.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.anvilAccess.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof AnvilBlock
				&& player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0D, false);
	}

	private static boolean hasSplitterUpgrade(ServerPlayer player) {
		if (player.getAbilities().instabuild || player.getMainHandItem().is(BlackboxModItems.CORE_SPLITTER.get())
				|| player.getOffhandItem().is(BlackboxModItems.CORE_SPLITTER.get())) {
			return true;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(BlackboxModItems.CORE_SPLITTER.get())) {
				return true;
			}
		}
		return false;
	}

	private static boolean consumeSplitterUpgrade(ServerPlayer player) {
		if (player.getAbilities().instabuild) {
			return true;
		}
		if (player.getMainHandItem().is(BlackboxModItems.CORE_SPLITTER.get())) {
			player.getMainHandItem().shrink(1);
			return true;
		}
		if (player.getOffhandItem().is(BlackboxModItems.CORE_SPLITTER.get())) {
			player.getOffhandItem().shrink(1);
			return true;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(BlackboxModItems.CORE_SPLITTER.get())) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (!player.level().isClientSide) {
			ItemStack remaining = this.coreSlot.removeItemNoUpdate(0);
			if (!remaining.isEmpty()) {
				ItemHandlerHelper.giveItemToPlayer(player, remaining);
			}
		}
	}
}
