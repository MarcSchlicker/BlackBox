package io.github.marcschlicker.blackboxlegacy.item;

import java.util.List;

import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public final class DimensionCoreItem extends Item {
    public DimensionCoreItem() {
        setRegistryName("dimension_core");
        setUnlocalizedName("dimension_core");
        setCreativeTab(LegacyRegistry.TAB);
        setMaxStackSize(1);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        items.add(new ItemStack(this));
        ItemStack ironFarm = new ItemStack(this);
        LegacyCoreData.programIronFarm(ironFarm);
        items.add(ironFarm);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        if (LegacyCoreData.isProgrammed(stack)) {
            tooltip.add("Legacy profile: " + LegacyCoreData.profile(stack));
            tooltip.add("Iron farm: 360 iron/hour with peaks");
        } else {
            tooltip.add("Unprogrammed farm core");
        }
    }
}
