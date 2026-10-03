package com.darkgreen_world.betterminecartwithfurnace;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

/** Dispensers aimed at a minecart with furnace: water bucket puts it out, flint and steel lights it again. */
public final class DispenserBehaviors {
	private DispenserBehaviors() {
	}

	/** Wraps the vanilla behaviors, which are already registered when mods initialize. */
	public static void register() {
		DispenseItemBehavior placeWater = DispenserBlock.DISPENSER_REGISTRY.get(Items.WATER_BUCKET);
		DispenserBlock.registerBehavior(Items.WATER_BUCKET, (source, stack) -> {
			List<MinecartFurnace> minecarts = minecartsInFront(source);

			if (minecarts.isEmpty()) {
				return placeWater.dispense(source, stack);
			}

			boolean extinguished = false;

			for (MinecartFurnace minecart : minecarts) {
				extinguished |= Extinguishing.extinguish(minecart);
			}

			if (extinguished) {
				playSuccess(source);
			} else {
				source.level().levelEvent(1001, source.pos(), 0);
			}

			return stack;
		});

		DispenseItemBehavior lightFire = DispenserBlock.DISPENSER_REGISTRY.get(Items.FLINT_AND_STEEL);
		DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, (source, stack) -> {
			List<MinecartFurnace> minecarts = minecartsInFront(source).stream().filter(Extinguishing::isExtinguished).toList();

			if (minecarts.isEmpty()) {
				return lightFire.dispense(source, stack);
			}

			minecarts.forEach(minecart -> Extinguishing.reignite(minecart, source.center()));
			stack.hurtAndBreak(1, source.level(), null, item -> {});
			playSuccess(source);
			return stack;
		});
	}

	private static List<MinecartFurnace> minecartsInFront(BlockSource source) {
		BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
		return source.level().getEntitiesOfClass(MinecartFurnace.class, new AABB(target), Entity::isAlive);
	}

	/** Same click and smoke as a vanilla dispenser. */
	private static void playSuccess(BlockSource source) {
		source.level().levelEvent(1000, source.pos(), 0);
		source.level().levelEvent(2000, source.pos(), source.state().getValue(DispenserBlock.FACING).get3DDataValue());
	}
}
