package io.github.marcschlicker.blackbox.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.util.FarmSimulationMachine;
import io.github.marcschlicker.blackbox.util.FarmMeasurementHost;
import io.github.marcschlicker.blackbox.util.FarmResourceStorage;
import io.github.marcschlicker.blackbox.util.MobInputStorage;
import io.github.marcschlicker.blackbox.util.MeasurementOutputBuffer;
import io.github.marcschlicker.blackbox.util.DurabilityOutputStorage;
import io.github.marcschlicker.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;

import io.netty.buffer.Unpooled;

import javax.annotation.Nullable;

public class DimensionalWorkbenchBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, FarmMeasurementHost {
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
	private final MeasurementOutputBuffer measurementOutputBuffer = new MeasurementOutputBuffer(this::markResourceChanged);
	private final MeasurementOutputBuffer measurementProgress = new MeasurementOutputBuffer(this::markResourceChanged);
	private final DurabilityOutputStorage durabilityOutputs = new DurabilityOutputStorage(this::markResourceChanged);
	private boolean resourceSyncPending;

	public DimensionalWorkbenchBlockEntity(BlockPos position, BlockState state) {
		super(BlackboxModBlockEntities.DIMENSIONAL_WORKBENCH.get(), position, state);
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
		this.measurementOutputBuffer.load(tag.getCompound("MeasurementOutputBuffer"), lookupProvider);
		this.measurementProgress.load(tag.getCompound("MeasurementProgress"), lookupProvider);
		this.durabilityOutputs.load(tag.getCompound("DurabilityOutputRemainders"), lookupProvider);
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
		tag.put("MeasurementOutputBuffer", this.measurementOutputBuffer.save(lookupProvider));
		tag.put("MeasurementProgress", this.measurementProgress.save(lookupProvider));
		tag.put("DurabilityOutputRemainders", this.durabilityOutputs.save(lookupProvider));
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
		return Component.translatable("block.blackbox.dimensional_workbench");
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
		return new DimensionalWorkbenchGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == CORE_SLOT) {
			return stack.is(BlackboxModItems.DIMENSION_CORE.get());
		}
		if (slot == UPGRADE_SLOT) {
			return stack.is(BlackboxModItems.STABILITY_UPGRADE.get());
		}
		return slot >= INPUT_START && slot < INPUT_END;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return AUTOMATION_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= OUTPUT_START && slot < OUTPUT_END;
	}

	@Override
	public int getSimulationTicks() {
		return this.simulationTicks;
	}

	@Override
	public void setSimulationTicks(int ticks) {
		if (this.simulationTicks != ticks) {
			this.simulationTicks = ticks;
			this.setChanged();
		}
	}

	@Override
	public String getActiveCoreId() {
		return this.activeCoreId;
	}

	@Override
	public void setActiveCoreId(String coreId) {
		if (!this.activeCoreId.equals(coreId)) {
			this.activeCoreId = coreId;
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

	public int getCalculationPhase() {
		return this.calculationPhase;
	}

	public int getCalculationTicksRemaining() {
		return this.calculationTicksRemaining;
	}

	public int getCalculationTotalTicks() {
		return this.calculationTotalTicks;
	}

	public int getCalculationItemsPerMinute() {
		return this.calculationItemsPerMinute;
	}

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

	public MeasurementOutputBuffer measurementOutputBuffer() {
		return this.measurementOutputBuffer;
	}

	@Override
	public MeasurementOutputBuffer outputBuffer() {
		return this.measurementOutputBuffer;
	}

	public MeasurementOutputBuffer measurementProgress() {
		return this.measurementProgress;
	}

	public void setMeasurementProgress(java.util.List<io.github.marcschlicker.blackbox.util.FarmCoreData.StackAmount> items,
			java.util.List<io.github.marcschlicker.blackbox.util.FarmCoreData.FluidAmount> fluids, long energy) {
		this.measurementProgress.replaceWith(items, fluids, energy);
	}

	@Override
	public DurabilityOutputStorage durabilityOutputs() {
		return this.durabilityOutputs;
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

	private void markResourceChanged() {
		this.resourceSyncPending = true;
		this.setChanged();
	}
}
