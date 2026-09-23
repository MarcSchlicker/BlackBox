package io.github.marcschlicker.blackboxlegacy.block;

import io.github.marcschlicker.blackboxlegacy.dimension.LegacyFarmDimension;
import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DimensionalBedrockBlock extends Block {
    public DimensionalBedrockBlock() {
        super(Material.ROCK);
        setRegistryName("dimensional_bedrock");
        setUnlocalizedName("dimensional_bedrock");
        setCreativeTab(LegacyRegistry.TAB);
        setBlockUnbreakable();
        setResistance(6000000.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote && player instanceof EntityPlayerMP && world.provider.getDimension() == LegacyFarmDimension.getDimensionId()) {
            LegacyFarmDimension.returnToOrigin((EntityPlayerMP) player);
        }
        return true;
    }
}
