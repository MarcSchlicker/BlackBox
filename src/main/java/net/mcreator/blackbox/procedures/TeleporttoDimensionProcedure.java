package net.mcreator.blackbox.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.util.FarmDimensionRuntime;
import net.mcreator.blackbox.util.FarmCell;

public class TeleporttoDimensionProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
			return;
		}

		BlockPos workbenchPos = BlockPos.containing(x, y, z);
		ResourceKey<Level> destinationType = FarmDimensionRuntime.resolveFarmDimension(player, workbenchPos);
		ResourceKey<Level> workbenchDimension = player.level().dimension();
		if (!player.level().dimension().equals(destinationType)) {
			BlackboxModVariables.PlayerVariables vars = player.getData(BlackboxModVariables.PLAYER_VARIABLES);
			vars.OverworldPositionx = player.getX();
			vars.OverworldPositiony = player.getY();
			vars.OverworldPositionz = player.getZ();
			vars.markSyncDirty();
		}

		ServerLevel nextLevel = player.server.getLevel(destinationType);
		if (nextLevel == null) {
			return;
		}

		FarmCell cell = FarmDimensionRuntime.enterFarmDimension(player, nextLevel, workbenchPos, workbenchDimension);
		if (cell == null) {
			return;
		}
		if (!player.level().dimension().equals(destinationType)) {
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
		}
		BlockPos spawn = cell.spawnPos();
		player.teleportTo(nextLevel, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
		player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
		for (MobEffectInstance effectInstance : player.getActiveEffects()) {
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effectInstance, false));
		}
		player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
	}
}
