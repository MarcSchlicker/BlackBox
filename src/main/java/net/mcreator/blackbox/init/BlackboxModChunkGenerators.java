package net.mcreator.blackbox.init;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.world.dimension.LimitedFlatLevelSource;

public final class BlackboxModChunkGenerators {
	public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> REGISTRY = DeferredRegister.create(Registries.CHUNK_GENERATOR, BlackboxMod.MODID);
	public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<LimitedFlatLevelSource>> LIMITED_FLAT = REGISTRY.register("limited_flat", () -> LimitedFlatLevelSource.CODEC);

	private BlackboxModChunkGenerators() {
	}
}
