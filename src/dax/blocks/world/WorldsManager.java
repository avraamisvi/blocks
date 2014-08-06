package dax.blocks.world;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WorldsManager {
	private static WorldsManager _instance;
	public static WorldsManager getInstance() {
		if(_instance == null) {
			_instance = new WorldsManager();
		}
		
		return _instance;
	}
	
	public static final String SAVES_DIR = "saves";

	private File savesDir;
	private WorldsManager() {
	}
	
	public void load() {
		savesDir = new File(SAVES_DIR);
		if(!savesDir.exists()) {
			savesDir.mkdir();
		}
	}

	public List<File> getWorldsDirs() {
		ArrayList<File> worlds = new ArrayList<File>();

		String[] names = savesDir.list();
			for (String name : names) {
				File d = new File(SAVES_DIR, name);
				if (d.isDirectory()) {
					worlds.add(d);
				}
			}
		

		return worlds;
	}

	public List<WorldInfo> getWorldsInfo() {
		ArrayList<WorldInfo> worlds = new ArrayList<WorldInfo>();

		List<File> dirs = getWorldsDirs();
		for (File d : dirs) {
			try {
				worlds.add(WorldInfo
						.constructFromFile(new File(d, "world.txt")));
			} catch (FileNotFoundException e) {
				Logger.getGlobal().info("World definition file doesn't exist");
			}
		}

		return worlds;
	}

	public WorldInfo getWorld(String name) {
		List<WorldInfo> worlds = getWorldsInfo();
		for (WorldInfo i : worlds) {
			if (i.getWorldName().equalsIgnoreCase(name)) {
				return i;
			}
		}

		return null;
	}


}
