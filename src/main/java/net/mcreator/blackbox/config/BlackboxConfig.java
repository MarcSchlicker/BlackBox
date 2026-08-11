package net.mcreator.blackbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BlackboxConfig {
	private static final List<String> DEFAULT_DENIED_BLOCKS = List.of(
			"minecraft:ender_chest",
			"blackbox:blackbox_block",
			"blackbox:dimensional_workbench"
	);
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec.IntValue MEASUREMENT_SECONDS = BUILDER
			.comment("Number of seconds recorded after the hidden warmup phase.")
			.defineInRange("measurementSeconds", 60, 10, 3600);
	public static final ModConfigSpec.IntValue WARMUP_SECONDS = BUILDER
			.comment("Hidden preparation time before a farm measurement begins.")
			.defineInRange("warmupSeconds", 30, 0, 600);
	public static final ModConfigSpec.IntValue MAX_BLUEPRINT_BLOCKS = BUILDER
			.comment("Maximum number of blocks stored in one blueprint.")
			.defineInRange("maxBlueprintBlocks", 20000, 256, 100000);
	public static final ModConfigSpec.IntValue MAX_SERVER_BLUEPRINTS = BUILDER
			.comment("Maximum number of blueprints listed from the server library.")
			.defineInRange("maxServerBlueprints", 256, 16, 2048);
	public static final ModConfigSpec.BooleanValue ALLOW_PUBLIC_FARMS = BUILDER
			.comment("Allow core owners to make farm cells public.")
			.define("allowPublicFarms", true);
	public static final ModConfigSpec.ConfigValue<List<? extends String>> DENIED_FARM_BLOCKS = BUILDER
			.comment("Blocks that players may not place inside a farm cell.")
			.defineListAllowEmpty("deniedFarmBlocks", DEFAULT_DENIED_BLOCKS, BlackboxConfig::isValidBlockId);
	public static final ModConfigSpec SPEC = BUILDER.build();

	private BlackboxConfig() {
	}

	public static int measurementTicks() {
		return MEASUREMENT_SECONDS.get() * 20;
	}

	public static int warmupTicks() {
		return WARMUP_SECONDS.get() * 20;
	}

	public static Set<Block> deniedBlocks() {
		Set<Block> result = new LinkedHashSet<>();
		for (String value : DENIED_FARM_BLOCKS.get()) {
			ResourceLocation id = ResourceLocation.tryParse(value);
			if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
				result.add(BuiltInRegistries.BLOCK.get(id));
			}
		}
		return result;
	}

	public static List<String> deniedBlockIds() {
		return new ArrayList<>(DENIED_FARM_BLOCKS.get());
	}

	public static void setDeniedBlocks(Collection<Block> blocks) {
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		for (Block block : blocks) {
			ids.add(BuiltInRegistries.BLOCK.getKey(block).toString());
		}
		DENIED_FARM_BLOCKS.set(List.copyOf(ids));
		SPEC.save();
	}

	private static boolean isValidBlockId(Object value) {
		return value instanceof String text && ResourceLocation.tryParse(text) != null;
	}
}
