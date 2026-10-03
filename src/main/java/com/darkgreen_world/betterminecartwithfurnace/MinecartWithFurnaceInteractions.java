package com.darkgreen_world.betterminecartwithfurnace;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Right-clicking a minecart with furnace: water bucket puts it out; flint and steel or fuel lights it again. */
public final class MinecartWithFurnaceInteractions {
	private MinecartWithFurnaceInteractions() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() || !(entity instanceof MinecartFurnace minecart)) {
				return InteractionResult.PASS;
			}

			ItemStack stack = player.getItemInHand(hand);

			if (stack.is(Items.WATER_BUCKET)) {
				Extinguishing.extinguish(minecart);
				return InteractionResult.SUCCESS;
			}

			if (!Extinguishing.isExtinguished(minecart)) {
				return InteractionResult.PASS;
			}

			if (stack.is(Items.FLINT_AND_STEEL)) {
				Extinguishing.reignite(minecart, player.position());
				minecart.playSound(SoundEvents.FLINTANDSTEEL_USE, 1.0F, minecart.getRandom().nextFloat() * 0.4F + 0.8F);
				stack.hurtAndBreak(1, player, hand);
				return InteractionResult.SUCCESS;
			}

			if (minecart.addFuel(player.position(), stack)) {
				stack.consume(1, player);
				Extinguishing.reignite(minecart, player.position());
				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});
	}
}
