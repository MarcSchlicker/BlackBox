package io.github.marcschlicker.blackboxlegacy.registry;

import io.github.marcschlicker.blackboxlegacy.BlackBoxLegacy;
import io.github.marcschlicker.blackboxlegacy.block.BlackBoxBlock;
import io.github.marcschlicker.blackboxlegacy.block.DimensionalBedrockBlock;
import io.github.marcschlicker.blackboxlegacy.block.DimensionalWorkbenchBlock;
import io.github.marcschlicker.blackboxlegacy.item.DimensionCoreItem;
import io.github.marcschlicker.blackboxlegacy.tile.MachineTileEntity;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = BlackBoxLegacy.MODID)
public final class LegacyRegistry {
    public static final CreativeTabs TAB = new CreativeTabs(BlackBoxLegacy.MODID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(net.minecraft.init.Items.ENDER_PEARL);
        }
    };

    public static final DimensionCoreItem DIMENSION_CORE = new DimensionCoreItem();
    public static final Block BLACKBOX = new BlackBoxBlock();
    public static final Block DIMENSIONAL_WORKBENCH = new DimensionalWorkbenchBlock();
    public static final Block DIMENSIONAL_BEDROCK = new DimensionalBedrockBlock();

    private LegacyRegistry() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BLACKBOX);
        event.getRegistry().register(DIMENSIONAL_WORKBENCH);
        event.getRegistry().register(DIMENSIONAL_BEDROCK);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(DIMENSION_CORE);
        event.getRegistry().register(new ItemBlock(BLACKBOX).setRegistryName(BLACKBOX.getRegistryName()));
        event.getRegistry().register(new ItemBlock(DIMENSIONAL_WORKBENCH).setRegistryName(DIMENSIONAL_WORKBENCH.getRegistryName()));
        event.getRegistry().register(new ItemBlock(DIMENSIONAL_BEDROCK).setRegistryName(DIMENSIONAL_BEDROCK.getRegistryName()));
    }

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(MachineTileEntity.class, new ResourceLocation(BlackBoxLegacy.MODID, "simulation_machine"));
    }
}
