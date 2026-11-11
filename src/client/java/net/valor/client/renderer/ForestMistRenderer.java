package net.valor.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.valor.client.WhisperingWoodsAmbience;
import net.valor.client.WhisperingWoodsTerrain;

/**
 * Low ground mist of the Whispering Woods: soft, slowly drifting wisps between the trunks.
 * <p>
 * <b>Why not particles:</b> vanilla's particle (and textured) shaders throw away every pixel below
 * 10 % opacity, and translucent particles write depth, so overlapping wisps would cut each other off.
 * Soft mist lives exactly in that faint range. Each wisp is therefore drawn here as a small mesh:
 * an upright oval facing the camera, solid in the middle and fading to fully transparent at its
 * irregular, slowly changing rim. It is drawn with the untextured {@code position_color} shader
 * (which only discards fully transparent pixels), without depth writes, after translucent terrain.
 * <p>
 * Wisps sit on the actual ground (re-measured while they drift), gather in hollows and under the
 * canopy, fade in and out over seconds and towards the camera, and follow
 * {@link WhisperingWoodsAmbience#presence()}, so they vanish smoothly when leaving the forest.
 * Client only; at most {@link #MAX_WISPS} are alive (fewer with reduced particle settings).
 */
public final class ForestMistRenderer {
	/** Mist color #788F85, a desaturated grey-green; darkened by the light at the wisp. */
	private static final float RED = 0x78 / 255.0F;
	private static final float GREEN = 0x8F / 255.0F;
	private static final float BLUE = 0x85 / 255.0F;

	// ---- Density -----------------------------------------------------------------------------------
	/** Opacity in the middle of one wisp. Overlapping wisps add up. */
	private static final float MAX_ALPHA = 0.3F;
	/** Wisps alive at once, per particle setting (All / Decreased / Minimal). */
	private static final int MAX_WISPS = 56;
	private static final int MAX_WISPS_DECREASED = 32;
	private static final int MAX_WISPS_MINIMAL = 12;
	/** New wisps per tick at full presence, until the cap is reached. */
	private static final float SPAWN_RATE = 0.4F;

	// ---- Shape and placement ------------------------------------------------------------------------
	private static final float MIN_SPAWN_DISTANCE = 3.0F;
	private static final float MAX_SPAWN_DISTANCE = 24.0F;
	private static final float MIN_HALF_WIDTH = 2.2F;
	private static final float MAX_HALF_WIDTH = 4.5F;
	private static final float MIN_HALF_HEIGHT = 0.6F;
	private static final float MAX_HALF_HEIGHT = 1.1F;
	/** The oval's center sits this many half-heights above the ground; its lower part is hidden in the ground. */
	private static final float CENTER_ABOVE_GROUND = 0.4F;
	private static final int MIN_LIFETIME = 300;
	private static final int MAX_LIFETIME = 520;
	private static final float FADE_IN = 0.25F;
	private static final float FADE_OUT = 0.3F;
	private static final int RIM_SEGMENTS = 16;

	// ---- Visibility -------------------------------------------------------------------------------
	/** Closer to the camera than this a wisp is invisible, so the player never stands inside a flat sheet. */
	private static final float CAMERA_FADE_NEAR = 2.0F;
	private static final float CAMERA_FADE_FAR = 6.0F;
	private static final float FAR_FADE_START = 24.0F;
	private static final float FAR_FADE_END = 32.0F;
	/** Darkest the mist gets at night, so it stays faintly visible in moonlight. */
	private static final float MIN_BRIGHTNESS = 0.35F;

	private static final List<Wisp> WISPS = new ArrayList<>();

	private ForestMistRenderer() {}

	/** Called every client tick by {@link WhisperingWoodsAmbience}. */
	public static void tick(ClientLevel level, LocalPlayer player, float presence) {
		Iterator<Wisp> iterator = WISPS.iterator();
		while (iterator.hasNext()) {
			Wisp wisp = iterator.next();
			if (!wisp.tick(level, player)) {
				iterator.remove();
			}
		}

		RandomSource random = level.random;
		if (presence > 0.0F && WISPS.size() < maxWisps() && random.nextFloat() < SPAWN_RATE * presence) {
			Wisp wisp = Wisp.spawn(level, player, random);
			if (wisp != null) {
				WISPS.add(wisp);
			}
		}
	}

	public static void clear() {
		WISPS.clear();
	}

	private static int maxWisps() {
		return switch (Minecraft.getInstance().options.particles().get()) {
			case ALL -> MAX_WISPS;
			case DECREASED -> MAX_WISPS_DECREASED;
			case MINIMAL -> MAX_WISPS_MINIMAL;
		};
	}

	/** Draws all wisps. Registered for Fabric's {@code AFTER_TRANSLUCENT} world render event. */
	public static void render(WorldRenderContext context) {
		float presence = WhisperingWoodsAmbience.presence();
		ClientLevel level = context.world();
		if (WISPS.isEmpty() || presence <= 0.0F || level == null) {
			return;
		}

		Camera camera = context.camera();
		Vec3 cameraPos = camera.getPosition();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		float daylight = level.getSkyDarken(partialTick);

		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Wisp wisp : WISPS) {
			wisp.emit(builder, cameraPos, partialTick, daylight, presence);
		}
		MeshData mesh = builder.build();
		if (mesh == null) {
			return;
		}

		// Positions are relative to the camera; the view rotation is already on the model-view stack here.
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		BufferUploader.drawWithShader(mesh);
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	/** One mist wisp. Simulated per tick, interpolated per frame. */
	private static final class Wisp {
		private double x;
		private double z;
		private double xo;
		private double zo;
		private float groundY;
		private float groundYo;
		private float targetGroundY;
		private final double xd;
		private final double zd;
		private final float halfWidth;
		private final float halfHeight;
		private final float strength;
		private final int lifetime;
		private final float phaseA;
		private final float phaseB;
		private final float morphSpeed;
		private float brightnessSky;
		private float brightnessBlock;
		private int age;

		private Wisp(double x, double z, int ground, float strength, RandomSource random) {
			this.x = this.xo = x;
			this.z = this.zo = z;
			this.groundY = this.groundYo = this.targetGroundY = ground;
			float angle = random.nextFloat() * Mth.TWO_PI;
			float speed = 0.006F + random.nextFloat() * 0.01F;
			this.xd = Mth.cos(angle) * speed;
			this.zd = Mth.sin(angle) * speed;
			this.halfWidth = Mth.lerp(random.nextFloat(), MIN_HALF_WIDTH, MAX_HALF_WIDTH);
			this.halfHeight = Mth.lerp(random.nextFloat(), MIN_HALF_HEIGHT, MAX_HALF_HEIGHT);
			this.strength = strength;
			this.lifetime = Mth.nextInt(random, MIN_LIFETIME, MAX_LIFETIME);
			this.phaseA = random.nextFloat() * Mth.TWO_PI;
			this.phaseB = random.nextFloat() * Mth.TWO_PI;
			this.morphSpeed = 0.006F + random.nextFloat() * 0.008F;
		}

		/** Picks the more sheltered of two random spots near the player, so mist gathers in hollows. */
		private static Wisp spawn(ClientLevel level, LocalPlayer player, RandomSource random) {
			Wisp best = null;
			float bestScore = -1.0F;
			for (int attempt = 0; attempt < 2; attempt++) {
				float angle = random.nextFloat() * Mth.TWO_PI;
				float distance = Mth.lerp(random.nextFloat(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
				int x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
				int z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
				int ground = WhisperingWoodsTerrain.findGround(level, x, z, player.getBlockY());
				if (ground == WhisperingWoodsTerrain.NO_GROUND) {
					continue;
				}
				float hollow = WhisperingWoodsTerrain.hollowness(level, x, z, ground);
				float shaded = level.canSeeSky(new BlockPos(x, ground, z)) ? 0.0F : 1.0F;
				float score = 0.6F * hollow + 0.4F * shaded + 0.1F * random.nextFloat();
				if (score > bestScore) {
					bestScore = score;
					best = new Wisp(x + random.nextDouble(), z + random.nextDouble(), ground,
							Math.min(1.0F, 0.6F + 0.2F * shaded + 0.45F * hollow), random);
				}
			}
			if (best != null) {
				best.updateLight(level);
			}
			return best;
		}

		/** Returns false once the wisp has faded out. */
		private boolean tick(ClientLevel level, LocalPlayer player) {
			this.age++;
			this.xo = this.x;
			this.zo = this.z;
			this.groundYo = this.groundY;
			this.x += this.xd;
			this.z += this.zd;

			// Follow the terrain while drifting: re-measure now and then, then ease towards it.
			if (this.age % 10 == 0) {
				int ground = WhisperingWoodsTerrain.findGround(level, Mth.floor(this.x), Mth.floor(this.z), Mth.floor(this.groundY));
				if (ground != WhisperingWoodsTerrain.NO_GROUND) {
					this.targetGroundY = ground;
				}
				this.updateLight(level);
			}
			this.groundY += (this.targetGroundY - this.groundY) * 0.08F;

			double dx = this.x - player.getX();
			double dz = this.z - player.getZ();
			boolean tooFar = dx * dx + dz * dz > FAR_FADE_END * FAR_FADE_END;
			return this.age < this.lifetime && !tooFar;
		}

		private void updateLight(ClientLevel level) {
			BlockPos pos = BlockPos.containing(this.x, this.groundY + 0.5, this.z);
			this.brightnessSky = level.getBrightness(LightLayer.SKY, pos) / 15.0F;
			this.brightnessBlock = level.getBrightness(LightLayer.BLOCK, pos) / 15.0F;
		}

		private void emit(BufferBuilder builder, Vec3 camera, float partialTick, float daylight, float presence) {
			double worldX = Mth.lerp(partialTick, this.xo, this.x);
			double worldZ = Mth.lerp(partialTick, this.zo, this.z);
			double ground = Mth.lerp(partialTick, this.groundYo, this.groundY);
			float cx = (float) (worldX - camera.x);
			float cy = (float) (ground + CENTER_ABOVE_GROUND * this.halfHeight - camera.y);
			float cz = (float) (worldZ - camera.z);

			float distance = Mth.sqrt(cx * cx + cy * cy + cz * cz);
			float life = (this.age + partialTick) / this.lifetime;
			float lifeFade = Math.min(smoothstep(0.0F, FADE_IN, life), smoothstep(1.0F, 1.0F - FADE_OUT, life));
			float alpha = MAX_ALPHA * this.strength * presence * lifeFade
					* smoothstep(CAMERA_FADE_NEAR, CAMERA_FADE_FAR, distance)
					* smoothstep(FAR_FADE_END, FAR_FADE_START, distance);
			if (alpha < 0.004F) {
				return;
			}

			// Upright oval facing the camera horizontally (turns around the vertical axis only).
			float horizontal = Mth.sqrt(cx * cx + cz * cz);
			if (horizontal < 0.01F) {
				return;
			}
			float rightX = -cz / horizontal;
			float rightZ = cx / horizontal;

			float brightness = Math.max(MIN_BRIGHTNESS, Math.max(this.brightnessSky * daylight, this.brightnessBlock));
			float r = RED * brightness;
			float g = GREEN * brightness;
			float b = BLUE * brightness;

			float time = (this.age + partialTick) * this.morphSpeed;
			float innerAlpha = alpha * 0.7F;
			float previousOuterX = 0;
			float previousOuterY = 0;
			float previousOuterZ = 0;
			float previousInnerX = 0;
			float previousInnerY = 0;
			float previousInnerZ = 0;
			for (int i = 0; i <= RIM_SEGMENTS; i++) {
				float angle = i * Mth.TWO_PI / RIM_SEGMENTS;
				// Irregular, slowly changing rim, so no two wisps (and no two moments) look alike.
				float wobble = 1.0F + 0.16F * Mth.sin(2.0F * angle + this.phaseA + time)
						+ 0.1F * Mth.sin(3.0F * angle + this.phaseB - 1.3F * time);
				float across = Mth.cos(angle) * this.halfWidth * wobble;
				float up = Mth.sin(angle) * this.halfHeight * wobble;
				float outerX = cx + rightX * across;
				float outerY = cy + up;
				float outerZ = cz + rightZ * across;
				float innerX = cx + rightX * across * 0.5F;
				float innerY = cy + up * 0.5F;
				float innerZ = cz + rightZ * across * 0.5F;

				if (i > 0) {
					// Solid core.
					builder.addVertex(cx, cy, cz).setColor(r, g, b, alpha);
					builder.addVertex(previousInnerX, previousInnerY, previousInnerZ).setColor(r, g, b, innerAlpha);
					builder.addVertex(innerX, innerY, innerZ).setColor(r, g, b, innerAlpha);
					// Soft rim fading to fully transparent.
					builder.addVertex(previousInnerX, previousInnerY, previousInnerZ).setColor(r, g, b, innerAlpha);
					builder.addVertex(previousOuterX, previousOuterY, previousOuterZ).setColor(r, g, b, 0.0F);
					builder.addVertex(outerX, outerY, outerZ).setColor(r, g, b, 0.0F);
					builder.addVertex(previousInnerX, previousInnerY, previousInnerZ).setColor(r, g, b, innerAlpha);
					builder.addVertex(outerX, outerY, outerZ).setColor(r, g, b, 0.0F);
					builder.addVertex(innerX, innerY, innerZ).setColor(r, g, b, innerAlpha);
				}
				previousOuterX = outerX;
				previousOuterY = outerY;
				previousOuterZ = outerZ;
				previousInnerX = innerX;
				previousInnerY = innerY;
				previousInnerZ = innerZ;
			}
		}
	}
}
