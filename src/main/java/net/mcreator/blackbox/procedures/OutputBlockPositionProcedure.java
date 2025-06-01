package net.mcreator.blackbox.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.blackbox.network.BlackboxModVariables;

public class OutputBlockPositionProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		{
			BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
			_vars.OutputBlock_X = x;
			_vars.syncPlayerVariables(entity);
		}
		{
			BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
			_vars.OutputBlock_Y = y;
			_vars.syncPlayerVariables(entity);
		}
		{
			BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
			_vars.OutputBlock_Z = z;
			_vars.syncPlayerVariables(entity);
		}
	}
}
