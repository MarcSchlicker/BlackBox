package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.block.entity.InputblockBlockEntity;
import net.mcreator.blackbox.block.entity.OutputBlockBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber
public final class FarmDimensionRuntime {
	public static final ResourceKey<Level> FARM_DIMENSION = FarmEnvironment.STANDARD.dimension();
	public static final int WARMUP_TICKS = 30 * 20;
	public static final int SAMPLE_TICKS = 60 * 20;
	private static final int TOTAL_TICKS = WARMUP_TICKS + SAMPLE_TICKS;

	private static final String DATA_CORE_ID = "BlackboxFarmCoreId";
	private static final String DATA_WORKBENCH_X = "BlackboxWorkbenchX";
	private static final String DATA_WORKBENCH_Y = "BlackboxWorkbenchY";
	private static final String DATA_WORKBENCH_Z = "BlackboxWorkbenchZ";
	private static final String DATA_WORKBENCH_DIMENSION = "BlackboxWorkbenchDimension";
	private static final String DATA_RETURN_X = "BlackboxReturnX";
	private static final String DATA_RETURN_Y = "BlackboxReturnY";
	private static final String DATA_RETURN_Z = "BlackboxReturnZ";
	private static final String DATA_FARM_DIMENSION = "BlackboxFarmDimension";
	private static final Map<UUID, FarmMeasurement> ACTIVE_MEASUREMENTS = new HashMap<>();
	private static final TicketType<UUID> MEASUREMENT_TICKET = TicketType.create("blackbox:farm_measurement", Comparator.<UUID>naturalOrder());

	private FarmDimensionRuntime() {
	}

	public static FarmCell enterFarmDimension(ServerPlayer player, ServerLevel farmLevel, BlockPos workbenchPos, ResourceKey<Level> workbenchDimension) {
		farmLevel.getWorldBorder().setCenter(0.0D, 0.0D);
		farmLevel.getWorldBorder().setSize(59_999_968.0D);
		IItemHandler workbench = getItemHandler((ServerLevel) player.level(), workbenchPos);
		int coreSlot = findDimensionCoreSlot(workbench);
		if (coreSlot < 0 || !(workbench instanceof IItemHandlerModifiable modifiable)) {
			player.sendSystemMessage(Component.translatable("message.blackbox.measurement.no_core").withStyle(ChatFormatting.RED));
			return null;
		}

		ItemStack core = workbench.getStackInSlot(coreSlot).copyWithCount(1);
		UUID coreId = FarmCoreData.ensureCoreId(core);
		modifiable.setStackInSlot(coreSlot, core);
		FarmMeasurement runningMeasurement = ACTIVE_MEASUREMENTS.remove(coreId);
		if (runningMeasurement != null) {
			cancelMeasurement(player.server, runningMeasurement);
			player.sendSystemMessage(Component.translatable("message.blackbox.measurement.cancelled_for_editing").withStyle(ChatFormatting.YELLOW));
		}

		FarmCell cell = FarmCell.fromCoreId(coreId);
		CompoundTag data = player.getPersistentData();
		data.putString(DATA_CORE_ID, coreId.toString());
		data.putInt(DATA_WORKBENCH_X, workbenchPos.getX());
		data.putInt(DATA_WORKBENCH_Y, workbenchPos.getY());
		data.putInt(DATA_WORKBENCH_Z, workbenchPos.getZ());
		data.putString(DATA_WORKBENCH_DIMENSION, workbenchDimension.location().toString());
		data.putDouble(DATA_RETURN_X, player.getX());
		data.putDouble(DATA_RETURN_Y, player.getY());
		data.putDouble(DATA_RETURN_Z, player.getZ());
		data.putString(DATA_FARM_DIMENSION, farmLevel.dimension().location().toString());
		FarmMobSpawnRules.setCellEnabled(farmLevel, cell, FarmCoreData.isMobSpawningEnabled(core));
		prepareCell(farmLevel, cell);
		if (FarmCoreData.isExampleIronFarm(core)) {
			ExampleIronFarmBuilder.ensureBuilt(farmLevel, cell);
		}
		player.sendSystemMessage(Component.translatable("message.blackbox.farm.entered").withStyle(ChatFormatting.AQUA));
		return cell;
	}

	public static boolean returnFromFarm(ServerPlayer player) {
		if (!FarmEnvironment.isFarmDimension(player.level().dimension())) {
			return false;
		}
		CompoundTag data = player.getPersistentData();
		ResourceLocation dimensionId = ResourceLocation.tryParse(data.getString(DATA_WORKBENCH_DIMENSION));
		ServerLevel destination = dimensionId == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (destination == null) {
			destination = player.server.overworld();
		}
		double x = data.contains(DATA_RETURN_X) ? data.getDouble(DATA_RETURN_X) : destination.getSharedSpawnPos().getX() + 0.5D;
		double y = data.contains(DATA_RETURN_Y) ? data.getDouble(DATA_RETURN_Y) : destination.getSharedSpawnPos().getY();
		double z = data.contains(DATA_RETURN_Z) ? data.getDouble(DATA_RETURN_Z) : destination.getSharedSpawnPos().getZ() + 0.5D;
		player.teleportTo(destination, x, y, z, player.getYRot(), player.getXRot());
		startMeasurement(player);
		return true;
	}

	public static Optional<FarmCell> getAssignedCell(ServerPlayer player) {
		String value = player.getPersistentData().getString(DATA_CORE_ID);
		try {
			return value.isBlank() ? Optional.empty() : Optional.of(FarmCell.fromCoreId(UUID.fromString(value)));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	public static String getAssignedFarmName(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		ResourceLocation dimensionId = ResourceLocation.tryParse(data.getString(DATA_WORKBENCH_DIMENSION));
		ServerLevel workbenchLevel = dimensionId == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (workbenchLevel == null) {
			return "";
		}
		BlockPos workbenchPos = new BlockPos(data.getInt(DATA_WORKBENCH_X), data.getInt(DATA_WORKBENCH_Y), data.getInt(DATA_WORKBENCH_Z));
		IItemHandler handler = getItemHandler(workbenchLevel, workbenchPos);
		Optional<FarmCell> assignedCell = getAssignedCell(player);
		if (handler == null || assignedCell.isEmpty()) {
			return "";
		}
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (FarmCoreData.getCoreId(stack).filter(assignedCell.get().coreId()::equals).isPresent()) {
				return FarmCoreData.getFarmName(stack);
			}
		}
		return "";
	}

	public static ResourceKey<Level> resolveFarmDimension(ServerPlayer player, BlockPos workbenchPos) {
		IItemHandler workbench = getItemHandler((ServerLevel) player.level(), workbenchPos);
		int coreSlot = findDimensionCoreSlot(workbench);
		if (coreSlot < 0) {
			return FarmEnvironment.STANDARD.dimension();
		}
		return FarmCoreData.getEnvironment(workbench.getStackInSlot(coreSlot)).dimension();
	}

	public static boolean isMeasurementActive(ItemStack core) {
		return FarmCoreData.getCoreId(core).map(ACTIVE_MEASUREMENTS::containsKey).orElse(false);
	}

	public static void prepareCell(ServerLevel level, FarmCell cell) {
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
		if (level.getBlockState(cell.inputPos()).isAir()) {
			level.setBlock(cell.inputPos(), BlackboxModBlocks.INPUTBLOCK.get().defaultBlockState(), 3);
		}
		if (level.getBlockState(cell.outputPos()).isAir()) {
			level.setBlock(cell.outputPos(), BlackboxModBlocks.OUTPUT_BLOCK.get().defaultBlockState(), 3);
		}
	}

	public static void startMeasurement(ServerPlayer player) {
		Optional<FarmCell> assignedCell = getAssignedCell(player);
		if (assignedCell.isEmpty() || ACTIVE_MEASUREMENTS.containsKey(assignedCell.get().coreId())) {
			return;
		}
		CompoundTag data = player.getPersistentData();
		BlockPos workbenchPos = new BlockPos(data.getInt(DATA_WORKBENCH_X), data.getInt(DATA_WORKBENCH_Y), data.getInt(DATA_WORKBENCH_Z));
		FarmMeasurement measurement = new FarmMeasurement(assignedCell.get(), player.getUUID(), workbenchPos, data.getString(DATA_WORKBENCH_DIMENSION), data.getString(DATA_FARM_DIMENSION));
		ServerLevel farmLevel = getFarmLevel(player.server, measurement);
		if (farmLevel == null) {
			return;
		}
		setCellTicket(farmLevel, measurement.cell, true);
		ACTIVE_MEASUREMENTS.put(measurement.cell.coreId(), measurement);
		setWorkbenchCalculationState(player.server, measurement, 1, 0);
		player.sendSystemMessage(Component.translatable("message.blackbox.measurement.started_after_exit").withStyle(ChatFormatting.YELLOW));
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ACTIVE_MEASUREMENTS.isEmpty()) {
			return;
		}
		MinecraftServer server = event.getServer();
		List<UUID> completed = new ArrayList<>();
		for (FarmMeasurement measurement : List.copyOf(ACTIVE_MEASUREMENTS.values())) {
			ServerLevel farmLevel = getFarmLevel(server, measurement);
			if (farmLevel == null) {
				continue;
			}
			measurement.elapsedTicks++;
			transferWorkbenchInput(server, farmLevel, measurement);
			transferFarmOutput(server, farmLevel, measurement);
			if (measurement.cell.coreId().equals(FarmCoreData.EXAMPLE_IRON_FARM_ID) && measurement.elapsedTicks >= WARMUP_TICKS && measurement.elapsedTicks % 200 == 0) {
				ExampleIronFarmBuilder.spawnTestGolem(farmLevel, measurement.cell);
			}
			if (measurement.elapsedTicks == WARMUP_TICKS) {
				measurement.baselineInventory = scanCellInventories(farmLevel, measurement.cell);
				measurement.lastTimelineSnapshot = measurement.baselineInventory;
				measurement.exportedSinceBaseline = new MeasuredItems();
				setWorkbenchCalculationState(server, measurement, 2, SAMPLE_TICKS);
				notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.scanning_ports").withStyle(ChatFormatting.AQUA));
			}
			if (measurement.elapsedTicks > WARMUP_TICKS && measurement.elapsedTicks % 20 == 0) {
				recordProductionTimeline(farmLevel, measurement);
			}
			if (measurement.elapsedTicks >= TOTAL_TICKS) {
				finishMeasurement(server, farmLevel, measurement);
				completed.add(measurement.cell.coreId());
			} else if (measurement.elapsedTicks > WARMUP_TICKS && measurement.elapsedTicks % 20 == 0) {
				setWorkbenchCalculationState(server, measurement, 2, TOTAL_TICKS - measurement.elapsedTicks);
			}
			if (measurement.elapsedTicks % 100 == 0 && measurement.elapsedTicks < TOTAL_TICKS) {
				showProgress(server, measurement);
			}
		}
		completed.forEach(ACTIVE_MEASUREMENTS::remove);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && FarmEnvironment.isFarmDimension(event.getFrom()) && !FarmEnvironment.isFarmDimension(event.getTo())) {
			startMeasurement(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && FarmEnvironment.isFarmDimension(player.level().dimension())) {
			startMeasurement(player);
		}
	}

	private static void finishMeasurement(MinecraftServer server, ServerLevel farmLevel, FarmMeasurement measurement) {
		MeasuredItems availableInventory = new MeasuredItems();
		availableInventory.addAll(measurement.baselineInventory);
		availableInventory.addAll(measurement.importedSinceBaseline);
		MeasuredItems finalInventory = scanCellInventories(farmLevel, measurement.cell);
		finalInventory.addAll(measurement.exportedSinceBaseline);
		List<FarmCoreData.StackAmount> inputs = decreased(availableInventory, finalInventory);
		List<FarmCoreData.StackAmount> outputs = increased(availableInventory, finalInventory);
		List<FarmCoreData.ProductionEvent> timeline = normalizeTimeline(measurement.productionTimeline, outputs);
		boolean saved = !outputs.isEmpty() && writeSampleToWorkbench(server, measurement, inputs, outputs, timeline);
		if (outputs.isEmpty()) {
			notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.no_output_port").withStyle(ChatFormatting.RED));
		} else if (!saved) {
			notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.no_matching_core").withStyle(ChatFormatting.RED));
		} else {
			notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.saved_ports", inputs.size(), outputs.size()).withStyle(ChatFormatting.GREEN));
		}
		FarmMobSpawnRules.clearCell(farmLevel, measurement.cell);
		setCellTicket(farmLevel, measurement.cell, false);
		setWorkbenchCalculationState(server, measurement, 0, 0);
	}

	private static void showProgress(MinecraftServer server, FarmMeasurement measurement) {
		Component message;
		if (measurement.elapsedTicks < WARMUP_TICKS) {
			message = Component.translatable("message.blackbox.measurement.warmup_background").withStyle(ChatFormatting.YELLOW);
		} else {
			int seconds = (TOTAL_TICKS - measurement.elapsedTicks + 19) / 20;
			message = Component.translatable("message.blackbox.measurement.measuring_background", seconds).withStyle(ChatFormatting.AQUA);
		}
		notifyOwner(server, measurement, message);
	}

	private static void notifyOwner(MinecraftServer server, FarmMeasurement measurement, Component message) {
		ServerPlayer owner = server.getPlayerList().getPlayer(measurement.owner);
		if (owner != null) {
			owner.displayClientMessage(message, true);
		}
	}

	private static MeasuredItems scanCellInventories(ServerLevel level, FarmCell cell) {
		MeasuredItems items = new MeasuredItems();
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
					if (!cell.contains(blockEntity.getBlockPos())) {
						continue;
					}
					IItemHandler handler = getItemHandler(level, blockEntity.getBlockPos());
					if (handler != null) {
						for (int slot = 0; slot < handler.getSlots(); slot++) {
							items.add(handler.getStackInSlot(slot));
						}
					} else if (blockEntity instanceof Container container) {
						for (int slot = 0; slot < container.getContainerSize(); slot++) {
							items.add(container.getItem(slot));
						}
					}
				}
			}
		}
		return items;
	}

	private static List<FarmCoreData.StackAmount> decreased(MeasuredItems before, MeasuredItems after) {
		List<FarmCoreData.StackAmount> result = new ArrayList<>();
		for (FarmCoreData.StackAmount entry : before.entries()) {
			long amount = entry.amount() - after.amountOf(entry.stack());
			if (amount > 0) {
				result.add(new FarmCoreData.StackAmount(entry.stack(), amount));
			}
		}
		return result;
	}

	private static List<FarmCoreData.StackAmount> increased(MeasuredItems before, MeasuredItems after) {
		List<FarmCoreData.StackAmount> result = new ArrayList<>();
		for (FarmCoreData.StackAmount entry : after.entries()) {
			long amount = entry.amount() - before.amountOf(entry.stack());
			if (amount > 0) {
				result.add(new FarmCoreData.StackAmount(entry.stack(), amount));
			}
		}
		return result;
	}

	private static boolean writeSampleToWorkbench(MinecraftServer server, FarmMeasurement measurement, List<FarmCoreData.StackAmount> inputs, List<FarmCoreData.StackAmount> outputs,
			List<FarmCoreData.ProductionEvent> timeline) {
		ResourceLocation dimensionId = ResourceLocation.tryParse(measurement.workbenchDimension);
		ServerLevel workbenchLevel = dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (workbenchLevel == null) {
			return false;
		}
		IItemHandler handler = getItemHandler(workbenchLevel, measurement.workbenchPos);
		if (!(handler instanceof IItemHandlerModifiable modifiable)) {
			return false;
		}
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stored = handler.getStackInSlot(slot);
			if (!stored.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.getCoreId(stored).filter(measurement.cell.coreId()::equals).isPresent()) {
				continue;
			}
			ItemStack core = stored.copyWithCount(1);
			FarmCoreData.write(core, workbenchLevel.registryAccess(), inputs, outputs, timeline, WARMUP_TICKS, SAMPLE_TICKS);
			modifiable.setStackInSlot(slot, core);
			if (workbenchLevel.getBlockEntity(measurement.workbenchPos) instanceof DimensionalWorkbenchBlockEntity workbench) {
				workbench.setActiveCoreId(measurement.cell.coreId().toString());
				workbench.setSimulationTicks(0);
				workbench.setStableCycleFunded(false);
			}
			return true;
		}
		return false;
	}

	private static void cancelMeasurement(MinecraftServer server, FarmMeasurement measurement) {
		ServerLevel farmLevel = getFarmLevel(server, measurement);
		if (farmLevel != null) {
			FarmMobSpawnRules.clearCell(farmLevel, measurement.cell);
			setCellTicket(farmLevel, measurement.cell, false);
		}
		ResourceLocation dimensionId = ResourceLocation.tryParse(measurement.workbenchDimension);
		ServerLevel workbenchLevel = dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (workbenchLevel == null) {
			return;
		}
		setWorkbenchCalculationState(server, measurement, 0, 0);
		IItemHandler handler = getItemHandler(workbenchLevel, measurement.workbenchPos);
		if (!(handler instanceof IItemHandlerModifiable modifiable)) {
			return;
		}
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stored = handler.getStackInSlot(slot);
			if (stored.is(BlackboxModItems.DIMENSION_CORE.get()) && FarmCoreData.getCoreId(stored).filter(measurement.cell.coreId()::equals).isPresent()) {
				ItemStack core = stored.copyWithCount(1);
				FarmCoreData.clearProfile(core, workbenchLevel.registryAccess());
				modifiable.setStackInSlot(slot, core);
				return;
			}
		}
	}

	private static void transferFarmOutput(MinecraftServer server, ServerLevel farmLevel, FarmMeasurement measurement) {
		if (!(farmLevel.getBlockEntity(measurement.cell.outputPos()) instanceof OutputBlockBlockEntity output)) {
			return;
		}
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench == null) {
			return;
		}
		for (int slot = 0; slot < output.getContainerSize(); slot++) {
			ItemStack stored = output.getItem(slot);
			if (stored.isEmpty()) {
				continue;
			}
			ItemStack template = stored.copyWithCount(1);
			int moved = insertIntoWorkbench(workbench, stored.copy());
			if (moved <= 0) {
				continue;
			}
			stored.shrink(moved);
			if (stored.isEmpty()) {
				output.setItem(slot, ItemStack.EMPTY);
			} else {
				output.setChanged();
			}
			if (measurement.elapsedTicks > WARMUP_TICKS) {
				measurement.exportedSinceBaseline.add(template, moved);
			}
		}
		workbench.setChanged();
	}

	private static void transferWorkbenchInput(MinecraftServer server, ServerLevel farmLevel, FarmMeasurement measurement) {
		if (!(farmLevel.getBlockEntity(measurement.cell.inputPos()) instanceof InputblockBlockEntity input)) {
			return;
		}
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench == null) {
			return;
		}
		for (int sourceSlot = FarmSimulationMachine.INPUT_START; sourceSlot < FarmSimulationMachine.INPUT_END; sourceSlot++) {
			ItemStack source = workbench.getItem(sourceSlot);
			if (source.isEmpty()) {
				continue;
			}
			ItemStack template = source.copyWithCount(1);
			int before = source.getCount();
			for (int targetSlot = 0; targetSlot < input.getContainerSize() && !source.isEmpty(); targetSlot++) {
				ItemStack stored = input.getItem(targetSlot);
				if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, source)) {
					int moved = Math.min(source.getCount(), stored.getMaxStackSize() - stored.getCount());
					stored.grow(moved);
					source.shrink(moved);
				}
			}
			for (int targetSlot = 0; targetSlot < input.getContainerSize() && !source.isEmpty(); targetSlot++) {
				if (input.getItem(targetSlot).isEmpty()) {
					int moved = Math.min(source.getCount(), source.getMaxStackSize());
					input.setItem(targetSlot, source.copyWithCount(moved));
					source.shrink(moved);
				}
			}
			int moved = before - source.getCount();
			if (moved > 0 && measurement.elapsedTicks > WARMUP_TICKS) {
				measurement.importedSinceBaseline.add(template, moved);
			}
		}
		input.setChanged();
		workbench.setChanged();
	}

	private static void recordProductionTimeline(ServerLevel farmLevel, FarmMeasurement measurement) {
		MeasuredItems current = scanCellInventories(farmLevel, measurement.cell);
		current.addAll(measurement.exportedSinceBaseline);
		List<FarmCoreData.StackAmount> produced = increased(measurement.lastTimelineSnapshot, current);
		if (!produced.isEmpty()) {
			measurement.productionTimeline.add(new FarmCoreData.ProductionEvent(measurement.elapsedTicks - WARMUP_TICKS, produced));
		}
		measurement.lastTimelineSnapshot = current;
	}

	private static List<FarmCoreData.ProductionEvent> normalizeTimeline(List<FarmCoreData.ProductionEvent> measured, List<FarmCoreData.StackAmount> finalOutputs) {
		MeasuredItems remaining = new MeasuredItems();
		for (FarmCoreData.StackAmount output : finalOutputs) {
			remaining.add(output.stack(), output.amount());
		}
		List<FarmCoreData.ProductionEvent> result = new ArrayList<>();
		for (FarmCoreData.ProductionEvent event : measured) {
			List<FarmCoreData.StackAmount> accepted = new ArrayList<>();
			for (FarmCoreData.StackAmount candidate : event.outputs()) {
				long available = remaining.amountOf(candidate.stack());
				long amount = Math.min(candidate.amount(), available);
				if (amount > 0) {
					accepted.add(new FarmCoreData.StackAmount(candidate.stack(), amount));
					remaining.remove(candidate.stack(), amount);
				}
			}
			if (!accepted.isEmpty()) {
				result.add(new FarmCoreData.ProductionEvent(Math.min(SAMPLE_TICKS, event.tick()), accepted));
			}
		}
		List<FarmCoreData.StackAmount> unrecorded = remaining.entries();
		if (!unrecorded.isEmpty()) {
			result.add(new FarmCoreData.ProductionEvent(SAMPLE_TICKS, unrecorded));
		}
		return result;
	}

	private static int insertIntoWorkbench(DimensionalWorkbenchBlockEntity workbench, ItemStack stack) {
		int original = stack.getCount();
		for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END && !stack.isEmpty(); slot++) {
			ItemStack stored = workbench.getItem(slot);
			if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, stack)) {
				int moved = Math.min(stack.getCount(), stored.getMaxStackSize() - stored.getCount());
				stored.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int slot = FarmSimulationMachine.OUTPUT_START; slot < FarmSimulationMachine.OUTPUT_END && !stack.isEmpty(); slot++) {
			if (workbench.getItem(slot).isEmpty()) {
				int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
				workbench.setItem(slot, stack.copyWithCount(moved));
				stack.shrink(moved);
			}
		}
		return original - stack.getCount();
	}

	private static DimensionalWorkbenchBlockEntity getWorkbenchBlockEntity(MinecraftServer server, FarmMeasurement measurement) {
		ResourceLocation dimensionId = ResourceLocation.tryParse(measurement.workbenchDimension);
		ServerLevel level = dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		return level != null && level.getBlockEntity(measurement.workbenchPos) instanceof DimensionalWorkbenchBlockEntity workbench ? workbench : null;
	}

	private static ServerLevel getFarmLevel(MinecraftServer server, FarmMeasurement measurement) {
		ResourceLocation dimensionId = ResourceLocation.tryParse(measurement.farmDimension);
		return dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
	}

	private static void setWorkbenchCalculationState(MinecraftServer server, FarmMeasurement measurement, int phase, int remainingTicks) {
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench != null) {
			workbench.setCalculationState(phase, Math.max(0, remainingTicks));
		}
	}

	private static int findDimensionCoreSlot(IItemHandler handler) {
		if (handler == null) {
			return -1;
		}
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			if (handler.getStackInSlot(slot).is(BlackboxModItems.DIMENSION_CORE.get())) {
				return slot;
			}
		}
		return -1;
	}

	private static IItemHandler getItemHandler(ServerLevel level, BlockPos pos) {
		return level instanceof ILevelExtension extension ? extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) : null;
	}

	private static void setCellTicket(ServerLevel level, FarmCell cell, boolean loaded) {
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
				if (loaded) {
					level.getChunkSource().addRegionTicket(MEASUREMENT_TICKET, chunkPos, 0, cell.coreId(), true);
				} else {
					level.getChunkSource().removeRegionTicket(MEASUREMENT_TICKET, chunkPos, 0, cell.coreId(), true);
				}
			}
		}
	}

	private static final class FarmMeasurement {
		private final FarmCell cell;
		private final UUID owner;
		private final BlockPos workbenchPos;
		private final String workbenchDimension;
		private final String farmDimension;
		private int elapsedTicks;
		private MeasuredItems baselineInventory = new MeasuredItems();
		private MeasuredItems importedSinceBaseline = new MeasuredItems();
		private MeasuredItems exportedSinceBaseline = new MeasuredItems();
		private MeasuredItems lastTimelineSnapshot = new MeasuredItems();
		private final List<FarmCoreData.ProductionEvent> productionTimeline = new ArrayList<>();

		private FarmMeasurement(FarmCell cell, UUID owner, BlockPos workbenchPos, String workbenchDimension, String farmDimension) {
			this.cell = cell;
			this.owner = owner;
			this.workbenchPos = workbenchPos;
			this.workbenchDimension = workbenchDimension;
			this.farmDimension = farmDimension;
		}
	}

	private static final class MeasuredItems {
		private final List<FarmCoreData.StackAmount> entries = new ArrayList<>();

		private void add(ItemStack stack) {
			if (stack.isEmpty()) {
				return;
			}
			add(stack, stack.getCount());
		}

		private void add(ItemStack stack, long amount) {
			if (stack.isEmpty() || amount <= 0) {
				return;
			}
			for (int index = 0; index < entries.size(); index++) {
				FarmCoreData.StackAmount current = entries.get(index);
				if (ItemStack.isSameItemSameComponents(current.stack(), stack)) {
					entries.set(index, new FarmCoreData.StackAmount(current.stack(), current.amount() + amount));
					return;
				}
			}
			entries.add(new FarmCoreData.StackAmount(stack, amount));
		}

		private void addAll(MeasuredItems other) {
			for (FarmCoreData.StackAmount entry : other.entries) {
				add(entry.stack(), entry.amount());
			}
		}

		private long amountOf(ItemStack stack) {
			for (FarmCoreData.StackAmount entry : entries) {
				if (ItemStack.isSameItemSameComponents(entry.stack(), stack)) {
					return entry.amount();
				}
			}
			return 0;
		}

		private void remove(ItemStack stack, long amount) {
			for (int index = 0; index < entries.size(); index++) {
				FarmCoreData.StackAmount entry = entries.get(index);
				if (ItemStack.isSameItemSameComponents(entry.stack(), stack)) {
					long remaining = Math.max(0, entry.amount() - amount);
					if (remaining == 0) {
						entries.remove(index);
					} else {
						entries.set(index, new FarmCoreData.StackAmount(entry.stack(), remaining));
					}
					return;
				}
			}
		}

		private List<FarmCoreData.StackAmount> entries() {
			return List.copyOf(entries);
		}
	}
}
