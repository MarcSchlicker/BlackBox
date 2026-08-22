package net.mcreator.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import net.mcreator.blackbox.config.BlackboxConfig;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FarmCoreData {
	public static final int DATA_VERSION = 5;
	public static final UUID EXAMPLE_IRON_FARM_ID = UUID.fromString("bb000000-0000-4000-8000-000000000001");
	private static final String CORE_ID_TAG = "farm_core_id";
	private static final String EXAMPLE_TAG = "example_iron_farm";
	private static final String FARM_NAME_TAG = "farm_name";
	private static final String ENVIRONMENT_TAG = "farm_environment";
	private static final String CELL_SIZE_TAG = "cell_size_chunks";
	private static final String MOB_SPAWNING_TAG = "natural_mob_spawning";
	private static final String OWNER_TAG = "farm_owner";
	private static final String OWNER_NAME_TAG = "farm_owner_name";
	private static final String OWNER_TEAM_TAG = "farm_owner_team";
	private static final String ACCESS_TAG = "farm_access";
	private static final int MAX_LEGACY_ENTRIES = 64;

	private FarmCoreData() {
	}

	public record StackAmount(ItemStack stack, long amount) {
		public StackAmount {
			stack = stack.copyWithCount(1);
		}
	}

	public record FluidAmount(FluidStack stack, long amount) {
		public FluidAmount {
			stack = stack.copyWithAmount(1);
		}
	}

	public record EntityAmount(ResourceLocation entityType, long amount) {
		public EntityAmount {
			if (entityType == null) {
				throw new IllegalArgumentException("entityType must not be null");
			}
			amount = Math.max(0, amount);
		}
	}

	public record ProductionEvent(int tick, List<StackAmount> outputs) {
		public ProductionEvent {
			tick = Math.max(0, tick);
			outputs = List.copyOf(outputs);
		}
	}

	public record Recipe(int sampleTicks, List<StackAmount> inputs, List<StackAmount> outputs, List<ProductionEvent> timeline, List<EntityAmount> entityInputs,
			List<FluidAmount> fluidInputs, List<FluidAmount> fluidOutputs, long energyInput, long energyOutput) {
		public Recipe {
			inputs = List.copyOf(inputs);
			outputs = List.copyOf(outputs);
			timeline = timeline.isEmpty() && !outputs.isEmpty() ? List.of(new ProductionEvent(sampleTicks, outputs)) : List.copyOf(timeline);
			entityInputs = List.copyOf(entityInputs);
			fluidInputs = List.copyOf(fluidInputs);
			fluidOutputs = List.copyOf(fluidOutputs);
			energyInput = Math.max(0, energyInput);
			energyOutput = Math.max(0, energyOutput);
		}

		public boolean isValid() {
			return sampleTicks > 0 && (!outputs.isEmpty() || !fluidOutputs.isEmpty() || energyOutput > 0);
		}
	}

	public static boolean isProgrammed(ItemStack core) {
		CompoundTag tag = data(core);
		if (tag.getInt("blackbox_version") >= 3) {
			return !tag.getList("outputs", Tag.TAG_COMPOUND).isEmpty()
					|| !tag.getList("fluid_outputs", Tag.TAG_COMPOUND).isEmpty()
					|| tag.getLong("energy_output") > 0;
		}
		return isExampleIronFarm(core);
	}

	public static UUID ensureCoreId(ItemStack core) {
		Optional<UUID> existing = getCoreId(core);
		if (existing.isPresent()) {
			return existing.get();
		}
		UUID coreId = UUID.randomUUID();
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putString(CORE_ID_TAG, coreId.toString()));
		return coreId;
	}

	public static Optional<UUID> getCoreId(ItemStack core) {
		return readUuid(data(core).getString(CORE_ID_TAG));
	}

	public static String getFarmName(ItemStack core) {
		return data(core).getString(FARM_NAME_TAG).trim();
	}

	public static void setFarmName(ItemStack core, String name) {
		boolean hadFarmName = !getFarmName(core).isEmpty();
		String cleanName = name == null ? "" : name.trim();
		if (cleanName.length() > 32) {
			cleanName = cleanName.substring(0, 32);
		}
		String finalName = cleanName;
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> {
			if (finalName.isEmpty()) {
				tag.remove(FARM_NAME_TAG);
			} else {
				tag.putString(FARM_NAME_TAG, finalName);
			}
		});
		if (cleanName.isEmpty() && hadFarmName) {
			core.remove(DataComponents.CUSTOM_NAME);
		} else if (!cleanName.isEmpty()) {
			core.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(cleanName));
		}
	}

	public static FarmEnvironment getEnvironment(ItemStack core) {
		return FarmEnvironment.fromId(data(core).getString(ENVIRONMENT_TAG));
	}

	public static void setEnvironment(ItemStack core, FarmEnvironment environment) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putString(ENVIRONMENT_TAG, environment.id()));
	}

	public static int getCellSizeChunks(ItemStack core) {
		CompoundTag tag = data(core);
		if (tag.contains(CELL_SIZE_TAG, Tag.TAG_INT)) {
			return clampCellSize(tag.getInt(CELL_SIZE_TAG));
		}
		// Cores that already own a cell were 3x3 before variable sizes existed.
		return getCoreId(core).isPresent() ? 3 : 1;
	}

	public static void setCellSizeChunks(ItemStack core, int sizeChunks) {
		int size = clampCellSize(sizeChunks);
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putInt(CELL_SIZE_TAG, size));
	}

	public static ItemStack createEnvironmentCore(ItemStack core, FarmEnvironment environment) {
		setEnvironment(core, environment);
		setCellSizeChunks(core, 1);
		core.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable("item.blackbox.dimension_core.preset." + environment.id()));
		return core;
	}

	public static ItemStack createCellSizeCore(ItemStack core, int sizeChunks) {
		setCellSizeChunks(core, sizeChunks);
		core.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable("item.blackbox.dimension_core.size", sizeChunks, sizeChunks));
		return core;
	}

	public static boolean isMobSpawningEnabled(ItemStack core) {
		return data(core).getBoolean(MOB_SPAWNING_TAG);
	}

	public static void setMobSpawningEnabled(ItemStack core, boolean enabled) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putBoolean(MOB_SPAWNING_TAG, enabled));
	}

	public static UUID ensureOwner(ItemStack core, ServerPlayer player) {
		Optional<UUID> owner = getOwner(core);
		if (owner.isPresent()) {
			if (owner.get().equals(player.getUUID())) {
				updateOwnerIdentity(core, player);
			}
			return owner.get();
		}
		updateOwnerIdentity(core, player);
		return player.getUUID();
	}

	public static Optional<UUID> getOwner(ItemStack core) {
		return readUuid(data(core).getString(OWNER_TAG));
	}

	public static String getOwnerName(ItemStack core) {
		return data(core).getString(OWNER_NAME_TAG);
	}

	public static FarmAccessMode getAccessMode(ItemStack core) {
		return FarmAccessMode.fromId(data(core).getString(ACCESS_TAG));
	}

	public static void setAccessMode(ItemStack core, FarmAccessMode mode) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putString(ACCESS_TAG, mode.id()));
	}

	public static boolean canManage(ItemStack core, ServerPlayer player) {
		return player.hasPermissions(2) || getOwner(core).map(player.getUUID()::equals).orElse(true);
	}

	public static boolean canAccess(ItemStack core, ServerPlayer player) {
		if (player.hasPermissions(2) || getOwner(core).map(player.getUUID()::equals).orElse(true)) {
			return true;
		}
		FarmAccessMode mode = getAccessMode(core);
		if (mode == FarmAccessMode.PUBLIC) {
			return BlackboxConfig.ALLOW_PUBLIC_FARMS.get();
		}
		if (mode != FarmAccessMode.TEAM || player.getTeam() == null) {
			return false;
		}
		String ownerTeam = data(core).getString(OWNER_TEAM_TAG);
		return !ownerTeam.isBlank() && ownerTeam.equals(player.getTeam().getName());
	}

	private static void updateOwnerIdentity(ItemStack core, ServerPlayer player) {
		String team = player.getTeam() == null ? "" : player.getTeam().getName();
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> {
			tag.putString(OWNER_TAG, player.getUUID().toString());
			tag.putString(OWNER_NAME_TAG, player.getGameProfile().getName());
			tag.putString(OWNER_TEAM_TAG, team);
			if (!tag.contains(ACCESS_TAG)) {
				tag.putString(ACCESS_TAG, FarmAccessMode.PRIVATE.id());
			}
		});
	}

	public static void clearProfile(ItemStack core, HolderLookup.Provider lookupProvider) {
		write(core, lookupProvider, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 0,
				BlackboxConfig.warmupTicks(), BlackboxConfig.measurementTicks());
	}

	public static ItemStack createExampleIronFarmCore(ItemStack core) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> {
			tag.putString(CORE_ID_TAG, EXAMPLE_IRON_FARM_ID.toString());
			tag.putBoolean(EXAMPLE_TAG, true);
			tag.putInt(CELL_SIZE_TAG, 2);
		});
		core.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable("item.blackbox.dimension_core.example_iron"));
		return core;
	}

	public static boolean isExampleIronFarm(ItemStack core) {
		return data(core).getBoolean(EXAMPLE_TAG);
	}

	public static void write(ItemStack core, HolderLookup.Provider lookupProvider, List<StackAmount> inputs, List<StackAmount> outputs, int warmupTicks, int sampleTicks) {
		write(core, lookupProvider, inputs, outputs, List.of(), List.of(), List.of(), List.of(), 0, 0, warmupTicks, sampleTicks);
	}

	public static void write(ItemStack core, HolderLookup.Provider lookupProvider, List<StackAmount> inputs, List<StackAmount> outputs,
			List<ProductionEvent> timeline, int warmupTicks, int sampleTicks) {
		write(core, lookupProvider, inputs, outputs, timeline, List.of(), List.of(), List.of(), 0, 0, warmupTicks, sampleTicks);
	}

	public static void write(ItemStack core, HolderLookup.Provider lookupProvider, List<StackAmount> inputs, List<StackAmount> outputs,
			List<ProductionEvent> timeline, List<EntityAmount> entityInputs, List<FluidAmount> fluidInputs, List<FluidAmount> fluidOutputs, long energyInput, long energyOutput,
			int warmupTicks, int sampleTicks) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> {
			clearLegacyData(tag);
			tag.putInt("blackbox_version", DATA_VERSION);
			tag.putInt("warmup_ticks", warmupTicks);
			tag.putInt("sample_ticks", sampleTicks);
			tag.putInt("total_observation_ticks", warmupTicks + sampleTicks);
			tag.putString("sample_rule", "warmup ignored, following window measured");
			tag.put("inputs", writeEntries(lookupProvider, inputs));
			tag.put("outputs", writeEntries(lookupProvider, outputs));
			tag.put("timeline", writeTimeline(lookupProvider, timeline));
			tag.put("entity_inputs", writeEntityEntries(entityInputs));
			tag.put("fluid_inputs", writeFluidEntries(lookupProvider, fluidInputs));
			tag.put("fluid_outputs", writeFluidEntries(lookupProvider, fluidOutputs));
			tag.putLong("energy_input", Math.max(0, energyInput));
			tag.putLong("energy_output", Math.max(0, energyOutput));
			tag.putInt("input_count", inputs.size());
			tag.putInt("output_count", outputs.size());
			writeLegacyData(tag, inputs, outputs, sampleTicks);
		});
	}

	public static Recipe read(ItemStack core, HolderLookup.Provider lookupProvider) {
		CompoundTag tag = data(core);
		int sampleTicks = tag.getInt("sample_ticks");
		if (sampleTicks <= 0) {
			sampleTicks = 10 * 20;
		}
		if (tag.getInt("blackbox_version") >= 3) {
			return new Recipe(sampleTicks,
					readEntries(tag.getList("inputs", Tag.TAG_COMPOUND), lookupProvider),
					readEntries(tag.getList("outputs", Tag.TAG_COMPOUND), lookupProvider),
					readTimeline(tag.getList("timeline", Tag.TAG_COMPOUND), lookupProvider),
					readEntityEntries(tag.getList("entity_inputs", Tag.TAG_COMPOUND)),
					readFluidEntries(tag.getList("fluid_inputs", Tag.TAG_COMPOUND), lookupProvider),
					readFluidEntries(tag.getList("fluid_outputs", Tag.TAG_COMPOUND), lookupProvider),
					tag.getLong("energy_input"), tag.getLong("energy_output"));
		}
		if (isExampleIronFarm(core)) {
			ItemStack iron = new ItemStack(Items.IRON_INGOT);
			return new Recipe(60 * 20, List.of(), List.of(new StackAmount(iron, 16)), List.of(
					new ProductionEvent(0, List.of(new StackAmount(iron, 4))),
					new ProductionEvent(300, List.of(new StackAmount(iron, 2))),
					new ProductionEvent(850, List.of(new StackAmount(iron, 8))),
					new ProductionEvent(1100, List.of(new StackAmount(iron, 2)))
			), List.of(), List.of(), List.of(), 0, 0);
		}
		return readLegacy(tag, sampleTicks);
	}

	private static ListTag writeEntries(HolderLookup.Provider lookupProvider, List<StackAmount> entries) {
		ListTag list = new ListTag();
		for (StackAmount entry : entries) {
			if (entry.stack().isEmpty() || entry.amount() <= 0) {
				continue;
			}
			CompoundTag entryTag = new CompoundTag();
			entryTag.put("stack", entry.stack().copyWithCount(1).saveOptional(lookupProvider));
			entryTag.putLong("amount", entry.amount());
			list.add(entryTag);
		}
		return list;
	}

	private static List<StackAmount> readEntries(ListTag list, HolderLookup.Provider lookupProvider) {
		List<StackAmount> entries = new ArrayList<>();
		for (int index = 0; index < list.size(); index++) {
			CompoundTag entryTag = list.getCompound(index);
			ItemStack stack = ItemStack.parseOptional(lookupProvider, entryTag.getCompound("stack"));
			long amount = entryTag.getLong("amount");
			if (!stack.isEmpty() && amount > 0) {
				entries.add(new StackAmount(stack, amount));
			}
		}
		return entries;
	}

	private static ListTag writeFluidEntries(HolderLookup.Provider lookupProvider, List<FluidAmount> entries) {
		ListTag list = new ListTag();
		for (FluidAmount entry : entries) {
			if (entry.stack().isEmpty() || entry.amount() <= 0) {
				continue;
			}
			CompoundTag entryTag = new CompoundTag();
			entryTag.put("stack", entry.stack().copyWithAmount(1).saveOptional(lookupProvider));
			entryTag.putLong("amount", entry.amount());
			list.add(entryTag);
		}
		return list;
	}

	private static List<FluidAmount> readFluidEntries(ListTag list, HolderLookup.Provider lookupProvider) {
		List<FluidAmount> entries = new ArrayList<>();
		for (int index = 0; index < list.size(); index++) {
			CompoundTag entryTag = list.getCompound(index);
			FluidStack stack = FluidStack.parseOptional(lookupProvider, entryTag.getCompound("stack"));
			long amount = entryTag.getLong("amount");
			if (!stack.isEmpty() && amount > 0) {
				entries.add(new FluidAmount(stack, amount));
			}
		}
		return entries;
	}

	private static ListTag writeEntityEntries(List<EntityAmount> entries) {
		ListTag list = new ListTag();
		for (EntityAmount entry : entries) {
			if (entry.amount() <= 0 || !BuiltInRegistries.ENTITY_TYPE.containsKey(entry.entityType())) {
				continue;
			}
			CompoundTag entryTag = new CompoundTag();
			entryTag.putString("type", entry.entityType().toString());
			entryTag.putLong("amount", entry.amount());
			list.add(entryTag);
		}
		return list;
	}

	private static List<EntityAmount> readEntityEntries(ListTag list) {
		List<EntityAmount> entries = new ArrayList<>();
		for (int index = 0; index < list.size(); index++) {
			CompoundTag entryTag = list.getCompound(index);
			ResourceLocation type = ResourceLocation.tryParse(entryTag.getString("type"));
			long amount = entryTag.getLong("amount");
			if (type != null && amount > 0 && BuiltInRegistries.ENTITY_TYPE.containsKey(type)) {
				entries.add(new EntityAmount(type, amount));
			}
		}
		return entries;
	}

	private static ListTag writeTimeline(HolderLookup.Provider lookupProvider, List<ProductionEvent> timeline) {
		ListTag list = new ListTag();
		for (ProductionEvent event : timeline) {
			if (event.outputs().isEmpty()) {
				continue;
			}
			CompoundTag eventTag = new CompoundTag();
			eventTag.putInt("tick", event.tick());
			eventTag.put("outputs", writeEntries(lookupProvider, event.outputs()));
			list.add(eventTag);
		}
		return list;
	}

	private static List<ProductionEvent> readTimeline(ListTag list, HolderLookup.Provider lookupProvider) {
		List<ProductionEvent> timeline = new ArrayList<>();
		for (int index = 0; index < list.size(); index++) {
			CompoundTag eventTag = list.getCompound(index);
			List<StackAmount> outputs = readEntries(eventTag.getList("outputs", Tag.TAG_COMPOUND), lookupProvider);
			if (!outputs.isEmpty()) {
				timeline.add(new ProductionEvent(eventTag.getInt("tick"), outputs));
			}
		}
		return timeline;
	}

	private static Recipe readLegacy(CompoundTag tag, int sampleTicks) {
		List<StackAmount> outputs = new ArrayList<>();
		int lastSlot = Math.min(MAX_LEGACY_ENTRIES - 1, (int) tag.getDouble("slots"));
		for (int slot = 0; slot <= lastSlot; slot++) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString("produces" + slot));
			if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
				continue;
			}
			long amount = Math.round(tag.getDouble("sampleAmount" + slot));
			if (amount <= 0) {
				amount = Math.round(tag.getDouble("TotalAmount" + slot));
			}
			if (amount > 0) {
				outputs.add(new StackAmount(new ItemStack(BuiltInRegistries.ITEM.get(id)), amount));
			}
		}
		return new Recipe(sampleTicks, List.of(), outputs, List.of(), List.of(), List.of(), List.of(), 0, 0);
	}

	private static void writeLegacyData(CompoundTag tag, List<StackAmount> inputs, List<StackAmount> outputs, int sampleTicks) {
		int outputCount = Math.min(outputs.size(), MAX_LEGACY_ENTRIES);
		for (int slot = 0; slot < outputCount; slot++) {
			StackAmount entry = outputs.get(slot);
			double ticksPerItem = sampleTicks / (double) entry.amount();
			tag.putString("produces" + slot, BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());
			tag.putDouble("TotalAmount" + slot, entry.amount());
			tag.putDouble("sampleAmount" + slot, entry.amount());
			tag.putDouble("number" + slot, ticksPerItem);
			tag.putDouble("tick" + slot, ticksPerItem);
		}
		tag.putDouble("slots", outputCount - 1);
		int inputCount = Math.min(inputs.size(), MAX_LEGACY_ENTRIES);
		for (int slot = 0; slot < inputCount; slot++) {
			StackAmount entry = inputs.get(slot);
			tag.putString("consumes" + slot, BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());
			tag.putDouble("InputAmount" + slot, entry.amount());
		}
		tag.putDouble("input_slots", inputCount - 1);
	}

	private static void clearLegacyData(CompoundTag tag) {
		for (int slot = 0; slot < MAX_LEGACY_ENTRIES; slot++) {
			tag.remove("produces" + slot);
			tag.remove("TotalAmount" + slot);
			tag.remove("sampleAmount" + slot);
			tag.remove("number" + slot);
			tag.remove("tick" + slot);
			tag.remove("consumes" + slot);
			tag.remove("InputAmount" + slot);
		}
	}

	private static CompoundTag data(ItemStack core) {
		return core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	private static Optional<UUID> readUuid(String value) {
		try {
			return value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	private static int clampCellSize(int sizeChunks) {
		return Math.max(FarmCell.MIN_SIZE_CHUNKS, Math.min(FarmCell.MAX_SIZE_CHUNKS, sizeChunks));
	}
}
