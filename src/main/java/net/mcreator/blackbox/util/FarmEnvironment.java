package net.mcreator.blackbox.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public enum FarmEnvironment {
	STANDARD("standard", "farmdimension"),
	OVERWORLD("overworld", "farm_overworld"),
	NETHER("nether", "farm_nether"),
	END("end", "farm_end");

	private final String id;
	private final ResourceKey<Level> dimension;

	FarmEnvironment(String id, String dimensionPath) {
		this.id = id;
		this.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("blackbox", dimensionPath));
	}

	public String id() {
		return this.id;
	}

	public ResourceKey<Level> dimension() {
		return this.dimension;
	}

	public static FarmEnvironment fromId(String id) {
		for (FarmEnvironment environment : values()) {
			if (environment.id.equals(id)) {
				return environment;
			}
		}
		return STANDARD;
	}

	public static boolean isFarmDimension(ResourceKey<Level> dimension) {
		for (FarmEnvironment environment : values()) {
			if (environment.dimension.equals(dimension)) {
				return true;
			}
		}
		return false;
	}
}
