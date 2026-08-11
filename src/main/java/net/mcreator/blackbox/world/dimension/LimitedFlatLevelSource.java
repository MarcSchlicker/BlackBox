package net.mcreator.blackbox.world.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.concurrent.CompletableFuture;

import net.mcreator.blackbox.util.FarmCell;

public final class LimitedFlatLevelSource extends FlatLevelSource {
	public static final MapCodec<LimitedFlatLevelSource> CODEC = FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(LimitedFlatLevelSource::new, LimitedFlatLevelSource::settings);

	public LimitedFlatLevelSource(FlatLevelGeneratorSettings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends LimitedFlatLevelSource> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
		if (!isFarmChunk(chunk.getPos().x, chunk.getPos().z)) {
			return CompletableFuture.completedFuture(chunk);
		}
		return super.fillFromNoise(blender, randomState, structureManager, chunk);
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
		if (isFarmChunk(chunk.getPos().x, chunk.getPos().z)) {
			super.buildSurface(region, structureManager, randomState, chunk);
		}
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState randomState) {
		return isFarmChunk(x >> 4, z >> 4) ? super.getBaseHeight(x, z, heightmapType, level, randomState) : level.getMinBuildHeight();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
		return isFarmChunk(x >> 4, z >> 4) ? super.getBaseColumn(x, z, level, randomState) : new NoiseColumn(level.getMinBuildHeight(), new net.minecraft.world.level.block.state.BlockState[0]);
	}

	@Override
	public void addDebugScreenInfo(java.util.List<String> info, RandomState randomState, BlockPos pos) {
		info.add("Blackbox farm cell: 3x3 chunks per core");
	}

	public static boolean isFarmChunk(int chunkX, int chunkZ) {
		int localX = Math.floorMod(chunkX, FarmCell.CELL_SPACING_CHUNKS);
		int localZ = Math.floorMod(chunkZ, FarmCell.CELL_SPACING_CHUNKS);
		return (localX <= 1 || localX >= FarmCell.CELL_SPACING_CHUNKS - 1) && (localZ <= 1 || localZ >= FarmCell.CELL_SPACING_CHUNKS - 1);
	}
}
