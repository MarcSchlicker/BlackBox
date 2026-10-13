package io.github.marcschlicker.blackbox.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.world.inventory.BlackBoxGuiMenu;
import io.github.marcschlicker.blackbox.util.FarmSimulationMachine;
import io.github.marcschlicker.blackbox.util.FarmMeasurementHost;
import io.github.marcschlicker.blackbox.util.FarmResourceStorage;
import io.github.marcschlicker.blackbox.util.MobInputStorage;
import io.github.marcschlicker.blackbox.util.DurabilityOutputStorage;
import io.github.marcschlicker.blackbox.util.MeasurementOutputBuffer;
import io.github.marcschlicker.blackbox.util.BlackboxSideMode;
import io.github.marcschlicker.blackbox.util.BlackboxSidedEnergyStorage;
import io.github.marcschlicker.blackbox.util.BlackboxSidedFluidHandler;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import io.netty.buffer.Unpooled;

import javax.annotation.Nullable;

public class BlackboxBlockBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, FarmMeasurementHost {
	public static final int CORE_SLOT = FarmSimulationMachine.CORE_SLOT;
	public static final int INPUT_START = FarmSimulationMachine.INPUT_START;
	public static final int INPUT_END = FarmSimulationMachine.INPUT_END;
	public static final int OUTPUT_START = FarmSimulationMachine.OUTPUT_START;
	public static final int OUTPUT_END = FarmSimulationMachine.OUTPUT_END;
	public static final int UPGRADE_SLOT = FarmSimulationMachine.UPGRADE_SLOT;
	public static final int FILTER_UPGRADE_SLOT = FarmSimulationMachine.SLOT_COUNT;
	public static final int FLUID_INPUT_CONTAINER_SLOT = FILTER_UPGRADE_SLOT + 1;
	public static final int FLUID_OUTPUT_CONTAINER_SLOT = FLUID_INPUT_CONTAINER_SLOT + 1;
	public static final int SLOT_COUNT = FLUID_OUTPUT_CONTAINER_SLOT + 1;
	private static final int[] INPUT_SLOTS = java.util.stream.IntStream.range(INPUT_START, INPUT_END).toArray();
	private static final int[] OUTPUT_SLOTS = java.util.stream.IntStream.range(OUTPUT_START, OUTPUT_END).toArray();
	private static final int[] AUTOMATION_SLOTS = java.util.stream.IntStream.range(INPUT_START, OUTPUT_END).toArray();
	private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int simulationTicks;
	private String activeCoreId = "";
	private boolean stableCycleFunded;
	private long simulationCycleSeed;
	private int calculationPhase;
	private int calculationTicksRemaining;
	private int calculationTotalTicks;
	private int calculationItemsPerMinute;
	private final FarmResourceStorage resources = new FarmResourceStorage(this::markResourceChanged);
	private final MobInputStorage mobInputs = new MobInputStorage(this::setChanged);
	private final MeasurementOutputBuffer outputBuffer = new MeasurementOutputBuffer(this::markResourceChanged);
	private final MeasurementOutputBuffer measurementProgress = new MeasurementOutputBuffer(this::markResourceChanged);
	private final DurabilityOutputStorage durabilityOutputs = new DurabilityOutputStorage(this::setChanged);
	private final BlackboxSideMode[] sideModes = defaultSideModes();
	private boolean resourceSyncPending;

	public BlackboxBlockBlockEntity(BlockPos position, BlockState state) {
		this(BlackboxModBlockEntities.BLACKBOX_BLOCK.get(), position, state);
	}

	protected BlackboxBlockBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos position, BlockState state) {
		super(type, position, state);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(tag, lookupProvider);
		this.stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, this.stacks, lookupProvider);
		this.simulationTicks = tag.getInt("SimulationTicks");
		this.activeCoreId = tag.getString("ActiveCoreId");
		this.stableCycleFunded = tag.getBoolean("StableCycleFunded");
		this.simulationCycleSeed = tag.getLong("SimulationCycleSeed");
		this.calculationPhase = tag.getInt("CalculationPhase");
		this.calculationTicksRemaining = tag.getInt("CalculationTicksRemaining");
		this.calculationTotalTicks = tag.getInt("CalculationTotalTicks");
		this.calculationItemsPerMinute = tag.getInt("CalculationItemsPerMinute");
		this.resources.load(tag, lookupProvider);
		this.mobInputs.load(tag.getList("MobInputs", net.minecraft.nbt.Tag.TAG_COMPOUND));
		this.outputBuffer.load(tag.getCompound("PendingOutputBuffer"), lookupProvider);
		this.measurementProgress.load(tag.getCompound("MeasurementProgress"), lookupProvider);
		this.durabilityOutputs.load(tag.getCompound("DurabilityOutputRemainders"), lookupProvider);
		for (Direction direction : Direction.values()) {
			String key = "SideMode_" + direction.getSerializedName();
			if (tag.contains(key)) {
				int ordinal = tag.getInt(key);
				this.sideModes[direction.get3DDataValue()] = ordinal >= 0 && ordinal < BlackboxSideMode.values().length
						? BlackboxSideMode.values()[ordinal] : defaultMode(direction);
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(tag, lookupProvider);
		ContainerHelper.saveAllItems(tag, this.stacks, lookupProvider);
		tag.putInt("SimulationTicks", this.simulationTicks);
		tag.putString("ActiveCoreId", this.activeCoreId);
		tag.putBoolean("StableCycleFunded", this.stableCycleFunded);
		tag.putLong("SimulationCycleSeed", this.simulationCycleSeed);
		tag.putInt("CalculationPhase", this.calculationPhase);
		tag.putInt("CalculationTicksRemaining", this.calculationTicksRemaining);
		tag.putInt("CalculationTotalTicks", this.calculationTotalTicks);
		tag.putInt("CalculationItemsPerMinute", this.calculationItemsPerMinute);
		this.resources.save(tag, lookupProvider);
		tag.put("MobInputs", this.mobInputs.save());
		tag.put("PendingOutputBuffer", this.outputBuffer.save(lookupProvider));
		tag.put("MeasurementProgress", this.measurementProgress.save(lookupProvider));
		tag.put("DurabilityOutputRemainders", this.durabilityOutputs.save(lookupProvider));
		for (Direction direction : Direction.values()) {
			tag.putInt("SideMode_" + direction.getSerializedName(), getSideMode(direction).ordinal());
		}
	}

	/** Imports the complete data payload written by the retired Dimensional Workbench. */
	public void loadLegacyWorkbenchData(CompoundTag legacyData, HolderLookup.Provider lookupProvider) {
		CompoundTag migrated = legacyData.copy();
		if (!migrated.contains("PendingOutputBuffer") && migrated.contains("MeasurementOutputBuffer")) {
			migrated.put("PendingOutputBuffer", migrated.getCompound("MeasurementOutputBuffer").copy());
		}
		loadAdditional(migrated, lookupProvider);
		setChanged();
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.blackbox.blackbox_block");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public int getContainerSize() {
		return SLOT_COUNT;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new BlackBoxGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == CORE_SLOT) {
			return stack.is(BlackboxModItems.DIMENSION_CORE.get());
		}
		if (slot == UPGRADE_SLOT) {
			return stack.is(BlackboxModItems.STABILITY_UPGRADE.get());
		}
		if (slot == FILTER_UPGRADE_SLOT) {
			return stack.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get());
		}
		if (slot == FLUID_INPUT_CONTAINER_SLOT) {
			return isFilledFluidContainer(stack);
		}
		if (slot == FLUID_OUTPUT_CONTAINER_SLOT) {
			return stack.is(Items.BUCKET);
		}
		return slot >= INPUT_START && slot < INPUT_END;
	}

	public static boolean isFilledFluidContainer(ItemStack stack) {
		return FluidUtil.getFluidContained(stack).isPresent();
	}

	public void processFluidContainers() {
		ItemStack filledContainer = getItem(FLUID_INPUT_CONTAINER_SLOT);
		if (isFilledFluidContainer(filledContainer)) {
			FluidActionResult result = FluidUtil.tryEmptyContainer(filledContainer, this.resources.inputFluids(), 1000, null, true);
			if (result.isSuccess()) {
				setItem(FLUID_INPUT_CONTAINER_SLOT, result.getResult());
			}
		}

		ItemStack emptyBucket = getItem(FLUID_OUTPUT_CONTAINER_SLOT);
		if (emptyBucket.is(Items.BUCKET)) {
			FluidActionResult result = FluidUtil.tryFillContainer(emptyBucket, this.resources.outputFluids(), 1000, null, true);
			if (result.isSuccess()) {
				setItem(FLUID_OUTPUT_CONTAINER_SLOT, result.getResult());
			}
		}
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == null) {
			return AUTOMATION_SLOTS;
		}
		return getSideMode(side) == BlackboxSideMode.ITEM_BOTH ? AUTOMATION_SLOTS : getSideMode(side) == BlackboxSideMode.ITEM_INPUT ? INPUT_SLOTS
				: getSideMode(side) == BlackboxSideMode.ITEM_OUTPUT ? OUTPUT_SLOTS : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == null ? canPlaceItem(slot, stack) : (getSideMode(side) == BlackboxSideMode.ITEM_INPUT || getSideMode(side) == BlackboxSideMode.ITEM_BOTH)
				&& slot >= INPUT_START && slot < INPUT_END && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == null ? slot >= OUTPUT_START && slot < OUTPUT_END
				: (getSideMode(side) == BlackboxSideMode.ITEM_OUTPUT || getSideMode(side) == BlackboxSideMode.ITEM_BOTH)
						&& slot >= OUTPUT_START && slot < OUTPUT_END;
	}

	public BlackboxSideMode getSideMode(Direction side) {
		return side == null ? BlackboxSideMode.DISABLED : this.sideModes[side.get3DDataValue()];
	}

	public void cycleSideMode(Direction side) {
		if (side != null) {
			this.sideModes[side.get3DDataValue()] = getSideMode(side).next();
			this.setChanged();
			if (this.level != null) {
				this.level.invalidateCapabilities(this.worldPosition);
			}
		}
	}

	public boolean consumeResourceSyncPending() {
		boolean pending = this.resourceSyncPending;
		this.resourceSyncPending = false;
		return pending;
	}

	public void syncToClient() {
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public boolean hasItemAccess(@Nullable Direction side) {
		return side == null || getSideMode(side) == BlackboxSideMode.ITEM_INPUT || getSideMode(side) == BlackboxSideMode.ITEM_OUTPUT
				|| getSideMode(side) == BlackboxSideMode.ITEM_BOTH;
	}

	public IFluidHandler fluidCapability(@Nullable Direction side) {
		if (side == null) {
			return new BlackboxSidedFluidHandler(this.resources.inputFluids(), this.resources.outputFluids(), true, true);
		}
		BlackboxSideMode mode = getSideMode(side);
		return mode == BlackboxSideMode.FLUID_INPUT ? new BlackboxSidedFluidHandler(this.resources.inputFluids(), this.resources.outputFluids(), true, false)
				: mode == BlackboxSideMode.FLUID_OUTPUT ? new BlackboxSidedFluidHandler(this.resources.inputFluids(), this.resources.outputFluids(), false, true)
				: mode == BlackboxSideMode.FLUID_BOTH ? new BlackboxSidedFluidHandler(this.resources.inputFluids(), this.resources.outputFluids(), true, true) : null;
	}

	public IEnergyStorage energyCapability(@Nullable Direction side) {
		if (side == null) {
			return new BlackboxSidedEnergyStorage(this.resources.inputEnergy(), this.resources.outputEnergy(), true, true);
		}
		BlackboxSideMode mode = getSideMode(side);
		return mode == BlackboxSideMode.ENERGY_INPUT ? new BlackboxSidedEnergyStorage(this.resources.inputEnergy(), this.resources.outputEnergy(), true, false)
				: mode == BlackboxSideMode.ENERGY_OUTPUT ? new BlackboxSidedEnergyStorage(this.resources.inputEnergy(), this.resources.outputEnergy(), false, true)
				: mode == BlackboxSideMode.ENERGY_BOTH ? new BlackboxSidedEnergyStorage(this.resources.inputEnergy(), this.resources.outputEnergy(), true, true) : null;
	}

	public int getSimulationTicks() {
		return this.simulationTicks;
	}

	public void setSimulationTicks(int simulationTicks) {
		if (this.simulationTicks != simulationTicks) {
			this.simulationTicks = simulationTicks;
			this.setChanged();
		}
	}

	public String getActiveCoreId() {
		return this.activeCoreId;
	}

	public void setActiveCoreId(String activeCoreId) {
		if (!this.activeCoreId.equals(activeCoreId)) {
			this.activeCoreId = activeCoreId;
			this.setChanged();
		}
	}

	@Override
	public boolean isStableCycleFunded() {
		return this.stableCycleFunded;
	}

	@Override
	public void setStableCycleFunded(boolean funded) {
		if (this.stableCycleFunded != funded) {
			this.stableCycleFunded = funded;
			this.setChanged();
		}
	}

	@Override
	public long getSimulationCycleSeed() {
		return this.simulationCycleSeed;
	}

	@Override
	public void setSimulationCycleSeed(long seed) {
		if (this.simulationCycleSeed != seed) {
			this.simulationCycleSeed = seed;
			this.setChanged();
		}
	}

	@Override
	public int getCalculationPhase() {
		return this.calculationPhase;
	}

	@Override
	public int getCalculationTicksRemaining() {
		return this.calculationTicksRemaining;
	}

	@Override
	public int getCalculationTotalTicks() {
		return this.calculationTotalTicks;
	}

	@Override
	public int getCalculationItemsPerMinute() {
		return this.calculationItemsPerMinute;
	}

	@Override
	public void setCalculationState(int phase, int ticksRemaining, int totalTicks, int itemsPerMinute) {
		int safePhase = Math.max(0, phase);
		int safeRemaining = Math.max(0, ticksRemaining);
		int safeTotal = Math.max(0, totalTicks);
		int safeRate = Math.max(0, itemsPerMinute);
		if (this.calculationPhase != safePhase || this.calculationTicksRemaining != safeRemaining
				|| this.calculationTotalTicks != safeTotal || this.calculationItemsPerMinute != safeRate) {
			if (safePhase == 1 && this.calculationPhase != 1) {
				this.measurementProgress.clear();
			}
			this.calculationPhase = safePhase;
			this.calculationTicksRemaining = safeRemaining;
			this.calculationTotalTicks = safeTotal;
			this.calculationItemsPerMinute = safeRate;
			this.setChanged();
		}
	}

	@Override
	public FarmResourceStorage resources() {
		return this.resources;
	}

	@Override
	public MobInputStorage mobInputs() {
		return this.mobInputs;
	}

	@Override
	public MeasurementOutputBuffer outputBuffer() {
		return this.outputBuffer;
	}

	@Override
	public MeasurementOutputBuffer measurementOutputBuffer() {
		return this.outputBuffer;
	}

	@Override
	public MeasurementOutputBuffer measurementProgress() {
		return this.measurementProgress;
	}

	@Override
	public void setMeasurementProgress(java.util.List<io.github.marcschlicker.blackbox.util.FarmCoreData.StackAmount> items,
			java.util.List<io.github.marcschlicker.blackbox.util.FarmCoreData.FluidAmount> fluids, long energy) {
		this.measurementProgress.replaceWith(items, fluids, energy);
	}

	@Override
	public DurabilityOutputStorage durabilityOutputs() {
		return this.durabilityOutputs;
	}

	private static BlackboxSideMode[] defaultSideModes() {
		BlackboxSideMode[] modes = new BlackboxSideMode[Direction.values().length];
		for (Direction direction : Direction.values()) {
			modes[direction.get3DDataValue()] = defaultMode(direction);
		}
		return modes;
	}

	private static BlackboxSideMode defaultMode(Direction direction) {
		return switch (direction) {
			case UP -> BlackboxSideMode.ITEM_INPUT;
			case DOWN -> BlackboxSideMode.ITEM_OUTPUT;
			case NORTH -> BlackboxSideMode.FLUID_INPUT;
			case SOUTH -> BlackboxSideMode.FLUID_OUTPUT;
			case WEST -> BlackboxSideMode.ENERGY_INPUT;
			case EAST -> BlackboxSideMode.ENERGY_OUTPUT;
		};
	}

	private void markResourceChanged() {
		this.resourceSyncPending = true;
		this.setChanged();
	}
}
