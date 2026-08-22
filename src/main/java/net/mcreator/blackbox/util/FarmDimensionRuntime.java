package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.block.entity.InputblockBlockEntity;
import net.mcreator.blackbox.block.entity.OutputBlockBlockEntity;
import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.init.BlackboxModItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber
public final class FarmDimensionRuntime {
	public static final ResourceKey<Level> FARM_DIMENSION = FarmEnvironment.STANDARD.dimension();

	private static final String DATA_CORE_ID = "BlackboxFarmCoreId";
	private static final String DATA_CELL_SIZE = "BlackboxFarmCellSize";
	private static final String DATA_WORKBENCH_X = "BlackboxWorkbenchX";
	private static final String DATA_WORKBENCH_Y = "BlackboxWorkbenchY";
	private static final String DATA_WORKBENCH_Z = "BlackboxWorkbenchZ";
	private static final String DATA_WORKBENCH_DIMENSION = "BlackboxWorkbenchDimension";
	private static final String DATA_RETURN_X = "BlackboxReturnX";
	private static final String DATA_RETURN_Y = "BlackboxReturnY";
	private static final String DATA_RETURN_Z = "BlackboxReturnZ";
	private static final String DATA_FARM_DIMENSION = "BlackboxFarmDimension";
	private static final Map<UUID, FarmMeasurement> ACTIVE_MEASUREMENTS = new HashMap<>();
	private static final Set<UUID> EDITING_CORES = new HashSet<>();
	private static final TicketController MEASUREMENT_TICKETS = new TicketController(
			ResourceLocation.fromNamespaceAndPath("blackbox", "farm_measurement"));
	private static MinecraftServer loadedServer;

	private FarmDimensionRuntime() {
	}

	public static void registerTicketController(RegisterTicketControllersEvent event) {
		event.register(MEASUREMENT_TICKETS);
	}

	public static FarmCell enterFarmDimension(ServerPlayer player, ServerLevel farmLevel, BlockPos workbenchPos, ResourceKey<Level> workbenchDimension) {
		ensureMeasurementsLoaded(player.server);
		farmLevel.getWorldBorder().setCenter(0.0D, 0.0D);
		farmLevel.getWorldBorder().setSize(59_999_968.0D);
		IItemHandler workbench = getItemHandler((ServerLevel) player.level(), workbenchPos);
		int coreSlot = findDimensionCoreSlot(workbench);
		if (coreSlot < 0 || !(workbench instanceof IItemHandlerModifiable modifiable)) {
			player.sendSystemMessage(Component.translatable("message.blackbox.measurement.no_core").withStyle(ChatFormatting.RED));
			return null;
		}

		ItemStack core = workbench.getStackInSlot(coreSlot).copyWithCount(1);
		if (!FarmCoreData.canAccess(core, player)) {
			player.sendSystemMessage(Component.translatable("message.blackbox.farm.access_denied").withStyle(ChatFormatting.RED));
			return null;
		}
		FarmCoreData.ensureOwner(core, player);
		UUID coreId = FarmCoreData.ensureCoreId(core);
		int cellSize = FarmCoreData.getCellSizeChunks(core);
		if (FarmCoreData.isExampleIronFarm(core) && cellSize < 2) {
			cellSize = 2;
			FarmCoreData.setCellSizeChunks(core, cellSize);
		}
		modifiable.setStackInSlot(coreSlot, core);
		FarmMeasurement runningMeasurement = ACTIVE_MEASUREMENTS.remove(coreId);
		if (runningMeasurement != null) {
			cancelMeasurement(player.server, runningMeasurement, true);
			player.sendSystemMessage(Component.translatable("message.blackbox.measurement.cancelled_for_editing").withStyle(ChatFormatting.YELLOW));
		} else if (FarmCoreData.isProgrammed(core)) {
			FarmCoreData.clearProfile(core, farmLevel.registryAccess());
			modifiable.setStackInSlot(coreSlot, core);
		}
		EDITING_CORES.add(coreId);

		FarmCell cell = FarmCell.fromCoreId(coreId, cellSize);
		CompoundTag data = player.getPersistentData();
		data.putString(DATA_CORE_ID, coreId.toString());
		data.putInt(DATA_CELL_SIZE, cellSize);
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
		upsertFarmRecord(player.server, core, farmLevel.dimension(), player.server.overworld().getGameTime());
		player.sendSystemMessage(Component.translatable("message.blackbox.farm.entered", cellSize, cellSize).withStyle(ChatFormatting.AQUA));
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
		CompoundTag data = player.getPersistentData();
		String value = data.getString(DATA_CORE_ID);
		try {
			int size = data.contains(DATA_CELL_SIZE, Tag.TAG_INT) ? data.getInt(DATA_CELL_SIZE) : FarmCell.MAX_SIZE_CHUNKS;
			return value.isBlank() ? Optional.empty() : Optional.of(FarmCell.fromCoreId(UUID.fromString(value), size));
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

	public static boolean isSimulationPaused(MinecraftServer server, ItemStack core) {
		ensureMeasurementsLoaded(server);
		return FarmCoreData.getCoreId(core).map(coreId -> ACTIVE_MEASUREMENTS.containsKey(coreId) || EDITING_CORES.contains(coreId)).orElse(false);
	}

	public static List<FarmWorldData.FarmRecord> farmRecords(MinecraftServer server) {
		ensureMeasurementsLoaded(server);
		return FarmWorldData.get(server).farms();
	}

	public static Optional<FarmCell> registeredCellAt(ServerLevel level, BlockPos pos) {
		for (FarmWorldData.FarmRecord record : FarmWorldData.get(level.getServer()).farms()) {
			if (!record.dimension().equals(level.dimension().location().toString())) {
				continue;
			}
			FarmCell cell = FarmCell.fromCoreId(record.coreId(), record.sizeChunks());
			if (cell.contains(pos)) {
				return Optional.of(cell);
			}
		}
		return Optional.empty();
	}

	public static void refreshFarmRecord(MinecraftServer server, ItemStack core) {
		Optional<UUID> coreId = FarmCoreData.getCoreId(core);
		if (coreId.isEmpty()) {
			return;
		}
		FarmWorldData.FarmRecord existing = FarmWorldData.get(server).find(coreId.get());
		ResourceKey<Level> dimension = existing == null
				? FarmCoreData.getEnvironment(core).dimension()
				: ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(existing.dimension()));
		upsertFarmRecord(server, core, dimension, server.overworld().getGameTime());
	}

	public static boolean deleteFarmCell(MinecraftServer server, UUID coreId) {
		ensureMeasurementsLoaded(server);
		FarmMeasurement running = ACTIVE_MEASUREMENTS.remove(coreId);
		EDITING_CORES.remove(coreId);
		if (running != null) {
			cancelMeasurement(server, running, false);
		}
		FarmWorldData data = FarmWorldData.get(server);
		FarmWorldData.FarmRecord record = data.find(coreId);
		if (record == null) {
			return false;
		}
		ResourceLocation dimensionId = ResourceLocation.tryParse(record.dimension());
		ServerLevel level = dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		FarmCell cell = FarmCell.fromCoreId(coreId, record.sizeChunks());
		if (level != null) {
			for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
				if (player.level() == level && cell.contains(player.blockPosition())) {
					ServerLevel destination = server.overworld();
					BlockPos spawn = destination.getSharedSpawnPos();
					player.teleportTo(destination, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
					clearAssignment(player);
				}
			}
			clearCellBlocks(level, cell);
			FarmMobSpawnRules.clearCell(level, cell);
		}
		boolean removed = data.remove(coreId);
		persistMeasurements(server);
		return removed;
	}

	public static void prepareCell(ServerLevel level, FarmCell cell) {
		FarmCell maximum = cell.maximumArea();
		for (int chunkX = maximum.minChunkX(); chunkX <= maximum.maxChunkX(); chunkX++) {
			for (int chunkZ = maximum.minChunkZ(); chunkZ <= maximum.maxChunkZ(); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
		for (int x = maximum.minBlockX(); x <= maximum.maxBlockX(); x++) {
			for (int z = maximum.minBlockZ(); z <= maximum.maxBlockZ(); z++) {
				BlockPos floor = new BlockPos(x, 0, z);
				if (cell.contains(floor)) {
					if (!level.getBlockState(floor).is(BlackboxModBlocks.EMERALD_BEDROCK.get())) {
						level.setBlock(floor, BlackboxModBlocks.EMERALD_BEDROCK.get().defaultBlockState(), 2);
					}
				} else if (!level.getBlockState(floor).isAir()) {
					level.setBlock(floor, Blocks.AIR.defaultBlockState(), 2);
				}
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
		ensureMeasurementsLoaded(player.server);
		Optional<FarmCell> assignedCell = getAssignedCell(player);
		if (assignedCell.isEmpty() || ACTIVE_MEASUREMENTS.containsKey(assignedCell.get().coreId())) {
			return;
		}
		CompoundTag data = player.getPersistentData();
		BlockPos workbenchPos = new BlockPos(data.getInt(DATA_WORKBENCH_X), data.getInt(DATA_WORKBENCH_Y), data.getInt(DATA_WORKBENCH_Z));
		FarmMeasurement measurement = new FarmMeasurement(assignedCell.get(), player.getUUID(), workbenchPos,
				data.getString(DATA_WORKBENCH_DIMENSION), data.getString(DATA_FARM_DIMENSION), BlackboxConfig.warmupTicks(), BlackboxConfig.measurementTicks());
		ServerLevel farmLevel = getFarmLevel(player.server, measurement);
		if (farmLevel == null || !hasMatchingCore(player.server, measurement)) {
			return;
		}
		setMeasurementTickets(player.server, measurement, true);
		ACTIVE_MEASUREMENTS.put(measurement.cell.coreId(), measurement);
		EDITING_CORES.remove(measurement.cell.coreId());
		setWorkbenchCalculationState(player.server, measurement, 1, 0);
		persistMeasurements(player.server);
		player.sendSystemMessage(Component.translatable("message.blackbox.measurement.started_after_exit").withStyle(ChatFormatting.YELLOW));
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		ensureMeasurementsLoaded(server);
		if (ACTIVE_MEASUREMENTS.isEmpty()) {
			return;
		}
		List<UUID> completed = new ArrayList<>();
		for (FarmMeasurement measurement : List.copyOf(ACTIVE_MEASUREMENTS.values())) {
			ServerLevel farmLevel = getFarmLevel(server, measurement);
			if (farmLevel == null || !hasMatchingCore(server, measurement)) {
				abortMeasurement(server, measurement);
				completed.add(measurement.cell.coreId());
				continue;
			}
			measurement.elapsedTicks++;
			transferWorkbenchInput(server, farmLevel, measurement);
			transferFarmOutput(server, farmLevel, measurement);
			if (measurement.cell.coreId().equals(FarmCoreData.EXAMPLE_IRON_FARM_ID) && measurement.elapsedTicks >= measurement.warmupTicks
					&& measurement.elapsedTicks % 200 == 0) {
				ExampleIronFarmBuilder.spawnTestGolem(farmLevel, measurement.cell);
			}
			if (measurement.elapsedTicks == measurement.warmupTicks) {
				measurement.baseline = scanCellResources(farmLevel, measurement.cell);
				measurement.lastTimelineSnapshot = measurement.baseline.items.copy();
				measurement.importedSinceBaseline = new ResourceSnapshot();
				measurement.exportedSinceBaseline = new ResourceSnapshot();
				measurement.mobSamples.clear();
				measurement.mobSamples.add(measurement.baseline.entities.copy());
				setWorkbenchCalculationState(server, measurement, 2, measurement.sampleTicks);
				notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.scanning_ports").withStyle(ChatFormatting.AQUA));
			}
			if (measurement.elapsedTicks > measurement.warmupTicks && measurement.elapsedTicks % 20 == 0) {
				recordProductionTimeline(farmLevel, measurement);
				recordMobSample(farmLevel, measurement);
			}
			if (measurement.elapsedTicks >= measurement.totalTicks()) {
				finishMeasurement(server, farmLevel, measurement);
				completed.add(measurement.cell.coreId());
			} else if (measurement.elapsedTicks > measurement.warmupTicks && measurement.elapsedTicks % 20 == 0) {
				setWorkbenchCalculationState(server, measurement, 2, measurement.totalTicks() - measurement.elapsedTicks);
			}
			if (measurement.elapsedTicks % 100 == 0 && measurement.elapsedTicks < measurement.totalTicks()) {
				showProgress(server, measurement);
			}
		}
		completed.forEach(ACTIVE_MEASUREMENTS::remove);
		if (!completed.isEmpty() || server.overworld().getGameTime() % 20 == 0) {
			persistMeasurements(server);
		}
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

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		if (loadedServer == event.getServer()) {
			ACTIVE_MEASUREMENTS.clear();
			EDITING_CORES.clear();
			loadedServer = null;
		}
	}

	private static void finishMeasurement(MinecraftServer server, ServerLevel farmLevel, FarmMeasurement measurement) {
		ResourceSnapshot available = measurement.baseline.copy();
		available.addAll(measurement.importedSinceBaseline);
		ResourceSnapshot finished = scanCellResources(farmLevel, measurement.cell);
		finished.addAll(measurement.exportedSinceBaseline);
		List<FarmCoreData.StackAmount> inputs = decreased(available.items, finished.items);
		List<FarmCoreData.StackAmount> outputs = increased(available.items, finished.items);
		List<FarmCoreData.FluidAmount> fluidInputs = decreased(available.fluids, finished.fluids);
		List<FarmCoreData.FluidAmount> fluidOutputs = increased(available.fluids, finished.fluids);
		long energyInput = Math.max(0, available.energy - finished.energy);
		long energyOutput = Math.max(0, finished.energy - available.energy);
		List<FarmCoreData.EntityAmount> entityInputs = stableEntityInputs(measurement.baseline.entities, measurement.mobSamples);
		List<FarmCoreData.ProductionEvent> timeline = normalizeTimeline(measurement.productionTimeline, outputs, measurement.sampleTicks);
		boolean hasOutput = !outputs.isEmpty() || !fluidOutputs.isEmpty() || energyOutput > 0;
		boolean saved = hasOutput && writeSampleToWorkbench(server, measurement, inputs, outputs, timeline, entityInputs, fluidInputs, fluidOutputs, energyInput, energyOutput);
		if (!hasOutput) {
			notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.no_output_port").withStyle(ChatFormatting.RED));
		} else if (!saved) {
			notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.no_matching_core").withStyle(ChatFormatting.RED));
		} else {
			notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.saved_resources", inputs.size(), outputs.size(), entityInputs.size(), fluidInputs.size(), fluidOutputs.size(), energyInput, energyOutput)
					.withStyle(ChatFormatting.GREEN));
		}
		FarmMobSpawnRules.clearCell(farmLevel, measurement.cell);
		setMeasurementTickets(server, measurement, false);
		setWorkbenchCalculationState(server, measurement, 0, 0);
	}

	private static void abortMeasurement(MinecraftServer server, FarmMeasurement measurement) {
		ServerLevel farmLevel = getFarmLevel(server, measurement);
		if (farmLevel != null) {
			FarmMobSpawnRules.clearCell(farmLevel, measurement.cell);
		}
		setMeasurementTickets(server, measurement, false);
		setWorkbenchCalculationState(server, measurement, 0, 0);
		notifyOwner(server, measurement, Component.translatable("message.blackbox.measurement.aborted_missing_machine").withStyle(ChatFormatting.RED));
	}

	private static void showProgress(MinecraftServer server, FarmMeasurement measurement) {
		Component message;
		if (measurement.elapsedTicks < measurement.warmupTicks) {
			message = Component.translatable("message.blackbox.measurement.warmup_background").withStyle(ChatFormatting.YELLOW);
		} else {
			int seconds = (measurement.totalTicks() - measurement.elapsedTicks + 19) / 20;
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

	private static ResourceSnapshot scanCellResources(ServerLevel level, FarmCell cell) {
		ResourceSnapshot result = new ResourceSnapshot();
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
					BlockPos pos = blockEntity.getBlockPos();
					if (!cell.contains(pos)) {
						continue;
					}
					List<IItemHandler> itemHandlers = getItemHandlersForScan(level, pos);
					if (!itemHandlers.isEmpty()) {
						for (IItemHandler itemHandler : itemHandlers) {
						for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
							result.items.add(itemHandler.getStackInSlot(slot));
						}
						}
					} else if (blockEntity instanceof Container container) {
						for (int slot = 0; slot < container.getContainerSize(); slot++) {
							result.items.add(container.getItem(slot));
						}
					}
					for (IFluidHandler fluidHandler : getFluidHandlersForScan(level, pos)) {
						for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
							result.fluids.add(fluidHandler.getFluidInTank(tank));
						}
					}
					for (IEnergyStorage energyStorage : getEnergyStoragesForScan(level, pos)) {
						result.energy += energyStorage.getEnergyStored();
					}
				}
			}
		}
		boolean activeSpawnerExists = false;
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX() && !activeSpawnerExists; chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ() && !activeSpawnerExists; chunkZ++) {
				activeSpawnerExists = level.getChunk(chunkX, chunkZ).getBlockEntities().values().stream()
						.anyMatch(blockEntity -> cell.contains(blockEntity.getBlockPos())
								&& (blockEntity.getBlockState().is(Blocks.SPAWNER) || blockEntity.getBlockState().is(Blocks.TRIAL_SPAWNER)));
			}
		}
		AABB bounds = new AABB(cell.minBlockX(), level.getMinBuildHeight(), cell.minBlockZ(),
				cell.maxBlockX() + 1.0D, level.getMaxBuildHeight(), cell.maxBlockZ() + 1.0D);
		boolean spawnerExists = activeSpawnerExists;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, bounds, entity -> FarmMobAccounting.shouldCount(entity, spawnerExists))) {
			result.entities.add(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()), 1);
		}
		return result;
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

	private static List<FarmCoreData.FluidAmount> decreased(MeasuredFluids before, MeasuredFluids after) {
		List<FarmCoreData.FluidAmount> result = new ArrayList<>();
		for (FarmCoreData.FluidAmount entry : before.entries()) {
			long amount = entry.amount() - after.amountOf(entry.stack());
			if (amount > 0) {
				result.add(new FarmCoreData.FluidAmount(entry.stack(), amount));
			}
		}
		return result;
	}

	private static List<FarmCoreData.FluidAmount> increased(MeasuredFluids before, MeasuredFluids after) {
		List<FarmCoreData.FluidAmount> result = new ArrayList<>();
		for (FarmCoreData.FluidAmount entry : after.entries()) {
			long amount = entry.amount() - before.amountOf(entry.stack());
			if (amount > 0) {
				result.add(new FarmCoreData.FluidAmount(entry.stack(), amount));
			}
		}
		return result;
	}

	private static boolean writeSampleToWorkbench(MinecraftServer server, FarmMeasurement measurement, List<FarmCoreData.StackAmount> inputs,
			List<FarmCoreData.StackAmount> outputs, List<FarmCoreData.ProductionEvent> timeline, List<FarmCoreData.EntityAmount> entityInputs, List<FarmCoreData.FluidAmount> fluidInputs,
			List<FarmCoreData.FluidAmount> fluidOutputs, long energyInput, long energyOutput) {
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench == null) {
			return false;
		}
		for (int slot = 0; slot < workbench.getContainerSize(); slot++) {
			ItemStack stored = workbench.getItem(slot);
			if (!stored.is(BlackboxModItems.DIMENSION_CORE.get()) || FarmCoreData.getCoreId(stored).filter(measurement.cell.coreId()::equals).isEmpty()) {
				continue;
			}
			ItemStack core = stored.copyWithCount(1);
			FarmCoreData.write(core, workbench.getLevel().registryAccess(), inputs, outputs, timeline, entityInputs, fluidInputs, fluidOutputs, energyInput, energyOutput,
					measurement.warmupTicks, measurement.sampleTicks);
			workbench.setItem(slot, core);
			workbench.setActiveCoreId(measurement.cell.coreId().toString());
			workbench.setSimulationTicks(0);
			workbench.setStableCycleFunded(false);
			refreshFarmRecord(server, core);
			return true;
		}
		return false;
	}

	private static void cancelMeasurement(MinecraftServer server, FarmMeasurement measurement, boolean clearProfile) {
		ServerLevel farmLevel = getFarmLevel(server, measurement);
		if (farmLevel != null) {
			FarmMobSpawnRules.clearCell(farmLevel, measurement.cell);
		}
		setMeasurementTickets(server, measurement, false);
		setWorkbenchCalculationState(server, measurement, 0, 0);
		if (!clearProfile) {
			persistMeasurements(server);
			return;
		}
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench == null) {
			return;
		}
		for (int slot = 0; slot < workbench.getContainerSize(); slot++) {
			ItemStack stored = workbench.getItem(slot);
			if (stored.is(BlackboxModItems.DIMENSION_CORE.get()) && FarmCoreData.getCoreId(stored).filter(measurement.cell.coreId()::equals).isPresent()) {
				ItemStack core = stored.copyWithCount(1);
				FarmCoreData.clearProfile(core, workbench.getLevel().registryAccess());
				workbench.setItem(slot, core);
				break;
			}
		}
		persistMeasurements(server);
	}

	private static void transferFarmOutput(MinecraftServer server, ServerLevel farmLevel, FarmMeasurement measurement) {
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench == null) {
			return;
		}
		for (OutputBlockBlockEntity output : outputPorts(farmLevel, measurement.cell)) {
			for (int slot = 0; slot < output.getContainerSize(); slot++) {
				ItemStack stored = output.getItem(slot);
				if (stored.isEmpty()) {
					continue;
				}
				ItemStack template = stored.copyWithCount(1);
				int total = stored.getCount();
				int inserted = insertIntoWorkbench(workbench, stored.copy());
				int buffered = total - inserted;
				if (buffered > 0) {
					workbench.measurementOutputBuffer().addItem(template, buffered);
				}
				output.setItem(slot, ItemStack.EMPTY);
				if (measurement.isSampling()) {
					measurement.exportedSinceBaseline.items.add(template, total);
				}
			}
			List<FluidStack> fluidTypes = new ArrayList<>();
			for (int tank = 0; tank < output.fluidStorage().getTanks(); tank++) {
				FluidStack template = output.fluidStorage().getFluidInTank(tank).copy();
				if (!template.isEmpty() && fluidTypes.stream().noneMatch(existing -> FluidStack.isSameFluidSameComponents(existing, template))) {
					fluidTypes.add(template.copyWithAmount(1));
				}
			}
			for (FluidStack template : fluidTypes) {
				long total = output.fluidStorage().amountOf(template);
				long inserted = moveFluid(output.fluidStorage(), workbench.resources().outputFluids(), template,
						(int) Math.min(Integer.MAX_VALUE, total));
				long buffered = output.fluidStorage().drainLong(template, total - inserted, IFluidHandler.FluidAction.EXECUTE);
				workbench.measurementOutputBuffer().addFluid(template, buffered);
				if (measurement.isSampling()) {
					measurement.exportedSinceBaseline.fluids.add(template, inserted + buffered);
				}
			}
			int insertedEnergy = moveEnergy(output.energyStorage(), workbench.resources().outputEnergy());
			int bufferedEnergy = output.energyStorage().extractEnergy(Integer.MAX_VALUE, false);
			workbench.measurementOutputBuffer().addEnergy(bufferedEnergy);
			if (measurement.isSampling()) {
				measurement.exportedSinceBaseline.energy += (long) insertedEnergy + bufferedEnergy;
			}
			output.setChanged();
		}
		workbench.setChanged();
	}

	private static List<OutputBlockBlockEntity> outputPorts(ServerLevel level, FarmCell cell) {
		List<OutputBlockBlockEntity> outputs = new ArrayList<>();
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
					if (blockEntity instanceof OutputBlockBlockEntity output && cell.contains(output.getBlockPos())) {
						outputs.add(output);
					}
				}
			}
		}
		outputs.sort(java.util.Comparator.comparingLong(output -> output.getBlockPos().asLong()));
		return outputs;
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
			if (moved > 0 && measurement.isSampling()) {
				measurement.importedSinceBaseline.items.add(template, moved);
			}
		}
		for (int tank = 0; tank < workbench.resources().inputFluids().getTanks(); tank++) {
			FluidStack template = workbench.resources().inputFluids().getFluidInTank(tank).copy();
			long moved = moveFluid(workbench.resources().inputFluids(), input.fluidStorage(), template, template.getAmount());
			if (moved > 0 && measurement.isSampling()) {
				measurement.importedSinceBaseline.fluids.add(template, moved);
			}
		}
		int movedEnergy = moveEnergy(workbench.resources().inputEnergy(), input.energyStorage());
		if (movedEnergy > 0 && measurement.isSampling()) {
			measurement.importedSinceBaseline.energy += movedEnergy;
		}
		input.setChanged();
		workbench.setChanged();
	}

	private static long moveFluid(IFluidHandler source, IFluidHandler target, FluidStack template, int requested) {
		if (template.isEmpty() || requested <= 0) {
			return 0;
		}
		int accepted = target.fill(template.copyWithAmount(requested), IFluidHandler.FluidAction.SIMULATE);
		if (accepted <= 0) {
			return 0;
		}
		FluidStack drained = source.drain(template.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
		return target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
	}

	private static int moveEnergy(IEnergyStorage source, IEnergyStorage target) {
		int available = source.extractEnergy(Integer.MAX_VALUE, true);
		int accepted = target.receiveEnergy(available, true);
		if (accepted <= 0) {
			return 0;
		}
		int extracted = source.extractEnergy(accepted, false);
		return target.receiveEnergy(extracted, false);
	}

	private static void recordProductionTimeline(ServerLevel farmLevel, FarmMeasurement measurement) {
		MeasuredItems current = scanCellResources(farmLevel, measurement.cell).items;
		current.addAll(measurement.exportedSinceBaseline.items);
		List<FarmCoreData.StackAmount> produced = increased(measurement.lastTimelineSnapshot, current);
		if (!produced.isEmpty()) {
			measurement.productionTimeline.add(new FarmCoreData.ProductionEvent(measurement.elapsedTicks - measurement.warmupTicks, produced));
		}
		measurement.lastTimelineSnapshot = current;
	}

	private static void recordMobSample(ServerLevel farmLevel, FarmMeasurement measurement) {
		measurement.mobSamples.add(scanCellResources(farmLevel, measurement.cell).entities.copy());
		while (measurement.mobSamples.size() > 10) {
			measurement.mobSamples.remove(0);
		}
	}

	private static List<FarmCoreData.EntityAmount> stableEntityInputs(MeasuredEntities baseline, List<MeasuredEntities> samples) {
		List<FarmCoreData.EntityAmount> result = new ArrayList<>();
		for (FarmCoreData.EntityAmount entry : baseline.entries()) {
			if (samples.isEmpty()) {
				continue;
			}
			List<Long> counts = new ArrayList<>();
			for (MeasuredEntities sample : samples) {
				counts.add(sample.amountOf(entry.entityType()));
			}
			counts.sort(Long::compareTo);
			long stableFinalCount = counts.get(counts.size() / 2);
			long consumed = entry.amount() - stableFinalCount;
			if (consumed > 0) {
				result.add(new FarmCoreData.EntityAmount(entry.entityType(), consumed));
			}
		}
		return result;
	}

	private static List<FarmCoreData.ProductionEvent> normalizeTimeline(List<FarmCoreData.ProductionEvent> measured,
			List<FarmCoreData.StackAmount> finalOutputs, int sampleTicks) {
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
				result.add(new FarmCoreData.ProductionEvent(Math.min(sampleTicks, event.tick()), accepted));
			}
		}
		List<FarmCoreData.StackAmount> unrecorded = remaining.entries();
		if (!unrecorded.isEmpty()) {
			result.add(new FarmCoreData.ProductionEvent(sampleTicks, unrecorded));
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
		if (level == null) {
			return null;
		}
		level.getChunkAt(measurement.workbenchPos);
		return level.getBlockEntity(measurement.workbenchPos) instanceof DimensionalWorkbenchBlockEntity workbench ? workbench : null;
	}

	private static boolean hasMatchingCore(MinecraftServer server, FarmMeasurement measurement) {
		return !matchingCore(server, measurement).isEmpty();
	}

	private static ItemStack matchingCore(MinecraftServer server, FarmMeasurement measurement) {
		DimensionalWorkbenchBlockEntity workbench = getWorkbenchBlockEntity(server, measurement);
		if (workbench == null) {
			return ItemStack.EMPTY;
		}
		for (int slot = 0; slot < workbench.getContainerSize(); slot++) {
			if (FarmCoreData.getCoreId(workbench.getItem(slot)).filter(measurement.cell.coreId()::equals).isPresent()) {
				return workbench.getItem(slot);
			}
		}
		return ItemStack.EMPTY;
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

	private static List<IItemHandler> getItemHandlersForScan(ServerLevel level, BlockPos pos) {
		if (!(level instanceof ILevelExtension extension)) {
			return List.of();
		}
		IItemHandler unsided = extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (unsided != null) {
			return List.of(unsided);
		}
		IdentityHashMap<IItemHandler, Boolean> handlers = new IdentityHashMap<>();
		for (Direction direction : Direction.values()) {
			IItemHandler handler = extension.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
			if (handler != null) {
				handlers.put(handler, Boolean.TRUE);
			}
		}
		return List.copyOf(handlers.keySet());
	}

	private static List<IFluidHandler> getFluidHandlersForScan(ServerLevel level, BlockPos pos) {
		if (!(level instanceof ILevelExtension extension)) {
			return List.of();
		}
		IFluidHandler unsided = extension.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
		if (unsided != null) {
			return List.of(unsided);
		}
		IdentityHashMap<IFluidHandler, Boolean> handlers = new IdentityHashMap<>();
		for (Direction direction : Direction.values()) {
			IFluidHandler handler = extension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (handler != null) {
				handlers.put(handler, Boolean.TRUE);
			}
		}
		return List.copyOf(handlers.keySet());
	}

	private static List<IEnergyStorage> getEnergyStoragesForScan(ServerLevel level, BlockPos pos) {
		if (!(level instanceof ILevelExtension extension)) {
			return List.of();
		}
		IEnergyStorage unsided = extension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (unsided != null) {
			return List.of(unsided);
		}
		IdentityHashMap<IEnergyStorage, Boolean> storages = new IdentityHashMap<>();
		for (Direction direction : Direction.values()) {
			IEnergyStorage storage = extension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (storage != null) {
				storages.put(storage, Boolean.TRUE);
			}
		}
		return List.copyOf(storages.keySet());
	}

	private static void setMeasurementTickets(MinecraftServer server, FarmMeasurement measurement, boolean loaded) {
		ServerLevel farmLevel = getFarmLevel(server, measurement);
		if (farmLevel != null) {
			setCellTicket(farmLevel, measurement.cell, loaded);
		}
		ResourceLocation dimensionId = ResourceLocation.tryParse(measurement.workbenchDimension);
		ServerLevel workbenchLevel = dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (workbenchLevel != null) {
			ChunkPos chunkPos = new ChunkPos(measurement.workbenchPos);
			MEASUREMENT_TICKETS.forceChunk(workbenchLevel, measurement.cell.coreId(), chunkPos.x, chunkPos.z, loaded, true);
		}
	}

	private static void setCellTicket(ServerLevel level, FarmCell cell, boolean loaded) {
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
				MEASUREMENT_TICKETS.forceChunk(level, cell.coreId(), chunkPos.x, chunkPos.z, loaded, true);
			}
		}
	}

	private static void ensureMeasurementsLoaded(MinecraftServer server) {
		if (loadedServer == server) {
			return;
		}
		ACTIVE_MEASUREMENTS.clear();
		EDITING_CORES.clear();
		loadedServer = server;
		for (Tag entry : FarmWorldData.get(server).measurements()) {
			if (!(entry instanceof CompoundTag tag)) {
				continue;
			}
			FarmMeasurement measurement = FarmMeasurement.load(tag, server.registryAccess());
			if (measurement == null || getFarmLevel(server, measurement) == null) {
				continue;
			}
			setMeasurementTickets(server, measurement, true);
			if (hasMatchingCore(server, measurement)) {
				ACTIVE_MEASUREMENTS.put(measurement.cell.coreId(), measurement);
				FarmMobSpawnRules.setCellEnabled(getFarmLevel(server, measurement), measurement.cell,
						FarmCoreData.isMobSpawningEnabled(matchingCore(server, measurement)));
				setWorkbenchCalculationState(server, measurement, measurement.isSampling() ? 2 : 1,
						measurement.isSampling() ? measurement.totalTicks() - measurement.elapsedTicks : 0);
			} else {
				setMeasurementTickets(server, measurement, false);
			}
		}
		persistMeasurements(server);
	}

	private static void persistMeasurements(MinecraftServer server) {
		ListTag list = new ListTag();
		for (FarmMeasurement measurement : ACTIVE_MEASUREMENTS.values()) {
			list.add(measurement.save(server.registryAccess()));
		}
		FarmWorldData.get(server).setMeasurements(list);
	}

	private static void upsertFarmRecord(MinecraftServer server, ItemStack core, ResourceKey<Level> dimension, long lastUsed) {
		Optional<UUID> coreId = FarmCoreData.getCoreId(core);
		if (coreId.isEmpty()) {
			return;
		}
		String name = FarmCoreData.getFarmName(core);
		FarmWorldData.get(server).upsert(new FarmWorldData.FarmRecord(coreId.get(), FarmCoreData.getCellSizeChunks(core), dimension.location().toString(),
				name, FarmCoreData.getOwner(core).orElse(null), FarmCoreData.getOwnerName(core), lastUsed));
	}

	private static void clearCellBlocks(ServerLevel level, FarmCell cell) {
		for (int chunkX = cell.minChunkX(); chunkX <= cell.maxChunkX(); chunkX++) {
			for (int chunkZ = cell.minChunkZ(); chunkZ <= cell.maxChunkZ(); chunkZ++) {
				level.getChunk(chunkX, chunkZ);
			}
		}
		for (int x = cell.minBlockX(); x <= cell.maxBlockX(); x++) {
			for (int z = cell.minBlockZ(); z <= cell.maxBlockZ(); z++) {
				for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!level.getBlockState(pos).isAir()) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	private static void clearAssignment(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.remove(DATA_CORE_ID);
		data.remove(DATA_CELL_SIZE);
		data.remove(DATA_FARM_DIMENSION);
	}

	private static final class FarmMeasurement {
		private final FarmCell cell;
		private final UUID owner;
		private final BlockPos workbenchPos;
		private final String workbenchDimension;
		private final String farmDimension;
		private final int warmupTicks;
		private final int sampleTicks;
		private int elapsedTicks;
		private ResourceSnapshot baseline = new ResourceSnapshot();
		private ResourceSnapshot importedSinceBaseline = new ResourceSnapshot();
		private ResourceSnapshot exportedSinceBaseline = new ResourceSnapshot();
		private MeasuredItems lastTimelineSnapshot = new MeasuredItems();
		private final List<FarmCoreData.ProductionEvent> productionTimeline = new ArrayList<>();
		private final List<MeasuredEntities> mobSamples = new ArrayList<>();

		private FarmMeasurement(FarmCell cell, UUID owner, BlockPos workbenchPos, String workbenchDimension, String farmDimension, int warmupTicks, int sampleTicks) {
			this.cell = cell;
			this.owner = owner;
			this.workbenchPos = workbenchPos;
			this.workbenchDimension = workbenchDimension;
			this.farmDimension = farmDimension;
			this.warmupTicks = Math.max(0, warmupTicks);
			this.sampleTicks = Math.max(1, sampleTicks);
		}

		private int totalTicks() {
			return this.warmupTicks + this.sampleTicks;
		}

		private boolean isSampling() {
			return this.elapsedTicks > this.warmupTicks;
		}

		private CompoundTag save(HolderLookup.Provider lookupProvider) {
			CompoundTag tag = new CompoundTag();
			tag.putString("CoreId", this.cell.coreId().toString());
			tag.putInt("CellSize", this.cell.sizeChunks());
			tag.putString("Owner", this.owner.toString());
			tag.putLong("WorkbenchPos", this.workbenchPos.asLong());
			tag.putString("WorkbenchDimension", this.workbenchDimension);
			tag.putString("FarmDimension", this.farmDimension);
			tag.putInt("WarmupTicks", this.warmupTicks);
			tag.putInt("SampleTicks", this.sampleTicks);
			tag.putInt("ElapsedTicks", this.elapsedTicks);
			tag.put("Baseline", this.baseline.save(lookupProvider));
			tag.put("Imported", this.importedSinceBaseline.save(lookupProvider));
			tag.put("Exported", this.exportedSinceBaseline.save(lookupProvider));
			tag.put("LastTimeline", this.lastTimelineSnapshot.save(lookupProvider));
			ListTag timeline = new ListTag();
			for (FarmCoreData.ProductionEvent event : this.productionTimeline) {
				CompoundTag eventTag = new CompoundTag();
				eventTag.putInt("Tick", event.tick());
				MeasuredItems items = new MeasuredItems();
				for (FarmCoreData.StackAmount output : event.outputs()) {
					items.add(output.stack(), output.amount());
				}
				eventTag.put("Items", items.save(lookupProvider));
				timeline.add(eventTag);
			}
			tag.put("Timeline", timeline);
			ListTag mobs = new ListTag();
			for (MeasuredEntities sample : this.mobSamples) {
				CompoundTag sampleTag = new CompoundTag();
				sampleTag.put("Entities", sample.save());
				mobs.add(sampleTag);
			}
			tag.put("MobSamples", mobs);
			return tag;
		}

		private static FarmMeasurement load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
			try {
				UUID coreId = UUID.fromString(tag.getString("CoreId"));
				FarmMeasurement measurement = new FarmMeasurement(FarmCell.fromCoreId(coreId, tag.getInt("CellSize")),
						UUID.fromString(tag.getString("Owner")), BlockPos.of(tag.getLong("WorkbenchPos")), tag.getString("WorkbenchDimension"),
						tag.getString("FarmDimension"), tag.getInt("WarmupTicks"), tag.getInt("SampleTicks"));
				measurement.elapsedTicks = Math.max(0, Math.min(measurement.totalTicks(), tag.getInt("ElapsedTicks")));
				measurement.baseline = ResourceSnapshot.load(tag.getCompound("Baseline"), lookupProvider);
				measurement.importedSinceBaseline = ResourceSnapshot.load(tag.getCompound("Imported"), lookupProvider);
				measurement.exportedSinceBaseline = ResourceSnapshot.load(tag.getCompound("Exported"), lookupProvider);
				measurement.lastTimelineSnapshot = MeasuredItems.load(tag.getList("LastTimeline", Tag.TAG_COMPOUND), lookupProvider);
				ListTag timeline = tag.getList("Timeline", Tag.TAG_COMPOUND);
				for (int index = 0; index < timeline.size(); index++) {
					CompoundTag eventTag = timeline.getCompound(index);
					measurement.productionTimeline.add(new FarmCoreData.ProductionEvent(eventTag.getInt("Tick"),
							MeasuredItems.load(eventTag.getList("Items", Tag.TAG_COMPOUND), lookupProvider).entries()));
				}
				ListTag mobs = tag.getList("MobSamples", Tag.TAG_COMPOUND);
				for (int index = Math.max(0, mobs.size() - 10); index < mobs.size(); index++) {
					measurement.mobSamples.add(MeasuredEntities.load(mobs.getCompound(index).getList("Entities", Tag.TAG_COMPOUND)));
				}
				return measurement;
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}
	}

	private static final class ResourceSnapshot {
		private final MeasuredItems items = new MeasuredItems();
		private final MeasuredFluids fluids = new MeasuredFluids();
		private final MeasuredEntities entities = new MeasuredEntities();
		private long energy;

		private void addAll(ResourceSnapshot other) {
			this.items.addAll(other.items);
			this.fluids.addAll(other.fluids);
			this.entities.addAll(other.entities);
			this.energy += other.energy;
		}

		private ResourceSnapshot copy() {
			ResourceSnapshot result = new ResourceSnapshot();
			result.addAll(this);
			return result;
		}

		private CompoundTag save(HolderLookup.Provider lookupProvider) {
			CompoundTag tag = new CompoundTag();
			tag.put("Items", this.items.save(lookupProvider));
			tag.put("Fluids", this.fluids.save(lookupProvider));
			tag.put("Entities", this.entities.save());
			tag.putLong("Energy", this.energy);
			return tag;
		}

		private static ResourceSnapshot load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
			ResourceSnapshot result = new ResourceSnapshot();
			result.items.addAll(MeasuredItems.load(tag.getList("Items", Tag.TAG_COMPOUND), lookupProvider));
			result.fluids.addAll(MeasuredFluids.load(tag.getList("Fluids", Tag.TAG_COMPOUND), lookupProvider));
			result.entities.addAll(MeasuredEntities.load(tag.getList("Entities", Tag.TAG_COMPOUND)));
			result.energy = Math.max(0, tag.getLong("Energy"));
			return result;
		}
	}

	private static final class MeasuredEntities {
		private final Map<ResourceLocation, Long> entries = new HashMap<>();

		private void add(ResourceLocation type, long amount) {
			if (type != null && amount > 0) {
				this.entries.merge(type, amount, Long::sum);
			}
		}

		private void addAll(MeasuredEntities other) {
			other.entries.forEach(this::add);
		}

		private MeasuredEntities copy() {
			MeasuredEntities result = new MeasuredEntities();
			result.addAll(this);
			return result;
		}

		private long amountOf(ResourceLocation type) {
			return this.entries.getOrDefault(type, 0L);
		}

		private List<FarmCoreData.EntityAmount> entries() {
			return this.entries.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(entry -> new FarmCoreData.EntityAmount(entry.getKey(), entry.getValue()))
					.toList();
		}

		private ListTag save() {
			ListTag list = new ListTag();
			for (FarmCoreData.EntityAmount entry : entries()) {
				CompoundTag tag = new CompoundTag();
				tag.putString("Type", entry.entityType().toString());
				tag.putLong("Amount", entry.amount());
				list.add(tag);
			}
			return list;
		}

		private static MeasuredEntities load(ListTag list) {
			MeasuredEntities result = new MeasuredEntities();
			for (int index = 0; index < list.size(); index++) {
				CompoundTag tag = list.getCompound(index);
				ResourceLocation type = ResourceLocation.tryParse(tag.getString("Type"));
				result.add(type, tag.getLong("Amount"));
			}
			return result;
		}
	}

	private static final class MeasuredItems {
		private final List<FarmCoreData.StackAmount> entries = new ArrayList<>();

		private void add(ItemStack stack) {
			if (!stack.isEmpty()) {
				add(stack, stack.getCount());
			}
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

		private MeasuredItems copy() {
			MeasuredItems result = new MeasuredItems();
			result.addAll(this);
			return result;
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

		private ListTag save(HolderLookup.Provider lookupProvider) {
			ListTag list = new ListTag();
			for (FarmCoreData.StackAmount entry : this.entries) {
				CompoundTag entryTag = new CompoundTag();
				entryTag.put("Stack", entry.stack().saveOptional(lookupProvider));
				entryTag.putLong("Amount", entry.amount());
				list.add(entryTag);
			}
			return list;
		}

		private static MeasuredItems load(ListTag list, HolderLookup.Provider lookupProvider) {
			MeasuredItems result = new MeasuredItems();
			for (int index = 0; index < list.size(); index++) {
				CompoundTag entryTag = list.getCompound(index);
				result.add(ItemStack.parseOptional(lookupProvider, entryTag.getCompound("Stack")), entryTag.getLong("Amount"));
			}
			return result;
		}
	}

	private static final class MeasuredFluids {
		private final List<FarmCoreData.FluidAmount> entries = new ArrayList<>();

		private void add(FluidStack stack) {
			if (!stack.isEmpty()) {
				add(stack, stack.getAmount());
			}
		}

		private void add(FluidStack stack, long amount) {
			if (stack.isEmpty() || amount <= 0) {
				return;
			}
			for (int index = 0; index < entries.size(); index++) {
				FarmCoreData.FluidAmount current = entries.get(index);
				if (FluidStack.isSameFluidSameComponents(current.stack(), stack)) {
					entries.set(index, new FarmCoreData.FluidAmount(current.stack(), current.amount() + amount));
					return;
				}
			}
			entries.add(new FarmCoreData.FluidAmount(stack, amount));
		}

		private void addAll(MeasuredFluids other) {
			for (FarmCoreData.FluidAmount entry : other.entries) {
				add(entry.stack(), entry.amount());
			}
		}

		private long amountOf(FluidStack stack) {
			for (FarmCoreData.FluidAmount entry : entries) {
				if (FluidStack.isSameFluidSameComponents(entry.stack(), stack)) {
					return entry.amount();
				}
			}
			return 0;
		}

		private List<FarmCoreData.FluidAmount> entries() {
			return List.copyOf(entries);
		}

		private ListTag save(HolderLookup.Provider lookupProvider) {
			ListTag list = new ListTag();
			for (FarmCoreData.FluidAmount entry : this.entries) {
				CompoundTag entryTag = new CompoundTag();
				entryTag.put("Stack", entry.stack().saveOptional(lookupProvider));
				entryTag.putLong("Amount", entry.amount());
				list.add(entryTag);
			}
			return list;
		}

		private static MeasuredFluids load(ListTag list, HolderLookup.Provider lookupProvider) {
			MeasuredFluids result = new MeasuredFluids();
			for (int index = 0; index < list.size(); index++) {
				CompoundTag entryTag = list.getCompound(index);
				result.add(FluidStack.parseOptional(lookupProvider, entryTag.getCompound("Stack")), entryTag.getLong("Amount"));
			}
			return result;
		}
	}
}
