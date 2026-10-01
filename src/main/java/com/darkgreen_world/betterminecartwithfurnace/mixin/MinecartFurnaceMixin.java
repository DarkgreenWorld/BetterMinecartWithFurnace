package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.BetterMinecartWithFurnace;
import com.darkgreen_world.betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin extends AbstractMinecart implements ExtinguishableMinecartWithFurnace {
	/** Vanilla hoppers move one item every 8 ticks. */
	@Unique
	private static final int HOPPER_TRANSFER_INTERVAL = 8;

	@Shadow
	private int fuel;

	@Shadow
	public Vec3 push;

	@Shadow
	protected abstract void setHasFuel(boolean fuel);

	@Shadow
	public abstract boolean addFuel(Vec3 interactingPos, ItemStack itemStack);

	/** Whether the minecart was on an inactive powered rail at the start of this tick (the same block vanilla moveAlongTrack checks for braking). */
	@Unique
	private boolean betterMinecartWithFurnace$onBrakeRail;

	@Unique
	private boolean betterMinecartWithFurnace$refuellingFromHopper;

	protected MinecartFurnaceMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean betterMinecartWithFurnace$isExtinguished() {
		return this.entityTags().contains(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
	}

	// ---------------------------------------------------------------- Extinguishing / reigniting

	@Override
	public boolean betterMinecartWithFurnace$isBurning() {
		return this.fuel > 0 && !this.betterMinecartWithFurnace$isExtinguished();
	}

	@Override
	public boolean betterMinecartWithFurnace$extinguish() {
		if (!this.betterMinecartWithFurnace$isBurning()) {
			return false;
		}

		this.addTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		this.setHasFuel(false);
		this.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + this.random.nextFloat() * 0.4F);

		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.8, this.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
		}

		return true;
	}

	@Override
	public void betterMinecartWithFurnace$reignite(Vec3 igniterPos) {
		this.removeTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		this.setHasFuel(this.fuel > 0);

		// A minecart put out with water still has its push. An empty one refuelled by a hopper has none,
		// so it heads away from whoever lit it, like vanilla does when fuel is added.
		if (this.push.lengthSqr() <= 1.0E-7) {
			this.push = this.position().subtract(igniterPos).horizontal();
		}
	}

	@Inject(method = "interact", at = @At("HEAD"))
	private void betterMinecartWithFurnace$useBucketOrFlintAndSteel(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		if (this.level().isClientSide()) {
			return;
		}

		// Neither item is fuel, so vanilla interact does nothing afterwards and returns SUCCESS; no need to cancel.
		// Flint and steel only works on an extinguished minecart: an empty one has no fuel to light.
		ItemStack itemStack = player.getItemInHand(hand);

		if (itemStack.is(Items.WATER_BUCKET)) {
			this.betterMinecartWithFurnace$extinguish();
		} else if (itemStack.is(Items.FLINT_AND_STEEL) && this.betterMinecartWithFurnace$isExtinguished()) {
			this.betterMinecartWithFurnace$reignite(player.position());
			this.playSound(SoundEvents.FLINTANDSTEEL_USE, 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
			itemStack.hurtAndBreak(1, player, hand);
		}
	}

	/** Reignite when fuel is added by a player (or by another mod calling addFuel), but not when a hopper adds it. */
	@Inject(method = "addFuel", at = @At("RETURN"))
	private void betterMinecartWithFurnace$reigniteOnRefuel(Vec3 interactingPos, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() && !this.betterMinecartWithFurnace$refuellingFromHopper && this.betterMinecartWithFurnace$isExtinguished()) {
			this.betterMinecartWithFurnace$reignite(interactingPos);
		}
	}

	// ---------------------------------------------------------------- Fuel is frozen while extinguished; hoppers refuel it while not burning

	@Inject(method = "tick", at = @At("HEAD"))
	private void betterMinecartWithFurnace$beforeTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		BlockState rail = this.level().getBlockState(this.getCurrentBlockPosOrRailBelow());
		this.betterMinecartWithFurnace$onBrakeRail = rail.is(Blocks.POWERED_RAIL) && !rail.getValue(PoweredRailBlock.POWERED);

		// Vanilla does --fuel at the end of tick, so add it back in advance. fuel stays > 0, so vanilla never clears push.
		if (this.fuel > 0 && this.betterMinecartWithFurnace$isExtinguished()) {
			this.fuel++;
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void betterMinecartWithFurnace$afterTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		if (this.fuel <= 0) {
			// "Extinguished" requires fuel to preserve (the tag may have been added by a command); without any it is just an empty minecart.
			this.removeTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		}

		if (!this.betterMinecartWithFurnace$isBurning() && this.tickCount % HOPPER_TRANSFER_INTERVAL == 0) {
			this.betterMinecartWithFurnace$refuelFromHopperAbove();
		}
	}

	/** While extinguished, clients should see an unlit furnace and no smoke. */
	@ModifyVariable(method = "setHasFuel", at = @At("HEAD"), argsOnly = true)
	private boolean betterMinecartWithFurnace$hideFlameWhileExtinguished(boolean fuel) {
		return fuel && !this.betterMinecartWithFurnace$isExtinguished();
	}

	/**
	 * Takes one fuel item from the hopper directly above. This only adds burn time: it does not light the minecart or
	 * change its direction. An empty minecart becomes extinguished once it has fuel.
	 */
	@Unique
	private void betterMinecartWithFurnace$refuelFromHopperAbove() {
		BlockPos hopperPos = this.blockPosition().above();
		BlockState state = this.level().getBlockState(hopperPos);

		// Same as a vanilla hopper: it has to point down and must not be locked by redstone.
		if (!state.is(Blocks.HOPPER) || state.getValue(HopperBlock.FACING) != Direction.DOWN || !state.getValue(HopperBlock.ENABLED)) {
			return;
		}

		if (!(this.level().getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)) {
			return;
		}

		// addFuel resets push from the "interacting position" and (see the injection above) reignites;
		// a hopper should do neither.
		Vec3 heading = this.push;
		this.betterMinecartWithFurnace$refuellingFromHopper = true;

		try {
			for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
				ItemStack itemStack = hopper.getItem(slot);

				if (!itemStack.isEmpty() && this.addFuel(this.position(), itemStack)) {
					hopper.removeItem(slot, 1);
					hopper.setChanged();
					this.addTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
					return;
				}
			}
		} finally {
			this.betterMinecartWithFurnace$refuellingFromHopper = false;
			this.push = heading;
		}
	}

	// ---------------------------------------------------------------- Engine: when it is off, and gradual acceleration

	@WrapMethod(method = "applyNaturalSlowdown")
	private Vec3 betterMinecartWithFurnace$applyNaturalSlowdown(Vec3 deltaMovement, Operation<Vec3> original) {
		// deltaMovement still carries gravity (y = -0.04) here; only the horizontal part matters, and the y of the
		// returned vector is always 0 anyway.
		Vec3 movement = deltaMovement.horizontal();
		Vec3 heading = betterMinecartWithFurnace$alongTrack(this.push, movement);
		Vec3 result;

		// The engine is off while extinguished. It is also off on an inactive powered rail, so braking works like it does
		// for a normal vanilla minecart (both physics implementations halve the speed each tick and zero it below 0.03);
		// otherwise the vanilla push would simply overpower the brake.
		if (this.betterMinecartWithFurnace$onBrakeRail || this.betterMinecartWithFurnace$isExtinguished()) {
			this.push = Vec3.ZERO;

			try {
				result = original.call(movement);
			} finally {
				this.push = heading;
			}
		} else {
			this.push = heading;
			result = original.call(movement);
		}

		return this.betterMinecartWithFurnace$limitAcceleration(movement, result);
	}

	/**
	 * Points push along the track (the line of movement), keeping its length and which way it faces.
	 *
	 * <p>Vanilla only does this once the minecart is faster than about 0.032 blocks/tick, which it normally is after one
	 * tick. With gradual acceleration it can stay slower than that, push would keep pointing away from the player
	 * instead of along the track, and the minecart would crawl forever. Vanilla also turns a push that is exactly
	 * perpendicular to the track into zero, which leaves a burning minecart that never moves again; here it follows
	 * the direction of movement instead.
	 */
	@Unique
	private static Vec3 betterMinecartWithFurnace$alongTrack(Vec3 heading, Vec3 movement) {
		double speed = movement.horizontalDistance();
		double length = heading.horizontalDistance();

		if (speed < 1.0E-6 || length < 1.0E-6) {
			return heading;
		}

		double scale = (heading.x * movement.x + heading.z * movement.z < 0.0 ? -length : length) / speed;
		return new Vec3(movement.x * scale, 0.0, movement.z * scale);
	}

	/**
	 * Vanilla does {@code 0.8 * v + push} every tick, and the length of push is the distance (several blocks) between
	 * the player and the minecart when fuel was added, so it exceeds top speed within a single tick. Until the minecart
	 * reaches top speed, this limits the speed gained per tick to {@code acceleration}; after that it is plain
	 * vanilla, so pushing other minecarts and climbing slopes are as strong as before.
	 */
	@Unique
	private Vec3 betterMinecartWithFurnace$limitAcceleration(Vec3 before, Vec3 after) {
		double speed = after.horizontalDistance();

		if (speed < 1.0E-9 || !(this.level() instanceof ServerLevel level)) {
			return after;
		}

		// Speed the minecart already had along its new direction. It is negative when reversing, which counts as 0,
		// i.e. it accelerates again from a standstill.
		double current = Math.max(0.0, (before.x * after.x + before.z * after.z) / speed);
		double allowed = current + BetterMinecartWithFurnace.acceleration();

		if (current >= this.getMaxSpeed(level) || speed <= allowed) {
			return after;
		}

		double scale = allowed / speed;
		return new Vec3(after.x * scale, after.y, after.z * scale);
	}
}
