package net.valor.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A tiny point of light drifting through the Whispering Woods: a few pixels in size, warm white or light
 * gold, now and then a soft mint, with a faint glow around it. Rises or drifts sideways with a gentle
 * wobble, may sparkle once, then slowly fades out after 2–5 seconds. Full bright, like vanilla's end rod
 * sparkles, so it stays visible in the shade of the canopy.
 * <p>
 * The particle shader discards pixels below 10 % opacity, so the glow's faint outer ring disappears
 * first while fading, which reads as the light shrinking.
 */
public final class GlimmerParticle extends TextureSheetParticle {
	private static final float MAX_ALPHA = 1.0F;
	/** Opacity of the glow around the core, relative to the core. */
	private static final float GLOW_ALPHA = 0.45F;
	/** Glow size relative to the core. */
	private static final float GLOW_SIZE = 3.2F;
	private static final int FADE_IN_TICKS = 8;
	/** Share of the lifetime at the end during which the glimmer fades out. */
	private static final float FADE_OUT = 0.5F;
	private static final float SPARKLE_CHANCE = 0.35F;
	private static final int SPARKLE_TICKS = 5;
	/** Sprite indices in glimmer.json: glitter 0–7 (0 = smallest dot), then the soft glow. */
	private static final int SPRITE_REST = 1;
	private static final int SPRITE_SPARKLE = 5;
	private static final int SPRITE_GLOW = 8;
	private static final int SPRITE_MAX = 8;

	private final SpriteSet sprites;
	private final float wobblePhase;
	private final float wobbleSpeed;
	/** Tick of the single sparkle, or -1 for none. */
	private final int sparkleAt;
	private final float baseSize;

	private GlimmerParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
		this.lifetime = 40 + this.random.nextInt(61);
		this.baseSize = 0.045F + this.random.nextFloat() * 0.02F;
		this.quadSize = this.baseSize;
		this.hasPhysics = false;
		this.gravity = 0.0F;
		this.friction = 0.96F;

		boolean sideways = this.random.nextFloat() < 0.35F;
		this.yd = sideways ? 0.001F : 0.004F + this.random.nextFloat() * 0.006F;
		float angle = this.random.nextFloat() * Mth.TWO_PI;
		float drift = sideways ? 0.008F : 0.002F;
		this.xd = Mth.cos(angle) * drift;
		this.zd = Mth.sin(angle) * drift;
		this.wobblePhase = this.random.nextFloat() * Mth.TWO_PI;
		this.wobbleSpeed = 0.08F + this.random.nextFloat() * 0.08F;

		this.sparkleAt = this.random.nextFloat() < SPARKLE_CHANCE
				? FADE_IN_TICKS + this.random.nextInt(Math.max(1, this.lifetime / 2 - FADE_IN_TICKS))
				: -1;

		float color = this.random.nextFloat();
		if (color < 0.55F) {
			this.setColor(1.0F, 0.97F, 0.88F); // warm white
		} else if (color < 0.9F) {
			this.setColor(1.0F, 0.87F, 0.52F); // light gold
		} else {
			this.setColor(0.74F, 1.0F, 0.84F); // soft mint
		}
		this.alpha = 0.0F;
		this.setSprite(sprites.get(SPRITE_REST, SPRITE_MAX));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.isAlive()) {
			return;
		}

		// Small irregular wobble on top of the base drift.
		float wobble = this.age * this.wobbleSpeed + this.wobblePhase;
		this.xd += Mth.sin(wobble) * 0.0006F;
		this.zd += Mth.cos(wobble * 1.3F) * 0.0006F;
		this.yd += (this.random.nextFloat() - 0.5F) * 0.0004F;

		float fadeIn = Math.min(this.age / (float) FADE_IN_TICKS, 1.0F);
		float remaining = 1.0F - this.age / (float) this.lifetime;
		float fadeOut = Math.min(remaining / FADE_OUT, 1.0F);
		this.alpha = MAX_ALPHA * fadeIn * fadeOut;

		boolean sparkling = this.sparkleAt >= 0 && this.age >= this.sparkleAt && this.age < this.sparkleAt + SPARKLE_TICKS;
		if (sparkling) {
			float t = (this.age - this.sparkleAt) / (float) SPARKLE_TICKS;
			float flash = 1.0F - Math.abs(2.0F * t - 1.0F);
			this.quadSize = this.baseSize * (1.0F + 0.9F * flash);
			this.alpha = Math.min(1.0F, this.alpha + 0.3F * flash);
			this.setSprite(this.sprites.get(SPRITE_SPARKLE, SPRITE_MAX));
		} else {
			this.quadSize = this.baseSize;
			this.setSprite(this.sprites.get(SPRITE_REST, SPRITE_MAX));
		}
	}

	/** Core, then a larger, fainter glow around it in the same color. */
	@Override
	public void render(VertexConsumer consumer, Camera camera, float partialTick) {
		super.render(consumer, camera, partialTick);

		TextureAtlasSprite core = this.sprite;
		float coreSize = this.quadSize;
		float coreAlpha = this.alpha;
		this.setSprite(this.sprites.get(SPRITE_GLOW, SPRITE_MAX));
		this.quadSize = coreSize * GLOW_SIZE;
		this.alpha = coreAlpha * GLOW_ALPHA;
		super.render(consumer, camera, partialTick);
		this.setSprite(core);
		this.quadSize = coreSize;
		this.alpha = coreAlpha;
	}

	@Override
	protected int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
				double xd, double yd, double zd) {
			return new GlimmerParticle(level, x, y, z, this.sprites);
		}
	}
}
