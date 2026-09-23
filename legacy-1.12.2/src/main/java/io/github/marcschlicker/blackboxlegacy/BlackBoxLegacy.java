package io.github.marcschlicker.blackboxlegacy;

import io.github.marcschlicker.blackboxlegacy.dimension.LegacyFarmDimension;
import io.github.marcschlicker.blackboxlegacy.registry.LegacyRegistry;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = BlackBoxLegacy.MODID, name = BlackBoxLegacy.NAME, version = BlackBoxLegacy.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public final class BlackBoxLegacy {
    public static final String MODID = "blackboxlegacy";
    public static final String NAME = "BlackBox Legacy";
    public static final String VERSION = "1.0.0-legacy-1.12.2";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        int dimensionId = config.getInt("farmDimensionId", "dimension", 72, -1024, 1024,
                "Dimension id for BlackBox farm cells. Change this if SkyFactory already uses the id.");
        if (config.hasChanged()) {
            config.save();
        }
        LegacyRegistry.registerTileEntities();
        LegacyFarmDimension.register(dimensionId);
    }
}
