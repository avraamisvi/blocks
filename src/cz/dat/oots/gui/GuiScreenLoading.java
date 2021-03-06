package cz.dat.oots.gui;

import cz.dat.oots.Game;
import org.lwjgl.opengl.Display;

public class GuiScreenLoading extends GuiScreen {

    private int width = 400;
    private int height = 30;
    private int overflow = 8;
    private GuiObjectTitleBar titleBar;

    public GuiScreenLoading(Game game, String text) {
        super(game);
        /*this.objects.add(new GuiObjectRectangle((game.s().windowWidth
				.getValue() - width - overflow) / 2, (game.s().windowHeight
				.getValue() - height - overflow) / 2, (game.s().windowWidth
				.getValue() + width + overflow) / 2, (game.s().windowHeight
				.getValue() + height + overflow) / 2, 0xA0000000));*/

        this.titleBar = new GuiObjectTitleBar(
                (game.s().resolution.width() - width) / 2,
                (game.s().resolution.height() - height) / 2,
                (game.s().resolution.width() + width) / 2,
                ((game.s().resolution.height() - height) / 2) + 30, this.f,
                text);
    }

    public GuiScreenLoading(Game game) {
        this(game, "Loading...");
    }

    public void update(String text) {
        this.titleBar.setText(text);
        this.game.render(0);
        Display.update();
    }

    @Override
    public void buttonPress(GuiObjectButton button) {

    }

    @Override
    public void sliderUpdate(GuiObjectSlider slider) {

    }

    @Override
    public void render() {
        super.render();
        this.titleBar.render();
    }

    @Override
    public void buttonChanged(GuiObjectChangingButton button, int line) {

    }

    @Override
    public void onClosing() {
    }

    @Override
    public void onOpening() {
    }
}
