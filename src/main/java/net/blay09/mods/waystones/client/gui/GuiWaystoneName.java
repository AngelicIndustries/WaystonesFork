package net.blay09.mods.waystones.client.gui;

import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.network.message.MessageRenameWaystone;
import net.blay09.mods.waystones.network.message.MessageWaystoneName;
import net.blay09.mods.waystones.util.BlockPos;
import net.blay09.mods.waystones.util.NameGenerator;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.config.GuiCheckBox;

public class GuiWaystoneName extends GuiScreen {

    private final TileWaystone tileWaystone;
    private GuiTextField textField;
    private GuiButton btnDone;
    private GuiButton btnCancel;
    private GuiCheckBox chkGlobal;
    private final boolean renaming;
    GuiScreen parentScreen;

    public GuiWaystoneName(TileWaystone tileWaystone, boolean renaming, GuiScreen parentScreen) {
        this.tileWaystone = tileWaystone;
        this.renaming = renaming;
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        String oldText = tileWaystone.getWaystoneName();
        boolean suggested = false;
        if (textField != null) {
            oldText = textField.getText();
        } else if (oldText.isEmpty() && !renaming
            && Waystones.getConfig().unnamedWaystoneNaming != WaystoneConfig.UnnamedWaystoneNaming.PROMPT) {
                oldText = NameGenerator.generate(
                    mc.theWorld,
                    tileWaystone.xCoord,
                    tileWaystone.yCoord,
                    tileWaystone.zCoord,
                    mc.theWorld.rand,
                    name -> WaystoneManager.isNameTaken(mc.thePlayer, name));
                suggested = true;
            }
        textField = new GuiTextField(fontRendererObj, width / 2 - 100, height / 2 - 20, 200, 20);
        textField.setText(oldText);
        textField.setFocused(true);
        if (suggested) {
            // Selected, so typing replaces the suggestion and Done keeps it
            textField.setCursorPositionEnd();
            textField.setSelectionPos(0);
        }
        chkGlobal = new GuiCheckBox(
            1,
            width / 2 - 100,
            height / 2 + 5,
            " " + I18n.format("gui.waystones:editWaystone.isGlobal"),
            WaystoneManager.getServerWaystone(tileWaystone.getWaystoneName()) != null);
        chkGlobal.visible = Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode && !renaming;

        btnCancel = new GuiButton(2, width / 2 - 102, height / 2 + 25, 100, 20, I18n.format("gui.cancel"));
        buttonList.add(btnCancel);
        btnDone = new GuiButton(0, width / 2 + 2, height / 2 + 25, 100, 20, I18n.format("gui.done"));
        buttonList.add(btnDone);

        buttonList.add(chkGlobal);
        updateDoneButton();

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnCancel) {
            if (renaming) {
                mc.displayGuiScreen(parentScreen);
            } else {
                mc.displayGuiScreen(null);
            }
        } else if (button == btnDone) {
            if (!renaming) {
                NetworkHandler.channel.sendToServer(
                    new MessageWaystoneName(
                        new BlockPos(tileWaystone),
                        textField.getText()
                            .trim(),
                        chkGlobal.isChecked()));
                mc.displayGuiScreen(null);
            } else {
                NetworkHandler.channel.sendToServer(
                    new MessageRenameWaystone(
                        new WaystoneEntry(tileWaystone),
                        textField.getText()
                            .trim()));
                mc.displayGuiScreen(parentScreen);
            }
        } else if (button == chkGlobal) {
            // Re-check the duplicate name whenever the checkbox is toggled
            updateDoneButton();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        textField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN) {
            if (btnDone.enabled) {
                actionPerformed(btnDone);
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
        textField.textboxKeyTyped(typedChar, keyCode);
        updateDoneButton();
    }

    private void updateDoneButton() {
        String newName = textField.getText()
            .trim();
        btnDone.enabled = !newName.isEmpty() && !isNameDuplicate(newName, chkGlobal.isChecked());
    }

    @Override
    public void updateScreen() {
        textField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawWorldBackground(0);
        super.drawScreen(mouseX, mouseY, partialTicks);

        fontRendererObj.drawString(
            I18n.format("gui.waystones:editWaystone.enterName"),
            width / 2 - 100,
            height / 2 - 35,
            0xFFFFFF);
        textField.drawTextBox();

        String newName = textField.getText()
            .trim();
        if (isNameDuplicate(newName, chkGlobal.isChecked())) {
            fontRendererObj.drawString(
                EnumChatFormatting.RED + I18n.format("gui.waystones:nameTaken"),
                width / 2 - 100,
                height / 2 + 50,
                0xFF0000);
        }
    }

    private boolean isNameDuplicate(String name, boolean global) {
        if (global) {
            return WaystoneManager.getServerWaystone(name) != null && !name.equals(tileWaystone.getWaystoneName());
        } else {
            for (WaystoneEntry entry : WaystoneManager.getKnownWaystones()) {
                if (!entry.isGlobal() && entry.getName()
                    .equals(name) && !name.equals(tileWaystone.getWaystoneName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return WaystoneConfig.menusPauseGame;
    }
}
