package net.mcreator.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FarmCoreData {
	public static final int DATA_VERSION = 3;
	public static final UUID EXAMPLE_IRON_FARM_ID = UUID.fromString("bb000000-0000-4000-8000-000000000001");
	private static final String CORE_ID_TAG = "farm_core_id";
	private static final String EXAMPLE_TAG = "example_iron_farm";
	private static final String FARM_NAME_TAG = "farm_name";
	private static final String ENVIRONMENT_TAG = "farm_environment";
	private static final String MOB_SPAWNING_TAG = "natural_mob_spawning";
	private static final int MAX_LEGACY_ENTRIES = 64;

	private FarmCoreData() {
	}

	public record StackAmount(ItemStack stack, long amount) {
		public StackAmount {
			stack = stack.copyWithCount(1);
		}
	}

	public record ProductionEvent(int tick, List<StackAmount> outputs) {
		public ProductionEvent {
			tick = Math.max(0, tick);
			outputs = List.copyOf(outputs);
		}
	}

	public record Recipe(int sampleTicks, List<StackAmount> inputs, List<StackAmount> outputs, List<ProductionEvent> timeline) {
		public Recipe {
			inputs = List.copyOf(inputs);
			outputs = List.copyOf(outputs);
			timeline = timeline.isEmpty() && !outputs.isEmpty() ? List.of(new ProductionEvent(sampleTicks, outputs)) : List.copyOf(timeline);
		}

		public boolean isValid() {
			return sampleTicks > 0 && !outputs.isEmpty();
		}
	}

	public static boolean isProgrammed(ItemStack core) {
		CompoundTag tag = core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.getInt("blackbox_version") >= DATA_VERSION ? !tag.getList("outputs", Tag.TAG_COMPOUND).isEmpty() : isExampleIronFarm(core);
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
		String value = core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(CORE_ID_TAG);
		try {
			return value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	public static String getFarmName(ItemStack core) {
		String name = core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(FARM_NAME_TAG).trim();
		return name.isEmpty() ? "" : name;
	}

	public static void setFarmName(ItemStack core, String name) {
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
	}

	public static FarmEnvironment getEnvironment(ItemStack core) {
		String environment = core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(ENVIRONMENT_TAG);
		return FarmEnvironment.fromId(environment);
	}

	public static void setEnvironment(ItemStack core, FarmEnvironment environment) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putString(ENVIRONMENT_TAG, environment.id()));
	}

	public static ItemStack createEnvironmentCore(ItemStack core, FarmEnvironment environment) {
		setEnvironment(core, environment);
		core.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable("item.blackbox.dimension_core.preset." + environment.id()));
		return core;
	}

	public static boolean isMobSpawningEnabled(ItemStack core) {
		return core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(MOB_SPAWNING_TAG);
	}

	public static void setMobSpawningEnabled(ItemStack core, boolean enabled) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> tag.putBoolean(MOB_SPAWNING_TAG, enabled));
	}

	public static void clearProfile(ItemStack core, HolderLookup.Provider lookupProvider) {
		write(core, lookupProvider, List.of(), List.of(), FarmDimensionRuntime.WARMUP_TICKS, FarmDimensionRuntime.SAMPLE_TICKS);
	}

	public static ItemStack createExampleIronFarmCore(ItemStack core) {
		CustomData.update(DataComponents.CUSTOM_DATA, core, tag -> {
			tag.putString(CORE_ID_TAG, EXAMPLE_IRON_FARM_ID.toString());
			tag.putBoolean(EXAMPLE_TAG, true);
		});
		core.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatable("item.blackbox.dimension_core.example_iron"));
		return core;
	}

	public static boolean isExampleIronFarm(ItemStack core) {
		return core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(EXAMPLE_TAG);
	}

	public static void write(ItemStack core, HolderLookup.Provider lookupProvider, List<StackAmount> inputs, List<StackAmount> outputs, int warmupTicks, int sampleTicks) {
		write(core, lookupProvider, inputs, outputs, List.of(), warmupTicks, sampleTicks);
	}

	public static void write(ItemStack core, HolderLookup.Provider lookupProvider, List<StackAmount> inputs, List<StackAmount> outputs, List<ProductionEvent> timeline, int warmupTicks, int sampleTicks) {
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
			tag.putInt("input_count", inputs.size());
			tag.putInt("output_count", outputs.size());
			writeLegacyData(tag, inputs, outputs, sampleTicks);
		});
	}

	public static Recipe read(ItemStack core, HolderLookup.Provider lookupProvider) {
		CompoundTag tag = core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		int sampleTicks = tag.getInt("sample_ticks");
		if (sampleTicks <= 0) {
			sampleTicks = 10 * 20;
		}
		if (tag.getInt("blackbox_version") >= DATA_VERSION) {
			return new Recipe(sampleTicks, readEntries(tag.getList("inputs", Tag.TAG_COMPOUND), lookupProvider), readEntries(tag.getList("outputs", Tag.TAG_COMPOUND), lookupProvider), readTimeline(tag.getList("timeline", Tag.TAG_COMPOUND), lookupProvider));
		}
		if (isExampleIronFarm(core)) {
			ItemStack iron = new ItemStack(Items.IRON_INGOT);
			return new Recipe(60 * 20, List.of(), List.of(new StackAmount(iron, 16)), List.of(
					new ProductionEvent(0, List.of(new StackAmount(iron, 4))),
					new ProductionEvent(300, List.of(new StackAmount(iron, 2))),
					new ProductionEvent(850, List.of(new StackAmount(iron, 8))),
					new ProductionEvent(1100, List.of(new StackAmount(iron, 2)))
			));
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
			String itemId = tag.getString("produces" + slot);
			ResourceLocation id = ResourceLocation.tryParse(itemId);
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
		return new Recipe(sampleTicks, List.of(), outputs, List.of());
	}

	private static void writeLegacyData(CompoundTag tag, List<StackAmount> inputs, List<StackAmount> outputs, int sampleTicks) {
		int outputCount = Math.min(outputs.size(), MAX_LEGACY_ENTRIES);
		for (int slot = 0; slot < outputCount; slot++) {
			StackAmount entry = outputs.get(slot);
			String itemId = BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString();
			double ticksPerItem = sampleTicks / (double) entry.amount();
			tag.putString("produces" + slot, itemId);
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
}
