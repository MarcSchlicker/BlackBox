package net.mcreator.blackbox.procedures;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.mcreator.blackbox.util.FarmDimensionRuntime;

public class EmeraldBedrockTeleportProcedure {
	public static void execute(Entity entity) {
		if (entity instanceof ServerPlayer player && !player.level().isClientSide()) {
			FarmDimensionRuntime.returnFromFarm(player);
		}
	}
}
