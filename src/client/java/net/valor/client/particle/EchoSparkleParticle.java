package net.valor.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;

/**
 * Tiny tinted glitter that briefly trails an {@link EchoNoteParticle}.
 * Animates through vanilla's end rod glitter sprites while fading and shrinking.
 */
public final class EchoSparkleParticle extends TextureSheetParticle {
	private static final float MAX_ALPHA = 0.8F;

	private final SpriteSet sprites;

	private EchoSparkleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
			ColorParticleOption color, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.friction = 0.9F;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.lifetime = 8 + this.random.nextInt(7);
		this.quadSize = 0.035F;
		this.alpha = MAX_ALPHA;
		this.setColor(color.getRed(), color.getGreen(), color.getBlue());
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.isAlive()) {
			return;
		}
		this.setSpriteFromAge(this.sprites);
		this.alpha = MAX_ALPHA * (1.0F - this.age / (float) this.lifetime);
	}

	@Override
	public float getQuadSize(float partialTick) {
		float progress = (this.age + partialTick) / this.lifetime;
		return this.quadSize * (1.0F - 0.5F * progress);
	}

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
			return new EchoSparkleParticle(level, x, y, z, xd, yd, zd, options, this.sprites);
		}
	}
}
