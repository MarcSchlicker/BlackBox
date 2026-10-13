package io.github.marcschlicker.blackbox.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import io.github.marcschlicker.blackbox.block.entity.FarmActorBlockEntity;

import javax.annotation.Nullable;

/** A non-solid anchor for the short-lived player entity used by farm measurements. */
public class FarmActorBlock extends Block implements EntityBlock {
	public FarmActorBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5F, 6.0F).noCollission().noOcclusion());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FarmActorBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof FarmActorBlockEntity actor) {
			actor.bindToFarm(player);
		}
	}
}
