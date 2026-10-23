package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Полноэкранное меню апгрейда: клик по слоту вкладывает предмет. */
public class UpgradeScreen extends Screen {
    public UpgradeScreen() {
        super(Component.literal("Апгрейд"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        UpgradeOverlay.render(g, width, height, mouseX, mouseY, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) return super.mouseClicked(event, doubled);
        int hit = UpgradeOverlay.hitSlot(event.x(), event.y());
        if (hit == -1) {
            StreamerActionClient.send("upgrade:go");
            return true;
        }
        if (hit >= 0) {
            StreamerActionClient.send("upgrade:slot:" + hit);
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        int idx = event.key() - GLFW.GLFW_KEY_1;
        if (idx >= 0 && idx <= 8 && UpgradeOverlay.canPick()) {
            StreamerActionClient.send("upgrade:slot:" + idx);
            return true;
        }
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE)
                && UpgradeOverlay.canPick()) {
            StreamerActionClient.send("upgrade:go");
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
