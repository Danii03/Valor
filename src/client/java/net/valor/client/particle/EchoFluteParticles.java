package net.valor.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.valor.client.renderer.EchoFluteFirstPersonLayout;
import net.valor.registry.ValorParticleTypes;
import net.valor.world.item.EchoFluteNote;

/**
 * Spawns the rising note effect at the Echo Flute held by a player.
 * <p>
 * In first person the notes rise from the upper edge of the raised flute, on the flute hand's side,
 * so they pass beside the view center instead of through it. For everyone else
 * (third person, other players) it is placed in front of the player's mouth on the flute's side.
 */
public final class EchoFluteParticles {

	/** Distance of first-person particles from the eye, in blocks. */
	private static final double FIRST_PERSON_DISTANCE = 0.7;
	/** Notes in first person are much closer to the camera, so they are drawn smaller and slower. */
	private static final float FIRST_PERSON_SCALE = 0.55F;

	private static final double THIRD_PERSON_FORWARD = 0.42;
	private static final double THIRD_PERSON_SIDE = 0.28;
	private static final double THIRD_PERSON_DOWN = 0.3;

	private static final double RISE_SPEED = 0.022;
	private static final double DRIFT_SPEED = 0.004;
	private static final double MAX_VIEW_DISTANCE_SQR = 32.0 * 32.0;

	private EchoFluteParticles() {}

	public static void spawn(Player player, InteractionHand hand, EchoFluteNote note) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		boolean firstPerson = player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
		if (!firstPerson && minecraft.gameRenderer.getMainCamera().getPosition()
				.distanceToSqr(player.position()) > MAX_VIEW_DISTANCE_SQR) {
			return;
		}

		float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		double side = arm == HumanoidArm.RIGHT ? 1.0 : -1.0;

		float yaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
		Vec3 forwardFlat = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
		Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
		Vec3 eye = player.getEyePosition(partialTick);

		Vec3 origin;
		Vec3 drift;
		float scale;
		if (firstPerson) {
			// Start at the raised flute's upper edge, as seen on screen. The world uses the player's FOV
			// (hands always use 70°), so the screen position is converted with the world FOV.
			Vec3 look = player.getViewVector(partialTick);
			Vec3 viewUp = right.cross(look).normalize();
			float[] anchor = EchoFluteFirstPersonLayout.particleAnchorNdc();
			double tanHalfFov = Math.tan(Math.toRadians(minecraft.options.fov().get() / 2.0));
			double aspect = minecraft.getWindow().getWidth() / (double) Math.max(1, minecraft.getWindow().getHeight());
			double sideways = anchor[0] * side * FIRST_PERSON_DISTANCE * tanHalfFov * aspect;
			double upwards = anchor[1] * FIRST_PERSON_DISTANCE * tanHalfFov;
			origin = eye.add(look.scale(FIRST_PERSON_DISTANCE)).add(right.scale(sideways)).add(viewUp.scale(upwards));
			drift = look.scale(DRIFT_SPEED);
			scale = FIRST_PERSON_SCALE;
		} else {
			origin = eye.add(forwardFlat.scale(THIRD_PERSON_FORWARD))
					.add(right.scale(THIRD_PERSON_SIDE * side))
					.subtract(0.0, THIRD_PERSON_DOWN, 0.0);
			drift = forwardFlat.scale(DRIFT_SPEED);
			scale = 1.0F;
		}

		Vec3 velocity = drift.add(0.0, RISE_SPEED * scale, 0.0);
		int color = FastColor.ARGB32.opaque(note.color());

		Particle noteParticle = minecraft.particleEngine.createParticle(
				ColorParticleOption.create(ValorParticleTypes.ECHO_NOTE, color),
				origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
		if (noteParticle != null) {
			noteParticle.scale(scale);
		}

		spawnSparkles(minecraft, origin, velocity, color, scale);
	}

	/** 2–4 tiny glitter particles around the note that drift along with it (fewer on lower particle settings). */
	private static void spawnSparkles(Minecraft minecraft, Vec3 origin, Vec3 velocity, int noteColor, float scale) {
		ParticleStatus status = minecraft.options.particles().get();
		if (status == ParticleStatus.MINIMAL) {
			return;
		}
		RandomSource random = minecraft.level.getRandom();
		int count = status == ParticleStatus.ALL ? 2 + random.nextInt(3) : 1 + random.nextInt(2);
		int sparkleColor = FastColor.ARGB32.lerp(0.45F, noteColor, 0xFFFFFFFF);
		double spread = 0.08 * scale;

		for (int i = 0; i < count; i++) {
			Particle sparkle = minecraft.particleEngine.createParticle(
					ColorParticleOption.create(ValorParticleTypes.ECHO_SPARKLE, sparkleColor),
					origin.x + random.triangle(0.0, spread),
					origin.y + random.triangle(0.0, spread),
					origin.z + random.triangle(0.0, spread),
					velocity.x * 0.8 + random.triangle(0.0, 0.004 * scale),
					velocity.y * 0.9 + random.triangle(0.0, 0.004 * scale),
					velocity.z * 0.8 + random.triangle(0.0, 0.004 * scale));
			if (sparkle != null) {
				sparkle.scale(scale);
			}
		}
	}
}
