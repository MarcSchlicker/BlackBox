package io.github.marcschlicker.blackbox.procedures;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import io.github.marcschlicker.blackbox.util.FarmDimensionRuntime;

public class EmeraldBedrockTeleportProcedure {
	public static void execute(Entity entity) {
		if (entity instanceof ServerPlayer player && !player.level().isClientSide()) {
			FarmDimensionRuntime.returnFromFarm(player);
		}
	}
}
