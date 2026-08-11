package net.mcreator.blackbox.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import net.mcreator.blackbox.client.BlueprintLibraryClient;
import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.network.BlueprintLibraryMessage;
import net.mcreator.blackbox.util.BlueprintLibrary;
import net.mcreator.blackbox.util.FarmEnvironment;

import java.util.List;

public class BlueprintItem extends Item {
	public enum StorageScope {
		LOCAL("local"),
		SERVER("server");

		private final String id;

		StorageScope(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		public static StorageScope fromId(String id) {
			return SERVER.id.equals(id) ? SERVER : LOCAL;
		}
	}

	public BlueprintItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			clearLegacyItemData(serverPlayer.getItemInHand(hand));
			BlueprintLibraryMessage.sendLibrary(serverPlayer, hand);
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (!FarmEnvironment.isFarmDimension(context.getLevel().dimension()) || player == null) {
			return InteractionResult.PASS;
		}
		if (!context.getLevel().getBlockState(context.getClickedPos()).is(BlackboxModBlocks.EMERALD_BEDROCK.get())) {
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.use_floor").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}

		BlackboxModVariables.PlayerVariables variables = player.getData(BlackboxModVariables.PLAYER_VARIABLES);
		String selectedId = variables.BlueprintSelectionId;
		StorageScope selectedScope = StorageScope.fromId(variables.BlueprintSelectionScope);
		StorageScope preferredStorage = StorageScope.fromId(variables.BlueprintPreferredStorage);
		boolean createNew = player.isShiftKeyDown() || selectedId.isBlank();
		if (context.getLevel().isClientSide()) {
			if (createNew) {
				BlueprintLibraryClient.openNamePrompt(context.getHand(), preferredStorage);
			} else if (selectedScope == StorageScope.LOCAL) {
				BlueprintLibraryClient.applyLocal(selectedId, context.getHand());
			}
			return InteractionResult.SUCCESS;
		}

		if (!(context.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.FAIL;
		}
		clearLegacyItemData(context.getItemInHand());
		if (createNew || selectedScope == StorageScope.LOCAL) {
			return InteractionResult.SUCCESS;
		}
		return BlueprintLibrary.apply(serverPlayer, level, selectedId) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.blackbox.blueprint.reusable").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("tooltip.blackbox.blueprint.save").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.blackbox.blueprint.apply").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.blackbox.blueprint.library").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static void clearLegacyItemData(ItemStack stack) {
		CompoundTag oldData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		boolean hadStoredBlueprint = oldData.contains("blueprint_id") || oldData.contains("blueprint_name");
		if (!oldData.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		}
		if (hadStoredBlueprint) {
			stack.remove(DataComponents.CUSTOM_NAME);
		}
	}
}
