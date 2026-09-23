package io.github.marcschlicker.blackboxlegacy.tile;

import javax.annotation.Nullable;

import io.github.marcschlicker.blackboxlegacy.item.LegacyCoreData;
import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class MachineTileEntity extends TileEntity implements ITickable {
    public static final int CORE_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 10;
    private static final int CYCLE_TICKS = 80 * 20;
    private final ItemStackHandler inventory = new ItemStackHandler(OUTPUT_END) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == CORE_SLOT && stack.getItem() == LegacyRegistry.DIMENSION_CORE && getStackInSlot(slot).isEmpty();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == CORE_SLOT ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };
    private int cycleTicks;

    public ItemStack getCore() {
        return inventory.getStackInSlot(CORE_SLOT);
    }

    public boolean insertCore(ItemStack core) {
        if (core.isEmpty() || core.getItem() != LegacyRegistry.DIMENSION_CORE || !getCore().isEmpty()) {
            return false;
        }
        ItemStack inserted = core.copy();
        inserted.setCount(1);
        LegacyCoreData.ensureId(inserted);
        inventory.setStackInSlot(CORE_SLOT, inserted);
        cycleTicks = 0;
        markDirty();
        return true;
    }

    public ItemStack removeCore() {
        ItemStack core = getCore();
        if (core.isEmpty()) {
            return ItemStack.EMPTY;
        }
        inventory.setStackInSlot(CORE_SLOT, ItemStack.EMPTY);
        cycleTicks = 0;
        markDirty();
        return core;
    }

    public void dropContents() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        ItemStack core = getCore();
        if (!LegacyCoreData.isProgrammed(core) || !"iron".equals(LegacyCoreData.profile(core))) {
            cycleTicks = 0;
            return;
        }
        int nextTick = cycleTicks + 1;
        if (nextTick > CYCLE_TICKS) {
            nextTick = 1;
        }
        if ((nextTick == 200 || nextTick == 1000) && !canStorePeak()) {
            return;
        }
        if (nextTick == 200 || nextTick == 1000) {
            store(new ItemStack(Items.IRON_INGOT, 4));
            store(new ItemStack(net.minecraft.init.Blocks.RED_FLOWER, 1, 0));
        }
        cycleTicks = nextTick;
        markDirty();
    }

    private boolean canStorePeak() {
        return canStore(new ItemStack(Items.IRON_INGOT, 4)) && canStore(new ItemStack(net.minecraft.init.Blocks.RED_FLOWER, 1, 0));
    }

    private boolean canStore(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, true);
        }
        return remaining.isEmpty();
    }

    private void store(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, false);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Items", inventory.serializeNBT());
        compound.setInteger("CycleTicks", cycleTicks);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        cycleTicks = compound.getInteger("CycleTicks");
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}
