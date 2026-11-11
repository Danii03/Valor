package net.valor.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Where the raised Echo Flute sits in first person, shared by the hand renderer and the note particles.
 * <p>
 * The flute is centered horizontally and placed directly above the music panel, so it never hides
 * behind the staff. The panel's height on screen depends on GUI scale and window size, so the
 * screen reports it via {@link #setPanelTop(float)}.
 * <p>
 * Camera space: +X right, +Y up, the camera looks down -Z. Vanilla renders hands with a fixed
 * 70° vertical FOV, independent of the player's FOV setting.
 */
public final class EchoFluteFirstPersonLayout {
	/** Distance from the camera at which the flute was originally tuned. */
	private static final float REFERENCE_DEPTH = 0.6F;
	/**
	 * How much larger the flute appears than at {@link #REFERENCE_DEPTH}. Achieved by moving it closer
	 * (perspective), not by scaling, so it reads as "held nearer to the face". 1.0 = original look.
	 */
	private static final float PROXIMITY_MAGNIFICATION = 1.18F;
	/** Distance from the camera along -Z, in camera units. */
	public static final float DEPTH = REFERENCE_DEPTH / PROXIMITY_MAGNIFICATION;
	public static final float SCALE = 0.34F;
	/** Tilts the top of the flute towards the camera, as if looking down at it at the lips. Also gives nicer lighting. */
	public static final float TILT_DEGREES = -30.0F;

	/** Apparent flute size in camera space per unit of {@link #SCALE} (measured in game; includes the tilt). */
	private static final float APPARENT_HEIGHT_PER_SCALE = 0.39F;
	private static final float APPARENT_WIDTH_PER_SCALE = 0.33F;

	private static final float HAND_TAN_HALF_FOV = (float) Math.tan(Math.toRadians(70.0 / 2.0));
	/** Gap between the flute and the panel, as a fraction of screen height. */
	private static final float PANEL_GAP = 0.015F;
	/** The flute's center never rises above this screen fraction, so it stays in the lower half. */
	private static final float MIN_CENTER_FRACTION = 0.5F;
	private static final float DEFAULT_PANEL_TOP = 0.72F;

	private static float panelTopFraction = DEFAULT_PANEL_TOP;

	private EchoFluteFirstPersonLayout() {}

	/** Called by the music screen whenever its layout changes. 0 = top of the screen, 1 = bottom. */
	public static void setPanelTop(float fraction) {
		panelTopFraction = fraction;
	}

	/** Center of the flute in camera space. */
	public static Vector3f center() {
		float halfHeightFraction = APPARENT_HEIGHT_PER_SCALE * SCALE / 2.0F / visibleHeight();
		float centerFraction = Math.max(MIN_CENTER_FRACTION, panelTopFraction - PANEL_GAP - halfHeightFraction);
		return new Vector3f(0.0F, fractionToY(centerFraction), -DEPTH);
	}

	/**
	 * Where note particles start in first person, as normalized device coordinates (-1..1, +Y up):
	 * just right of the flute's upper edge, so they rise beside the view center instead of through it.
	 */
	public static float[] particleAnchorNdc() {
		Minecraft minecraft = Minecraft.getInstance();
		float aspect = minecraft.getWindow().getWidth() / (float) Math.max(1, minecraft.getWindow().getHeight());
		Vector3f center = center();
		float halfWidth = APPARENT_WIDTH_PER_SCALE * SCALE / 2.0F;
		float halfHeight = APPARENT_HEIGHT_PER_SCALE * SCALE / 2.0F;
		float x = (halfWidth * 1.2F) / (DEPTH * HAND_TAN_HALF_FOV * aspect);
		float y = (center.y + halfHeight * 0.7F) / (DEPTH * HAND_TAN_HALF_FOV);
		return new float[] {Mth.clamp(x, -1.0F, 1.0F), Mth.clamp(y, -1.0F, 1.0F)};
	}

	/** Height of the visible area at {@link #DEPTH}, in camera units. */
	private static float visibleHeight() {
		return 2.0F * DEPTH * HAND_TAN_HALF_FOV;
	}

	private static float fractionToY(float screenFraction) {
		return (0.5F - screenFraction) * visibleHeight();
	}
}
