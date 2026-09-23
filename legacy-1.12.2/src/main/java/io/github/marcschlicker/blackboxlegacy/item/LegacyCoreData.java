package io.github.marcschlicker.blackboxlegacy.item;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class LegacyCoreData {
    private static final String FARM_ID = "FarmId";
    private static final String PROFILE = "Profile";

    private LegacyCoreData() {
    }

    public static void ensureId(ItemStack core) {
        NBTTagCompound tag = data(core);
        if (!tag.hasKey(FARM_ID)) {
            tag.setString(FARM_ID, UUID.randomUUID().toString());
        }
    }

    public static boolean isProgrammed(ItemStack core) {
        return !core.isEmpty() && !data(core).getString(PROFILE).isEmpty();
    }

    public static String profile(ItemStack core) {
        return data(core).getString(PROFILE);
    }

    public static void programIronFarm(ItemStack core) {
        ensureId(core);
        data(core).setString(PROFILE, "iron");
        core.setStackDisplayName("Iron Farm Core");
    }

    public static NBTTagCompound data(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
