package net.blay09.mods.waystones.util;

import java.nio.FloatBuffer;

import net.blay09.mods.waystones.Waystones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ClientUtil {

    /**
     * Draws a texture scaled to a target size.
     *
     * @param texture      ResourceLocation of the texture
     * @param x            Screen X
     * @param y            Screen Y
     * @param baseWidth    Actual texture width (px)
     * @param baseHeight   Actual texture height (px)
     * @param targetWidth  Desired on-screen width
     * @param targetHeight Desired on-screen height
     */
    public static void drawScaledTexture(ResourceLocation texture, int x, int y, int baseWidth, int baseHeight,
        int targetWidth, int targetHeight) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        Gui.func_146110_a(x, y, 0.0F, 0.0F, targetWidth, targetHeight, (float) baseWidth, (float) baseHeight);
    }

    public static void drawTextFieldHint(GuiTextField field, String hint, FontRenderer fontRenderer) {
        if (!field.isFocused() && field.getText()
            .isEmpty()) {
            hint = fontRenderer.trimStringToWidth("Search Waystones", field.width);
            int textX = field.getEnableBackgroundDrawing() ? field.xPosition + 4 : field.xPosition;
            int textY = field.getEnableBackgroundDrawing() ? field.yPosition + (field.height - 8) / 2 : field.yPosition;
            fontRenderer.drawStringWithShadow(hint, textX, textY, 0x292827);
        }
    }

    public static void drawSolidRect(Tessellator tessellator, double x, double y, double width, double height,
        int colorARGB) {

        // Extract ARGB
        float a = (colorARGB >> 24 & 255) / 255.0F;
        float r = (colorARGB >> 16 & 255) / 255.0F;
        float g = (colorARGB >> 8 & 255) / 255.0F;
        float b = (colorARGB & 255) / 255.0F;

        double left = x;
        double right = x + width;
        double top = y;
        double bottom = y + height;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);

        tessellator.startDrawingQuads();
        tessellator.addVertex(left, bottom, 0.0D);
        tessellator.addVertex(right, bottom, 0.0D);
        tessellator.addVertex(right, top, 0.0D);
        tessellator.addVertex(left, top, 0.0D);
        tessellator.draw();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static int getMouseDwheel() {
        int dwheel = Mouse.getEventDWheel();
        if (dwheel != 0) {
            // Source:
            // https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/master/src/main/java/appeng/client/gui/AEBaseGui.java
            if (!Waystones.hasLwjgl3) {
                // LWJGL2 reports different scroll values for every platform, 120 for one tick on Windows.
                // LWJGL3 reports the delta in exact scroll ticks.
                // Round away from zero to avoid dropping small scroll events
                if (dwheel > 0) {
                    dwheel = (int) MiscUtil.ceilDiv(dwheel, 120);
                } else {
                    dwheel = -(int) MiscUtil.ceilDiv(-dwheel, 120);
                }
            }
        }
        return dwheel;
    }

    public static int backupGLColor() {
        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, colorBuffer);
        int r = (int) (colorBuffer.get(0) * 255);
        int g = (int) (colorBuffer.get(1) * 255);
        int b = (int) (colorBuffer.get(2) * 255);
        int a = (int) (colorBuffer.get(3) * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void restoreGLColor(int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        GL11.glColor4f(r, g, b, a);
    }

    public static int dimColor(int color) {
        int a = (color >> 24) & 0xFF;
        int r = ((color >> 16) & 0xFF) / 2;
        int g = ((color >> 8) & 0xFF) / 2;
        int b = (color & 0xFF) / 2;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    void printGlColor() {
        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, colorBuffer);
        Waystones.LOG.info(
            "Current GL color: {}, {}, {}, {}",
            colorBuffer.get(0),
            colorBuffer.get(1),
            colorBuffer.get(2),
            colorBuffer.get(3));
    }

    public static boolean mouseOverArea(int mouseX, int mouseY, int areaX, int areaY, int areaW, int areaH) {
        return mouseX >= areaX && mouseX <= areaX + areaW && mouseY >= areaY && mouseY <= areaY + areaH;
    }

    // Same sound vanilla GuiButtons play, for click areas that aren't real buttons
    public static void playButtonSound() {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public static void drawBorderedBox(double x, double y, double width, double height, double borderSize,
        int backgroundColor, int borderColor) {
        Tessellator tessellator = Tessellator.instance;
        // Background
        ClientUtil.drawSolidRect(tessellator, x, y, width, height, backgroundColor);

        // Border
        // Left
        ClientUtil.drawSolidRect(
            tessellator,
            x - borderSize,
            y - borderSize,
            borderSize,
            height + borderSize * 2,
            borderColor);
        // Right
        ClientUtil
            .drawSolidRect(tessellator, x + width, y - borderSize, borderSize, height + borderSize * 2, borderColor);
        // Top
        ClientUtil.drawSolidRect(tessellator, x, y - borderSize, width, borderSize, borderColor);
        // Bottom
        ClientUtil.drawSolidRect(tessellator, x, y + height, width, borderSize, borderColor);
    }
}
