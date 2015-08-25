package cz.dat.oots.gui;

import org.newdawn.slick.Font;

import cz.dat.oots.settings.SettingsObject;

public class GuiObjectSettingsBooleanButton extends GuiObjectButton {

	private SettingsObject<Boolean> object;

	public GuiObjectSettingsBooleanButton(int x1, int y1, int x2, int y2,
			Font font, int id, GuiScreen parent, SettingsObject<Boolean> object) {
		super(x1, y1, x2, y2, font, object.getRepresentation(), id, parent);
		this.object = object;
	}

	@Override
	protected void onClick() {
		this.object.setValue(!this.object.getValue());
		this.text = this.object.getRepresentation();
	}
}
