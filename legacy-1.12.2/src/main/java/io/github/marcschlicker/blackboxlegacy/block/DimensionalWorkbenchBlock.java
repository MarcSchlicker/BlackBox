package io.github.marcschlicker.blackboxlegacy.block;

import io.github.marcschlicker.blackboxlegacy.dimension.LegacyFarmDimension;
import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import io.github.marcschlicker.blackboxlegacy.tile.MachineTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DimensionalWorkbenchBlock extends BlackBoxBlock {
    public DimensionalWorkbenchBlock() {
        setRegistryName("dimensional_workbench");
        setUnlocalizedName("dimensional_workbench");
        setHardness(3.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        MachineTileEntity machine = machineAt(world, pos);
        if (machine == null) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() == LegacyRegistry.DIMENSION_CORE && machine.getCore().isEmpty()) {
            if (machine.insertCore(held)) {
                held.shrink(1);
            }
            return true;
        }
        if (held.isEmpty() && player.isSneaking()) {
            return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
        }
        if (held.isEmpty() && player instanceof EntityPlayerMP && !machine.getCore().isEmpty()) {
            LegacyFarmDimension.enter((EntityPlayerMP) player, machine.getCore(), world.provider.getDimension(), pos);
            return true;
        }
        return true;
    }
}
