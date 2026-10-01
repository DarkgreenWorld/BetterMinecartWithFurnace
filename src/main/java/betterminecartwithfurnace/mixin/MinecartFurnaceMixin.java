package betterminecartwithfurnace.mixin;

import betterminecartwithfurnace.BetterMinecartWithFurnace;
import betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
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
	/** 原版漏斗每 8 tick 传送一个物品。 */
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

	/** 上一次 applyNaturalSlowdown 返回的水平速度，以及它发生在哪个 tick。 */
	@Unique
	private double betterMinecartWithFurnace$lastSpeed;

	@Unique
	private int betterMinecartWithFurnace$lastSpeedTick;

	/** 本 tick 开始时矿车是否在未充能的动力铁轨上（与原版 moveAlongTrack 判断刹车用的是同一格）。 */
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

	// ---------------------------------------------------------------- 熄灭 / 重新点燃

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

		// 被水熄灭的矿车 push 还在；空矿车由漏斗加的燃料则没有方向，这时和原版添加燃料一样朝远离点火者的方向走。
		if (this.push.lengthSqr() <= 1.0E-7) {
			this.push = this.position().subtract(igniterPos).horizontal();
		}
	}

	@Inject(method = "interact", at = @At("HEAD"))
	private void betterMinecartWithFurnace$useBucketOrFlintAndSteel(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		if (this.level().isClientSide()) {
			return;
		}

		// 两种物品都不是燃料，原版 interact 接下来什么也不做并返回 SUCCESS，所以不需要取消。
		// 打火石只对熄灭状态的矿车有效：空矿车没有燃料，点不着。
		ItemStack itemStack = player.getItemInHand(hand);

		if (itemStack.is(Items.WATER_BUCKET)) {
			this.betterMinecartWithFurnace$extinguish();
		} else if (itemStack.is(Items.FLINT_AND_STEEL) && this.betterMinecartWithFurnace$isExtinguished()) {
			this.betterMinecartWithFurnace$reignite(player.position());
			this.playSound(SoundEvents.FLINTANDSTEEL_USE, 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
			itemStack.hurtAndBreak(1, player, hand);
		}
	}

	/** 玩家手持燃料右键（以及其他模组调用 addFuel）成功添加燃料时重新点燃；漏斗补充的不算。 */
	@Inject(method = "addFuel", at = @At("RETURN"))
	private void betterMinecartWithFurnace$reigniteOnRefuel(Vec3 interactingPos, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() && !this.betterMinecartWithFurnace$refuellingFromHopper && this.betterMinecartWithFurnace$isExtinguished()) {
			this.betterMinecartWithFurnace$reignite(interactingPos);
		}
	}

	// ---------------------------------------------------------------- 熄灭期间冻结燃料；没在燃烧时可由漏斗补充

	@Inject(method = "tick", at = @At("HEAD"))
	private void betterMinecartWithFurnace$beforeTick(CallbackInfo ci) {
		if (this.level().isClientSide()) {
			return;
		}

		BlockState rail = this.level().getBlockState(this.getCurrentBlockPosOrRailBelow());
		this.betterMinecartWithFurnace$onBrakeRail = rail.is(Blocks.POWERED_RAIL) && !rail.getValue(PoweredRailBlock.POWERED);

		// 原版 tick 末尾会 --fuel，这里先加回来；fuel 始终 > 0，所以原版也不会清空 push。
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
			// 熄灭状态必须有燃料可保留（标签可能是用命令加的）；没有就是普通的空矿车。
			this.removeTag(BetterMinecartWithFurnace.EXTINGUISHED_TAG);
		}

		if (!this.betterMinecartWithFurnace$isBurning() && this.tickCount % HOPPER_TRANSFER_INTERVAL == 0) {
			this.betterMinecartWithFurnace$refuelFromHopperAbove();
		}
	}

	/** 熄灭期间客户端应当看到没点着的熔炉，也不冒烟。 */
	@ModifyVariable(method = "setHasFuel", at = @At("HEAD"), argsOnly = true)
	private boolean betterMinecartWithFurnace$hideFlameWhileExtinguished(boolean fuel) {
		return fuel && !this.betterMinecartWithFurnace$isExtinguished();
	}

	/** 从正上方的漏斗取一个燃料。只增加燃烧时间，不点燃，也不改变行进方向；空矿车加了燃料后进入熄灭状态。 */
	@Unique
	private void betterMinecartWithFurnace$refuelFromHopperAbove() {
		BlockPos hopperPos = this.blockPosition().above();
		BlockState state = this.level().getBlockState(hopperPos);

		// 和原版漏斗一样：必须朝下，且没有被红石信号锁住。
		if (!state.is(Blocks.HOPPER) || state.getValue(HopperBlock.FACING) != Direction.DOWN || !state.getValue(HopperBlock.ENABLED)) {
			return;
		}

		if (!(this.level().getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)) {
			return;
		}

		// addFuel 会按“交互位置”重设 push，并且（见上面的注入）会重新点燃，这两件事漏斗都不该做。
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

	// ---------------------------------------------------------------- 发动机：停机条件与逐渐加速

	@WrapMethod(method = "applyNaturalSlowdown")
	private Vec3 betterMinecartWithFurnace$applyNaturalSlowdown(Vec3 deltaMovement, Operation<Vec3> original) {
		// 原版把 push 投影到带着重力分量（y = -0.04）的 deltaMovement 上，低速时 push 会几乎竖直。
		// 原版一 tick 就冲过了低速段所以无所谓，但逐渐加速会在低速段停留很久。返回值的 y 本来就恒为 0。
		Vec3 movement = deltaMovement.horizontal();
		Vec3 result;

		// 熄灭时发动机不工作；在未充能的动力铁轨上也不工作，这样刹车就和原版普通矿车一样
		// （新旧两套矿车物理都是每 tick 速度减半、低于 0.03 归零），否则原版的 push 会直接顶过刹车。
		if (this.betterMinecartWithFurnace$onBrakeRail || this.betterMinecartWithFurnace$isExtinguished()) {
			Vec3 heading = this.push;
			this.push = Vec3.ZERO;

			try {
				result = original.call(movement);
			} finally {
				this.push = betterMinecartWithFurnace$alongTrack(heading, movement);
			}
		} else {
			result = original.call(movement);
		}

		return this.betterMinecartWithFurnace$limitAcceleration(movement, result);
	}

	/** 与原版 calculateNewPushAlong 相同：让行进方向跟着轨道转弯，发动机停机时滑行过弯也不会丢。 */
	@Unique
	private static Vec3 betterMinecartWithFurnace$alongTrack(Vec3 heading, Vec3 movement) {
		if (heading.horizontalDistanceSqr() > 1.0E-4 && movement.horizontalDistanceSqr() > 0.001) {
			Vec3 realigned = heading.projectedOn(movement).normalize().scale(heading.length());

			if (realigned.lengthSqr() > 1.0E-7) {
				return realigned;
			}
		}

		return heading;
	}

	/**
	 * 原版每 tick 做 {@code 0.8 * v + push}，而 push 的长度是加燃料时玩家到矿车的距离（几格），
	 * 所以一 tick 就远超最高速度。这里把发动机带来的增速限制为每 tick {@code acceleration}，
	 * 直到矿车达到最高速度为止；之后完全交还给原版，推车、爬坡的力度不变。
	 */
	@Unique
	private Vec3 betterMinecartWithFurnace$limitAcceleration(Vec3 before, Vec3 after) {
		double speed = after.horizontalDistance();
		boolean continuous = this.betterMinecartWithFurnace$lastSpeedTick == this.tickCount - 1;
		double previous = this.betterMinecartWithFurnace$lastSpeed;
		this.betterMinecartWithFurnace$lastSpeedTick = this.tickCount;
		this.betterMinecartWithFurnace$lastSpeed = speed;

		if (speed < 1.0E-9 || !(this.level() instanceof ServerLevel level)) {
			return after;
		}

		// 矿车在发动机推进方向上已有的速度（还在反向滑行时为负）。
		double current = (before.x * after.x + before.z * after.z) / speed;

		if (current >= this.getMaxSpeed(level) - 1.0E-6) {
			return after;
		}

		double acceleration = BetterMinecartWithFurnace.acceleration();
		double maxAcceleration = BetterMinecartWithFurnace.POWERED_RAIL_ACCELERATION;
		double allowed;

		if (continuous && current > previous) {
			// 这一 tick 已经被外力加速过（下坡、被推），发动机只补到总增速不超过动力铁轨为止。
			allowed = current + Math.min(acceleration, Math.max(0.0, maxAcceleration - (current - previous)));
		} else {
			// 上坡重力、推其他矿车等造成的损失先补回来再加速，否则加速度一小就起不了步；
			// 但单 tick 的总增速仍不超过动力铁轨。减速、掉头不受限制，掉头后从 0 重新加速。
			double base = continuous ? Math.max(current, previous) : current;
			allowed = Math.max(acceleration, Math.min(base + acceleration, current + maxAcceleration));
		}

		if (speed <= allowed) {
			return after;
		}

		this.betterMinecartWithFurnace$lastSpeed = allowed;
		double scale = allowed / speed;
		return new Vec3(after.x * scale, after.y, after.z * scale);
	}
}
