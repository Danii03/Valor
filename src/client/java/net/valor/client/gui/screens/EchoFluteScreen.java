package net.valor.client.gui.screens;

import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.valor.client.particle.EchoFluteParticles;
import net.valor.client.renderer.EchoFluteFirstPersonLayout;
import net.valor.client.renderer.EchoFluteHandAnimation;
import net.valor.network.ServerboundPlayEchoFluteNotePayload;
import net.valor.registry.ValorItems;
import net.valor.world.item.EchoFluteNote;
import org.lwjgl.glfw.GLFW;

/**
 * Music interface of the Echo Flute.
 * <p>
 * Draws a five-line staff inside a translucent panel in the lower third of the screen, on top of
 * the visible world.
 * Keys A, S, D, F, G play the five {@link EchoFluteNote}s, from the lowest line to the highest.
 * The last {@value #MAX_NOTES} notes are shown on the staff. Everything is drawn with plain
 * fills, so no textures are needed.
 */
public final class EchoFluteScreen extends Screen {

	/** Keys for the notes, in the same order as {@link EchoFluteNote#values()}. */
	private static final int[] NOTE_KEYS = {
			GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G
	};
	private static final String[] NOTE_KEY_LABELS = {"A", "S", "D", "F", "G"};
	private static final EchoFluteNote[] NOTES = EchoFluteNote.values();

	private static final int MAX_NOTES = 8;

	// Layout, in scaled GUI pixels.
	private static final int LINE_SPACING = 9;
	private static final int STAFF_HEIGHT = LINE_SPACING * (NOTES.length - 1);
	private static final int MAX_STAFF_WIDTH = 220;
	private static final int MIN_SIDE_MARGIN = 36;
	private static final int STEM_LENGTH = 12;
	/** Gap between a key label and the start of its line. */
	private static final int LABEL_GAP = 8;

	// Panel behind staff and labels.
	private static final int PANEL_PADDING_X = 10;
	/** Leaves room for stems and the glow halo of notes on the outer lines. */
	private static final int PANEL_PADDING_Y = 11;
	/** Keeps the panel clear of the hotbar and the health/hunger bars. */
	private static final int PANEL_BOTTOM_CLEARANCE = 42;

	// Colors (ARGB).
	private static final int LINE_COLOR = 0xD0F4F1EA;
	private static final int SHADOW_COLOR = 0x50000000;
	/** Dark anthracite at ~62 % opacity: the world stays visible, notes stay readable. */
	private static final int PANEL_COLOR = 0x9E1C1F22;
	private static final int PANEL_BORDER_COLOR = 0x2EFFFFFF;

	// Animation.
	private static final long GLOW_DURATION_MS = 350L;
	/** How fast notes slide to a new slot when older ones drop out. Higher = snappier. */
	private static final float SLIDE_SPEED = 14.0F;

	private final InteractionHand hand;
	private final Deque<PlayedNote> notes = new ArrayDeque<>(MAX_NOTES + 1);
	private final boolean[] heldKeys = new boolean[NOTES.length];

	private int staffLeft;
	private int staffRight;
	/** Y of the lowest line. Line {@code i} sits at {@code staffBottom - i * LINE_SPACING}. */
	private int staffBottom;
	private int panelLeft;
	private int panelTop;
	private int panelRight;
	private int panelBottom;
	private long lastFrameMillis = Util.getMillis();

	public EchoFluteScreen(InteractionHand hand) {
		super(Component.translatable("screen.valor.echo_flute"));
		this.hand = hand;
	}

	// ---- Layout -----------------------------------------------------------------------------

	/** Runs on open and on every window resize. */
	@Override
	protected void init() {
		int staffWidth = Math.min(MAX_STAFF_WIDTH, this.width - 2 * MIN_SIDE_MARGIN);
		this.staffLeft = (this.width - staffWidth) / 2;
		this.staffRight = this.staffLeft + staffWidth;

		// Center the staff in the lower third, but never let the panel overlap the HUD at the bottom.
		int lowerThirdCenter = this.height * 5 / 6;
		int maxStaffBottom = this.height - PANEL_BOTTOM_CLEARANCE - PANEL_PADDING_Y - 1;
		this.staffBottom = Math.min(lowerThirdCenter + STAFF_HEIGHT / 2, maxStaffBottom);

		// The panel wraps labels and staff, with the same margin on both sides so the staff stays centered.
		int labelWidth = 0;
		for (String label : NOTE_KEY_LABELS) {
			labelWidth = Math.max(labelWidth, this.font.width(label));
		}
		int sideExtent = LABEL_GAP + labelWidth + PANEL_PADDING_X;
		this.panelLeft = this.staffLeft - sideExtent;
		this.panelRight = this.staffRight + sideExtent;
		this.panelTop = this.staffBottom - STAFF_HEIGHT - PANEL_PADDING_Y;
		this.panelBottom = this.staffBottom + PANEL_PADDING_Y + 1;

		// The raised flute (first person) sits right above the panel.
		EchoFluteFirstPersonLayout.setPanelTop(this.panelTop / (float) this.height);
	}

	private int lineY(int noteIndex) {
		return this.staffBottom - noteIndex * LINE_SPACING;
	}

	/** X center of a note slot. Slot 0 is the oldest note on the left. */
	private float slotX(int slot) {
		float slotWidth = (this.staffRight - this.staffLeft) / (float) MAX_NOTES;
		return this.staffLeft + slotWidth * (slot + 0.5F);
	}

	// ---- Input ------------------------------------------------------------------------------

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		int noteIndex = noteIndexForKey(keyCode);
		if (noteIndex >= 0) {
			// Ignore key repeat: holding a key plays the note once.
			if (!this.heldKeys[noteIndex]) {
				this.heldKeys[noteIndex] = true;
				playNote(NOTES[noteIndex]);
			}
			return true;
		}
		// ESC closes the screen through Screen#keyPressed.
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		int noteIndex = noteIndexForKey(keyCode);
		if (noteIndex >= 0) {
			this.heldKeys[noteIndex] = false;
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	private static int noteIndexForKey(int keyCode) {
		for (int i = 0; i < NOTE_KEYS.length; i++) {
			if (NOTE_KEYS[i] == keyCode) {
				return i;
			}
		}
		return -1;
	}

	private void playNote(EchoFluteNote note) {
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}

		// Played locally right away; other players get the sound from the server (see EchoFluteItem).
		player.level().playLocalSound(player.getX(), player.getEyeY(), player.getZ(),
				note.sound().value(), SoundSource.RECORDS, 1.0F, note.pitch(), false);

		EchoFluteParticles.spawn(player, this.hand, note);
		EchoFluteHandAnimation.onNotePlayed(note);

		// The server relays sound and particles to other nearby players.
		if (ClientPlayNetworking.canSend(ServerboundPlayEchoFluteNotePayload.TYPE)) {
			ClientPlayNetworking.send(new ServerboundPlayEchoFluteNotePayload(this.hand, note));
		}

		// New notes appear directly in their final slot; only older notes slide.
		float startX = slotX(Math.min(this.notes.size(), MAX_NOTES - 1));
		this.notes.addLast(new PlayedNote(note, Util.getMillis(), startX));
		while (this.notes.size() > MAX_NOTES) {
			this.notes.removeFirst();
		}
	}

	// ---- Lifecycle --------------------------------------------------------------------------

	@Override
	public void tick() {
		// Close if the flute is no longer in the hand that opened the screen.
		LocalPlayer player = this.minecraft.player;
		if (player == null || !player.getItemInHand(this.hand).is(ValorItems.ECHO_FLUTE)) {
			this.onClose();
		}
	}

	/** Ends the item-use state, which lowers the flute again (first person) and drops the arm pose (third person). */
	@Override
	public void removed() {
		LocalPlayer player = this.minecraft.player;
		if (player != null && EchoFluteHandAnimation.isPlaying(player)) {
			this.minecraft.gameMode.releaseUsingItem(player);
		}
	}

	/** The world keeps running behind the screen, also in singleplayer. */
	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- Rendering --------------------------------------------------------------------------

	/** No darkening, blur or menu background: the world stays fully visible. */
	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		long now = Util.getMillis();
		float deltaSeconds = Math.min((now - this.lastFrameMillis) / 1000.0F, 0.1F);
		this.lastFrameMillis = now;

		renderPanel(graphics);
		renderStaff(graphics);
		renderNotes(graphics, now, deltaSeconds);
	}

	/** Translucent panel with 2-pixel stepped corners and a faint light edge, Minecraft pixel style. */
	private void renderPanel(GuiGraphics graphics) {
		int l = this.panelLeft;
		int t = this.panelTop;
		int r = this.panelRight;
		int b = this.panelBottom;

		// Body as non-overlapping rows so the translucent color is uniform.
		graphics.fill(l + 2, t, r - 2, t + 1, PANEL_COLOR);
		graphics.fill(l + 1, t + 1, r - 1, t + 2, PANEL_COLOR);
		graphics.fill(l, t + 2, r, b - 2, PANEL_COLOR);
		graphics.fill(l + 1, b - 2, r - 1, b - 1, PANEL_COLOR);
		graphics.fill(l + 2, b - 1, r - 2, b, PANEL_COLOR);

		// Edge, following the stepped outline.
		graphics.fill(l + 2, t, r - 2, t + 1, PANEL_BORDER_COLOR);
		graphics.fill(l + 2, b - 1, r - 2, b, PANEL_BORDER_COLOR);
		graphics.fill(l, t + 2, l + 1, b - 2, PANEL_BORDER_COLOR);
		graphics.fill(r - 1, t + 2, r, b - 2, PANEL_BORDER_COLOR);
		graphics.fill(l + 1, t + 1, l + 2, t + 2, PANEL_BORDER_COLOR);
		graphics.fill(r - 2, t + 1, r - 1, t + 2, PANEL_BORDER_COLOR);
		graphics.fill(l + 1, b - 2, l + 2, b - 1, PANEL_BORDER_COLOR);
		graphics.fill(r - 2, b - 2, r - 1, b - 1, PANEL_BORDER_COLOR);
	}

	private void renderStaff(GuiGraphics graphics) {
		for (int i = 0; i < NOTES.length; i++) {
			int y = lineY(i);
			graphics.fill(this.staffLeft + 1, y + 1, this.staffRight + 1, y + 2, SHADOW_COLOR);
			graphics.fill(this.staffLeft, y, this.staffRight, y + 1, LINE_COLOR);

			// Key hint left of each line, in the note's color. Brighter while the key is held.
			int alpha = this.heldKeys[i] ? 0xFF : 0xB0;
			int labelColor = FastColor.ARGB32.color(alpha, NOTES[i].color());
			String label = NOTE_KEY_LABELS[i];
			int labelX = this.staffLeft - LABEL_GAP - this.font.width(label);
			graphics.drawString(this.font, label, labelX, y - this.font.lineHeight / 2, labelColor, true);
		}
	}

	private void renderNotes(GuiGraphics graphics, long now, float deltaSeconds) {
		float slideFactor = 1.0F - (float) Math.exp(-SLIDE_SPEED * deltaSeconds);
		int slot = 0;
		for (PlayedNote played : this.notes) {
			played.x = Mth.lerp(slideFactor, played.x, slotX(slot));
			float glow = glowStrength(now - played.playedAtMillis);
			renderNote(graphics, played.note, played.x, lineY(played.note.ordinal()), glow);
			slot++;
		}
	}

	/** 1 right after playing, fading out to 0 with an ease-out curve. */
	private static float glowStrength(long ageMillis) {
		if (ageMillis >= GLOW_DURATION_MS) {
			return 0.0F;
		}
		float remaining = 1.0F - ageMillis / (float) GLOW_DURATION_MS;
		return remaining * remaining;
	}

	/**
	 * Draws a simple quarter note centered on ({@code x}, {@code y}). Notes on the upper two lines
	 * get a downward stem on the left, like in regular notation.
	 */
	private void renderNote(GuiGraphics graphics, EchoFluteNote note, float x, int y, float glow) {
		boolean stemDown = note.ordinal() >= 3;
		int color = FastColor.ARGB32.opaque(note.color());

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0F);
		float scale = 1.0F + 0.12F * glow;
		graphics.pose().scale(scale, scale, 1.0F);

		if (glow > 0.0F) {
			// Soft halo in the note's color, drawn as two stacked translucent layers.
			int outer = FastColor.ARGB32.color(Math.round(glow * 0x38), note.color());
			int inner = FastColor.ARGB32.color(Math.round(glow * 0x50), note.color());
			graphics.fill(-7, -6, 8, 7, outer);
			graphics.fill(-5, -4, 6, 5, inner);
			// Briefly brighten the note itself towards white.
			color = FastColor.ARGB32.lerp(glow * 0.45F, color, 0xFFFFFFFF);
		}

		drawNoteShape(graphics, 1, 1, stemDown, SHADOW_COLOR);
		drawNoteShape(graphics, 0, 0, stemDown, color);

		graphics.pose().popPose();
	}

	/** 7×5 oval note head with a stem, offset by ({@code dx}, {@code dy}). */
	private static void drawNoteShape(GuiGraphics graphics, int dx, int dy, boolean stemDown, int color) {
		graphics.fill(dx - 2, dy - 2, dx + 3, dy - 1, color);
		graphics.fill(dx - 3, dy - 1, dx + 4, dy + 2, color);
		graphics.fill(dx - 2, dy + 2, dx + 3, dy + 3, color);
		if (stemDown) {
			graphics.fill(dx - 3, dy + 2, dx - 2, dy + 2 + STEM_LENGTH, color);
		} else {
			graphics.fill(dx + 3, dy - STEM_LENGTH, dx + 4, dy - 1, color);
		}
	}

	/** A note on the staff. {@code x} is animated towards the note's slot. */
	private static final class PlayedNote {
		private final EchoFluteNote note;
		private final long playedAtMillis;
		private float x;

		private PlayedNote(EchoFluteNote note, long playedAtMillis, float x) {
			this.note = note;
			this.playedAtMillis = playedAtMillis;
			this.x = x;
		}
	}
}
