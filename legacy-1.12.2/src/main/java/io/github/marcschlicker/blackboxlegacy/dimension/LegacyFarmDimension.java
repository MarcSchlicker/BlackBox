package io.github.marcschlicker.blackboxlegacy.dimension;

import java.util.UUID;

import io.github.marcschlicker.blackboxlegacy.BlackBoxLegacy;
import io.github.marcschlicker.blackboxlegacy.item.LegacyCoreData;
import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public final class LegacyFarmDimension {
    private static final String RETURN_DIMENSION = "BlackBoxReturnDimension";
    private static final String RETURN_X = "BlackBoxReturnX";
    private static final String RETURN_Y = "BlackBoxReturnY";
    private static final String RETURN_Z = "BlackBoxReturnZ";
    private static final int CELL_SIZE_BLOCKS = 48;
    private static final int CELL_HALF_SIZE = CELL_SIZE_BLOCKS / 2;
    private static final int CELL_SPACING_BLOCKS = 1024;
    private static int dimensionId;
    private static DimensionType dimensionType;

    private LegacyFarmDimension() {
    }

    public static void register(int configuredDimensionId) {
        dimensionId = configuredDimensionId;
        if (DimensionManager.isDimensionRegistered(dimensionId)) {
            dimensionId = DimensionManager.getNextFreeDimId();
        }
        dimensionType = DimensionType.register("BlackBox Farm", "_blackbox_farm", dimensionId, LegacyWorldProvider.class, false);
        DimensionManager.registerDimension(dimensionId, dimensionType);
    }

    public static int getDimensionId() {
        return dimensionId;
    }

    public static void enter(EntityPlayerMP player, ItemStack core, int returnDimension, BlockPos returnPos) {
        if (player.getServer() == null) {
            return;
        }
        LegacyCoreData.ensureId(core);
        WorldServer farmWorld = player.getServer().getWorld(dimensionId);
        if (farmWorld == null) {
            return;
        }
        Cell cell = Cell.fromCore(core);
        prepareCell(farmWorld, cell);
        NBTTagCompound playerData = player.getEntityData();
        playerData.setInteger(RETURN_DIMENSION, returnDimension);
        playerData.setDouble(RETURN_X, returnPos.getX() + 0.5D);
        playerData.setDouble(RETURN_Y, returnPos.getY() + 1.0D);
        playerData.setDouble(RETURN_Z, returnPos.getZ() + 0.5D);
        player.changeDimension(dimensionId);
        player.setPositionAndUpdate(cell.centerX + 0.5D, 1.0D, cell.centerZ + 0.5D);
    }

    public static void returnToOrigin(EntityPlayerMP player) {
        NBTTagCompound playerData = player.getEntityData();
        if (!playerData.hasKey(RETURN_DIMENSION)) {
            return;
        }
        int returnDimension = playerData.getInteger(RETURN_DIMENSION);
        MinecraftServer server = player.getServer();
        WorldServer destination = server == null ? null : server.getWorld(returnDimension);
        if (destination == null) {
            destination = server == null ? null : server.getWorld(0);
        }
        if (destination == null) {
            return;
        }
        double x = playerData.getDouble(RETURN_X);
        double y = playerData.getDouble(RETURN_Y);
        double z = playerData.getDouble(RETURN_Z);
        player.changeDimension(destination.provider.getDimension());
        player.setPositionAndUpdate(x, y, z);
        playerData.removeTag(RETURN_DIMENSION);
        playerData.removeTag(RETURN_X);
        playerData.removeTag(RETURN_Y);
        playerData.removeTag(RETURN_Z);
    }

    private static void prepareCell(WorldServer world, Cell cell) {
        BlockPos marker = new BlockPos(cell.centerX, 0, cell.centerZ);
        if (world.getBlockState(marker).getBlock() == LegacyRegistry.DIMENSIONAL_BEDROCK) {
            return;
        }
        int minX = cell.centerX - CELL_HALF_SIZE;
        int maxX = minX + CELL_SIZE_BLOCKS - 1;
        int minZ = cell.centerZ - CELL_HALF_SIZE;
        int maxZ = minZ + CELL_SIZE_BLOCKS - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(x, 0, z), Blocks.BEDROCK.getDefaultState(), 2);
            }
        }
        world.setBlockState(marker, LegacyRegistry.DIMENSIONAL_BEDROCK.getDefaultState(), 2);
        for (int y = 0; y < world.getActualHeight(); y++) {
            for (int x = minX - 1; x <= maxX + 1; x++) {
                world.setBlockState(new BlockPos(x, y, minZ - 1), Blocks.BARRIER.getDefaultState(), 2);
                world.setBlockState(new BlockPos(x, y, maxZ + 1), Blocks.BARRIER.getDefaultState(), 2);
            }
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(minX - 1, y, z), Blocks.BARRIER.getDefaultState(), 2);
                world.setBlockState(new BlockPos(maxX + 1, y, z), Blocks.BARRIER.getDefaultState(), 2);
            }
        }
    }

    private static final class Cell {
        private final int centerX;
        private final int centerZ;

        private Cell(int centerX, int centerZ) {
            this.centerX = centerX;
            this.centerZ = centerZ;
        }

        private static Cell fromCore(ItemStack core) {
            UUID id = UUID.fromString(LegacyCoreData.data(core).getString("FarmId"));
            long mixedX = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);
            long mixedZ = id.getLeastSignificantBits() ^ Long.rotateLeft(id.getMostSignificantBits(), 29);
            int gridX = (int) Math.floorMod(mixedX, 40001L) - 20000;
            int gridZ = (int) Math.floorMod(mixedZ, 40001L) - 20000;
            return new Cell(gridX * CELL_SPACING_BLOCKS, gridZ * CELL_SPACING_BLOCKS);
        }
    }
}
