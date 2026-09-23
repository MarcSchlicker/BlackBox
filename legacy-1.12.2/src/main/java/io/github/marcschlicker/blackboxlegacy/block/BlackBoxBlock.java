package io.github.marcschlicker.blackboxlegacy.block;

import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import io.github.marcschlicker.blackboxlegacy.tile.MachineTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlackBoxBlock extends Block {
    public BlackBoxBlock() {
        super(Material.IRON);
        setRegistryName("blackbox");
        setUnlocalizedName("blackbox");
        setCreativeTab(LegacyRegistry.TAB);
        setHardness(3.5F);
        setResistance(12.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MachineTileEntity();
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
            ItemStack core = machine.removeCore();
            if (!core.isEmpty() && !player.inventory.addItemStackToInventory(core)) {
                player.dropItem(core, false);
            }
            return true;
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        MachineTileEntity machine = machineAt(world, pos);
        if (machine != null) {
            machine.dropContents();
        }
        super.breakBlock(world, pos, state);
    }

    protected MachineTileEntity machineAt(World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof MachineTileEntity ? (MachineTileEntity) tileEntity : null;
    }
}
