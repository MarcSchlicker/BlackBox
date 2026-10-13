package io.github.marcschlicker.blackbox.world.inventory;

import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;

import io.github.marcschlicker.blackbox.block.entity.CoreDistributionMatrixBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModBlocks;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.init.BlackboxModMenus;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmDimensionRuntime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-authoritative assignment state for one open Core Distribution Matrix. */
public class CoreDistributionMatrixMenu extends AbstractContainerMenu {
	public static final int MAX_ITEM_OUTPUTS = 256;
	public static final int MAX_FLUID_OUTPUTS = 64;
	public static final int CYCLE_ITEM_GROUP = 0;
	public static final int CYCLE_FLUID_GROUP = 1;
	public static final int CYCLE_ENERGY_GROUP = 2;
	public static final int RESET_ASSIGNMENTS = 3;
	public static final int ARM_SPLIT = 4;
	public static final int CONFIRM_SPLIT = 5;

	public final Level world;
	public final int x;
	public final int y;
	public final int z;
	private final ContainerLevelAccess access;
	private final CoreDistributionMatrixBlockEntity matrix;
	private final DataSlot confirmation = DataSlot.standalone();
	private final DataSlot[] itemGroups = new DataSlot[MAX_ITEM_OUTPUTS];
	private final DataSlot[] fluidGroups = new DataSlot[MAX_FLUID_OUTPUTS];
	private final DataSlot energyGroup = DataSlot.standalone();

	public CoreDistributionMatrixMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		this(id, inventory, extraData.readBlockPos());
	}

	public CoreDistributionMatrixMenu(int id, Inventory inventory, BlockPos pos) {
		super(BlackboxModMenus.CORE_DISTRIBUTION_MATRIX.get(), id);
		this.world = inventory.player.level();
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.access = ContainerLevelAccess.create(this.world, pos);
		this.matrix = this.world.getBlockEntity(pos) instanceof CoreDistributionMatrixBlockEntity found ? found : null;
		this.addDataSlot(this.confirmation);
		for (int index = 0; index < this.itemGroups.length; index++) {
			this.itemGroups[index] = DataSlot.standalone();
			this.addDataSlot(this.itemGroups[index]);
		}
		for (int index = 0; index < this.fluidGroups.length; index++) {
			this.fluidGroups[index] = DataSlot.standalone();
			this.addDataSlot(this.fluidGroups[index]);
		}
		this.addDataSlot(this.energyGroup);

		if (this.matrix != null) {
			InvWrapper storage = new InvWrapper(this.matrix);
			this.addSlot(new SlotItemHandler(storage, CoreDistributionMatrixBlockEntity.CORE_SLOT, 89, 25) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(BlackboxModItems.DIMENSION_CORE.get());
				}

				@Override
				public void setChanged() {
					super.setChanged();
					if (!CoreDistributionMatrixMenu.this.world.isClientSide) {
						CoreDistributionMatrixMenu.this.resetAssignments();
					}
				}
			});
			this.addSlot(new SlotItemHandler(storage, CoreDistributionMatrixBlockEntity.SPLITTER_SLOT, 125, 25) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(BlackboxModItems.CORE_SPLITTER.get());
				}

				@Override
				public void setChanged() {
					super.setChanged();
					CoreDistributionMatrixMenu.this.confirmation.set(0);
				}
			});
		} else {
			this.addSlot(new Slot(new SimpleContainer(1), 0, 89, 25) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
			this.addSlot(new Slot(new SimpleContainer(1), 0, 125, 25) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		addPlayerInventory(inventory);
		if (!this.world.isClientSide) {
			resetAssignments();
		}
	}

	private void addPlayerInventory(Inventory inventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 173 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(inventory, column, 8 + column * 18, 231));
		}
	}

	public ItemStack core() {
		return this.matrix == null ? ItemStack.EMPTY : this.matrix.getItem(CoreDistributionMatrixBlockEntity.CORE_SLOT);
	}

	public FarmCoreData.Recipe recipe() {
		return this.matrix == null ? new FarmCoreData.Recipe(0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 0)
				: FarmCoreData.read(core(), this.world.registryAccess());
	}

	public boolean isAwaitingConfirmation() {
		return this.confirmation.get() != 0;
	}

	public int itemGroup(int index) {
		return index < 0 || index >= this.itemGroups.length ? -1 : this.itemGroups[index].get();
	}

	public int fluidGroup(int index) {
		return index < 0 || index >= this.fluidGroups.length ? -1 : this.fluidGroups[index].get();
	}

	public int energyGroup() {
		return this.energyGroup.get();
	}

	public int resultCoreCount() {
		FarmCoreData.Recipe recipe = recipe();
		int count = recipe.outputTemplates().size();
		if (count > MAX_ITEM_OUTPUTS) {
			return 0;
		}
		boolean[] used = new boolean[count];
		for (int index = 0; index < count; index++) {
			int group = itemGroup(index);
			if (group >= 0 && group < used.length) {
				used[group] = true;
			}
		}
		int result = 0;
		for (boolean group : used) {
			if (group) {
				result++;
			}
		}
		return result;
	}

	public void handleAction(ServerPlayer player, int action, int index) {
		if (!stillValid(player) || !isMatrixAt(player) || !canConfigure(player, true)) {
			return;
		}
		FarmCoreData.Recipe recipe = recipe();
		int itemCount = recipe.outputTemplates().size();
		switch (action) {
			case CYCLE_ITEM_GROUP -> {
				if (index < 0 || index >= itemCount) {
					return;
				}
				this.itemGroups[index].set((itemGroup(index) + 1) % itemCount);
				this.confirmation.set(0);
			}
			case CYCLE_FLUID_GROUP -> {
				if (index < 0 || index >= recipe.fluidOutputs().size()) {
					return;
				}
				this.fluidGroups[index].set((fluidGroup(index) + 1) % itemCount);
				this.confirmation.set(0);
			}
			case CYCLE_ENERGY_GROUP -> {
				if (recipe.energyOutput() <= 0) {
					return;
				}
				this.energyGroup.set((energyGroup() + 1) % itemCount);
				this.confirmation.set(0);
			}
			case RESET_ASSIGNMENTS -> resetAssignments();
			case ARM_SPLIT -> {
				this.confirmation.set(1);
				player.sendSystemMessage(Component.translatable("message.blackbox.core_distribution_matrix.armed"));
			}
			case CONFIRM_SPLIT -> {
				if (isAwaitingConfirmation()) {
					split(player, recipe);
				}
			}
			default -> {
				return;
			}
		}
		this.broadcastChanges();
	}

	private boolean canConfigure(ServerPlayer player, boolean notify) {
		if (this.matrix == null || !this.matrix.getItem(CoreDistributionMatrixBlockEntity.SPLITTER_SLOT).is(BlackboxModItems.CORE_SPLITTER.get())
				|| !core().is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core())
				|| FarmCoreData.isVillageArchiveCore(core()) || FarmCoreData.isSplitOutputCore(core()) || !FarmCoreData.canManage(core(), player)) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.blackbox.core_distribution_matrix.invalid"));
			}
			return false;
		}
		FarmCoreData.Recipe recipe = recipe();
		int outputs = recipe.outputTemplates().size();
		if (!recipe.isValid() || outputs < 2) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.blackbox.core_splitter.single_output"));
			}
			return false;
		}
		if (outputs > MAX_ITEM_OUTPUTS || recipe.fluidOutputs().size() > MAX_FLUID_OUTPUTS) {
			if (notify) {
				player.sendSystemMessage(Component.translatable("message.blackbox.core_distribution_matrix.too_many_outputs", MAX_ITEM_OUTPUTS));
			}
			return false;
		}
		return true;
	}

	private void resetAssignments() {
		this.confirmation.set(0);
		FarmCoreData.Recipe recipe = recipe();
		int itemCount = Math.min(recipe.outputTemplates().size(), MAX_ITEM_OUTPUTS);
		for (int index = 0; index < this.itemGroups.length; index++) {
			this.itemGroups[index].set(index < itemCount ? index : -1);
		}
		for (int index = 0; index < this.fluidGroups.length; index++) {
			this.fluidGroups[index].set(index < recipe.fluidOutputs().size() && itemCount > 0 ? 0 : -1);
		}
		this.energyGroup.set(recipe.energyOutput() > 0 && itemCount > 0 ? 0 : -1);
	}

	private void split(ServerPlayer player, FarmCoreData.Recipe recipe) {
		int itemCount = recipe.outputTemplates().size();
		Map<Integer, List<FarmCoreData.StackAmount>> outputGroups = new LinkedHashMap<>();
		for (int index = 0; index < itemCount; index++) {
			int group = itemGroup(index);
			if (group < 0 || group >= itemCount) {
				resetAssignments();
				return;
			}
			outputGroups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(recipe.outputTemplates().get(index));
		}
		ItemStack source = core().copyWithCount(1);
		List<ItemStack> results = new ArrayList<>();
		for (Map.Entry<Integer, List<FarmCoreData.StackAmount>> entry : outputGroups.entrySet()) {
			int group = entry.getKey();
			List<FarmCoreData.FluidAmount> fluids = new ArrayList<>();
			for (int index = 0; index < recipe.fluidOutputs().size(); index++) {
				if (fluidGroup(index) == group) {
					fluids.add(recipe.fluidOutputs().get(index));
				}
			}
			results.add(FarmCoreData.createDistributedOutputCore(source, recipe, entry.getValue(), fluids, energyGroup() == group, player.level().registryAccess()));
		}

		if (!player.getAbilities().instabuild) {
			this.matrix.removeItem(CoreDistributionMatrixBlockEntity.SPLITTER_SLOT, 1);
		}
		this.matrix.setItem(CoreDistributionMatrixBlockEntity.CORE_SLOT, ItemStack.EMPTY);
		FarmCoreData.getCoreId(source).ifPresent(coreId -> FarmDimensionRuntime.deleteFarmCell(player.server, coreId));
		for (ItemStack result : results) {
			ItemHandlerHelper.giveItemToPlayer(player, result);
		}
		this.confirmation.set(0);
		this.matrix.setChanged();
		player.sendSystemMessage(Component.translatable("message.blackbox.core_splitter.completed", results.size()));
	}

	private boolean isMatrixAt(ServerPlayer player) {
		BlockPos pos = new BlockPos(this.x, this.y, this.z);
		return pos.closerToCenterThan(player.position(), 8.0D)
				&& player.level().getBlockState(pos).is(BlackboxModBlocks.CORE_DISTRIBUTION_MATRIX.get())
				&& player.level().getBlockEntity(pos) == this.matrix;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (this.matrix == null || index < 0 || index >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();
		if (index < CoreDistributionMatrixBlockEntity.SLOT_COUNT) {
			if (!this.moveItemStackTo(source, CoreDistributionMatrixBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.DIMENSION_CORE.get())) {
			if (!this.moveItemStackTo(source, CoreDistributionMatrixBlockEntity.CORE_SLOT, CoreDistributionMatrixBlockEntity.CORE_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (source.is(BlackboxModItems.CORE_SPLITTER.get())) {
			if (!this.moveItemStackTo(source, CoreDistributionMatrixBlockEntity.SPLITTER_SLOT, CoreDistributionMatrixBlockEntity.SPLITTER_SLOT + 1, false)) {
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
		return this.matrix != null && AbstractContainerMenu.stillValid(this.access, player, BlackboxModBlocks.CORE_DISTRIBUTION_MATRIX.get());
	}
}
