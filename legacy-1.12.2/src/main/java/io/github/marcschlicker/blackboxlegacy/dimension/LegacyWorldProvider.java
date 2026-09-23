package io.github.marcschlicker.blackboxlegacy.dimension;

import net.minecraft.init.Biomes;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.IChunkGenerator;

public final class LegacyWorldProvider extends WorldProvider {
    @Override
    public void init() {
        biomeProvider = new BiomeProviderSingle(Biomes.PLAINS);
        hasSkyLight = true;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorFlat(world, world.getSeed(), false, "3;1*minecraft:air;1;");
    }

    @Override
    public DimensionType getDimensionType() {
        return DimensionType.getById(LegacyFarmDimension.getDimensionId());
    }

    @Override
    public int getAverageGroundLevel() {
        return 1;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }
}
