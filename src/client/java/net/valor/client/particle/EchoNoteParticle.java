package net.valor.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;

/**
 * A tinted music note that rises slowly, sways gently sideways and fades out over ~1 second.
 * Uses vanilla's note sprite. The color comes from the {@link ColorParticleOption}.
 */
public final class EchoNoteParticle extends TextureSheetParticle {
	private static final float MAX_ALPHA = 0.9F;
	private static final float SWAY_STRENGTH = 0.0035F;

	private final float swayPhase;
	private final float swayDirX;
	private final float swayDirZ;

	private EchoNoteParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
			ColorParticleOption color, SpriteSet sprites) {
		super(level, x, y, z);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.friction = 0.95F;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.lifetime = 20 + this.random.nextInt(5); // 1.0–1.2 s
		this.quadSize = 0.14F;
		this.alpha = 0.0F;
		this.setColor(color.getRed(), color.getGreen(), color.getBlue());
		this.pickSprite(sprites);

		float swayAngle = this.random.nextFloat() * Mth.TWO_PI;
		this.swayDirX = Mth.cos(swayAngle);
		this.swayDirZ = Mth.sin(swayAngle);
		this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.isAlive()) {
			return;
		}
		// Gentle side-to-side sway along a random horizontal axis.
		float sway = Mth.sin(this.age * 0.3F + this.swayPhase) * SWAY_STRENGTH;
		this.xd += this.swayDirX * sway;
		this.zd += this.swayDirZ * sway;

		// Quick fade-in, then increasingly transparent until it's gone.
		float progress = this.age / (float) this.lifetime;
		float fadeIn = Math.min(1.0F, this.age / 2.0F);
		this.alpha = MAX_ALPHA * fadeIn * (1.0F - progress * progress);
	}

	/** Small pop-in: grows from 80 % to full size during the first ticks. */
	@Override
	public float getQuadSize(float partialTick) {
		float grow = Math.min(1.0F, (this.age + partialTick) / 4.0F);
		return this.quadSize * (0.8F + 0.2F * grow);
	}

	/** Self-lit, so the color stays true in caves and at night. */
	@Override
	protected int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<ColorParticleOption> {
		@Override
		public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z,
				double xd, double yd, double zd) {
			return new EchoNoteParticle(level, x, y, z, xd, yd, zd, options, this.sprites);
		}
	}
}
