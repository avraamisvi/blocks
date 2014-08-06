package dax.blocks.world;

import dax.blocks.collisions.AABB;
import dax.blocks.gui.ingame.GuiManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Random;

import dax.blocks.Coord2D;
import dax.blocks.Coord3D;
import dax.blocks.Game;
import dax.blocks.Particle;
import dax.blocks.block.Block;
import dax.blocks.movable.entity.PlayerEntity;
import dax.blocks.render.IRenderable;
import dax.blocks.settings.Settings;
import dax.blocks.world.chunk.Chunk;
import dax.blocks.world.chunk.ChunkProvider;

public class World implements IRenderable {

	public static final float GRAVITY = 0.06f;
	public static final int MAX_PARTICLES = 10000;

	private List<ScheduledUpdate> scheduledUpdates;
	private List<ScheduledUpdate> newlyScheduledUpdates;
	private List<IRenderable> renderables;
	private List<IRenderable> scheduledRenderablesRemoval;
	private List<IRenderable> scheduledRenderablesAdding;

	private Coord2D c2d;
	private PlayerEntity player;
	private ChunkProvider chunkProvider;
	private DataManager blockDataManager;
	
	public int size;
	public int sizeBlocks;

	public String name;

	private float[] rightMod = new float[3];
	private float[] upMod = new float[3];

	private Random rand = new Random();
	private int vertices;

	public int chunksDrawn;

	private boolean trees;

	public int getVertices() {
		return this.vertices;
	}

	public World(boolean trees, Game game, boolean load, String worldName) {
		this.name = worldName;
		
		this.player = new PlayerEntity(this, 0, 128, 0);

		this.renderables = new ArrayList<IRenderable>();
		this.renderables.add(this.player);

		this.chunkProvider = new ChunkProvider(this, load);

		this.c2d = new Coord2D(-1, -1);

		this.scheduledUpdates = new LinkedList<ScheduledUpdate>();
		this.newlyScheduledUpdates = new LinkedList<ScheduledUpdate>();
		this.scheduledRenderablesAdding = new LinkedList<IRenderable>();
		this.scheduledRenderablesRemoval = new LinkedList<IRenderable>();

		this.chunkProvider.updateLoadedChunksInRadius((int) this.player.getPosX(),
				(int) this.player.getPosZ(), Settings.getInstance().drawDistance.getValue());
	}

	public Coord2D getCoord2D(int x, int y) {
		this.c2d.set(x, y);
		return this.c2d;
	}
	

	public PlayerEntity getPlayer() {
		return this.player;
	}
	
	public ChunkProvider getChunkProvider() {
		return this.chunkProvider;
	}
	
	public DataManager getDataManager() {
		return this.blockDataManager;
	}
	
	public void createDataManager(File file) {
		try {
			this.blockDataManager = new DataManager(file);
		} catch (IOException e) {
			Logger.getGlobal().warning("Can't create data file!");
		}
	}
	
	public void registerNewRenderable(IRenderable renderable) {
		if(!this.scheduledRenderablesAdding.contains(renderable))
			this.scheduledRenderablesAdding.add(renderable);
	}
	
	public void registerNewRenderableRemoval(IRenderable renderable) {
		if(!this.scheduledRenderablesRemoval.contains(renderable) && this.renderables.contains(renderable))
			this.scheduledRenderablesRemoval.add(renderable);
	}

	
	public void spawnParticleWithRandomDirectionFast(float x, float y, float z,
			float vel, float velFuzziness) {

		float velhalf = vel * 0.5f;

		float velX = velhalf - this.rand.nextFloat() * vel - this.rand.nextFloat()
				* velFuzziness;
		float velY = velhalf - this.rand.nextFloat() * vel - this.rand.nextFloat()
				* velFuzziness;
		float velZ = velhalf - this.rand.nextFloat() * vel - this.rand.nextFloat()
				* velFuzziness;

		Particle p = new Particle(this, x, y, z, velX, velY, velZ, 100, this.rand.nextFloat(), 
				this.rand.nextFloat(), this.rand.nextFloat());
		this.registerNewRenderable(p);

	}

	public void spawnParticle(float x, float y, float z) {
		float velocity = 2.0f + this.rand.nextFloat() * 0.15f;
		float heading = 180 - this.rand.nextFloat() * 360f;
		float tilt = 180 - this.rand.nextFloat() * 360f;

		float velY = (float) (Math.cos(tilt) * velocity);
		float mult = (float) (Math.sin(tilt));

		float velX = (float) (Math.cos(heading) * velocity * mult);
		float velZ = (float) (Math.sin(heading) * velocity * mult);

		Particle p = new Particle(this, x, y, z, velX, velY, velZ,
				50 + this.rand.nextInt(20), this.rand.nextFloat(), this.rand.nextFloat(),
				this.rand.nextFloat());
		this.registerNewRenderable(p);

	}

	int removedParticles = 0;

	public boolean isOccluder(int x, int y, int z) {
		int id = this.getBlock(x, y, z);
		return id > 0 ? Block.getBlock((byte) id).isOccluder() : false;
	}

	public void updateNeighbours(int x, int y, int z) {
		this.scheduleUpdate(x + 1, y, z, 1);
		this.scheduleUpdate(x - 1, y, z, 1);
		this.scheduleUpdate(x, y + 1, z, 1);
		this.scheduleUpdate(x, y - 1, z, 1);
		this.scheduleUpdate(x, y, z + 1, 1);
		this.scheduleUpdate(x, y, z - 1, 1);
	}

	public void updateBlock(int x, int y, int z) {
		int id = this.getBlock(x, y, z);
		if(id > 0) {
			Block.getBlock(id).onTick(x, y, z, this);
		}

	}

	public void scheduleUpdate(int x, int y, int z, int ticks) {
		this.newlyScheduledUpdates.add(new ScheduledUpdate(x, y, z, ticks));
	}

	public void menuUpdate() {
		this.chunkProvider.updateLoadedChunksInRadius(
				((int) Math.floor(this.player.getPosX())) >> 4,
				((int) Math.floor(this.player.getPosZ())) >> 4,
				Settings.getInstance().drawDistance.getValue() + 1);
	}

	public void setChunkDirty(int x, int y, int z) {
		Coord2D coord = this.getCoord2D(x, z);

		if(this.chunkProvider.isChunkLoaded(coord)) {
			this.chunkProvider.getChunk(coord).setDirty(y);
		}
	}

	public int getBlock(int x, int y, int z) {
		int icx = x & 15;
		int icz = z & 15;

		int cx = x >> 4;
		int cz = z >> 4;

		Coord2D coord = this.getCoord2D(cx, cz);

		Chunk c = this.chunkProvider.getChunk(coord);
		return c != null ? c.getBlock(icx, y, icz) : 0;
	}

	public void setBlock(int x, int y, int z, int id, boolean artificial, boolean notify) {
		int icx = x & 15;
		int icz = z & 15;

		int cx = x >> 4;
		int cz = z >> 4;

		Coord2D coord = this.getCoord2D(cx, cz);

		if(this.chunkProvider.isChunkLoaded(coord)) {
			Chunk c = this.chunkProvider.getChunk(coord);
			
			if(notify) {
				Block before = Block.getBlock(getBlock(x, y, z));
				
				if(before != null) { 
					before.onRemoved(x, y, z, this);
				}
			}
			
			c.setBlock(icx, y, icz, id, true);
			c.changed = artificial;
			if(artificial) {
				this.scheduleUpdate(x, y, z, 1);
				this.updateNeighbours(x, y, z);
			}
		}
		
		if(notify) {
			if(id != 0) Block.getBlock(id).onPlaced(x, y, z, this);
		}
	}

	public void setBlockNoRebuild(int x, int y, int z, byte id) {
		int icx = x & 15;
		int icz = z & 15;

		int cx = x >> 4;
		int cz = z >> 4;

		Coord2D coord = this.getCoord2D(cx, cz);

		if(this.chunkProvider.isChunkLoaded(coord)) {
			this.chunkProvider.getChunk(coord).setBlock(icx, y, icz, id, false);
		}
	}

	public float[] clipMovement(AABB bb, float xm, float ym, float zm) {

		float _x0 = bb.x0;
		float _y0 = bb.y0;
		float _z0 = bb.z0;
		float _x1 = bb.x1;
		float _y1 = bb.y1;
		float _z1 = bb.z1;

		if(xm < 0.0F) {
			_x0 += xm;
		}

		if(xm > 0.0F) {
			_x1 += xm;
		}

		if(ym < 0.0F) {
			_y0 += ym;
		}

		if(ym > 0.0F) {
			_y1 += ym;
		}

		if(zm < 0.0F) {
			_z0 += zm;
		}

		if(zm > 0.0F) {
			_z1 += zm;
		}

		int x0 = (int) (_x0 - 1.0F);
		int x1 = (int) (_x1 + 1.0F);
		int y0 = (int) (_y0 - 1.0F);
		int y1 = (int) (_y1 + 1.0F);
		int z0 = (int) (_z0 - 1.0F);
		int z1 = (int) (_z1 + 1.0F);

		for(int x = x0; x < x1; ++x) {
			for(int y = y0; y < y1; ++y) {
				for(int z = z0; z < z1; ++z) {
					int blockId = getBlock(x, y, z);
					if(blockId > 0) {
						Block block = Block.getBlock(blockId);
						if(block.isCollidable()) {
							AABB blockBB = block.getOffsetAABB(x, y, z);
							xm = blockBB.clipXCollide(bb, xm);
						}
					}
				}
			}
		}
		bb.move(xm, 0.0F, 0.0F);

		for(int x = x0; x < x1; ++x) {
			for(int y = y0; y < y1; ++y) {
				for(int z = z0; z < z1; ++z) {
					int blockId = getBlock(x, y, z);
					if(blockId > 0) {
						Block block = Block.getBlock(blockId);
						if(block.isCollidable()) {
							AABB blockBB = block.getOffsetAABB(x, y, z);
							ym = blockBB.clipYCollide(bb, ym);
						}
					}
				}
			}
		}
		bb.move(0.0F, ym, 0.0F);

		for(int x = x0; x < x1; ++x) {
			for(int y = y0; y < y1; ++y) {
				for(int z = z0; z < z1; ++z) {
					int blockId = getBlock(x, y, z);
					if(blockId > 0) {
						Block block = Block.getBlock(blockId);
						if(block.isCollidable()) {
							AABB blockBB = block.getOffsetAABB(x, y, z);
							zm = blockBB.clipZCollide(bb, zm);
						}
					}
				}
			}
		}
		bb.move(0.0F, 0.0F, zm);

		return new float[] { xm, ym, zm };
	}

	public void setAllChunksDirty() {
		for(Chunk c : this.chunkProvider.getAllLoadedChunks()) {
			c.setAllDirty();
		}
	}

	public void deleteAllDisplayLists() {
		for(Chunk c : this.chunkProvider.getAllLoadedChunks()) {
			c.deleteAllRenderChunks();
		}
	}

	public void saveAllChunks() {
		this.chunkProvider.loader.saveAll();
	}

	public List<IRenderable> getRenderables() {
		return this.renderables;
	}

	public void setData(int x, int y, int z, int key, String value) {
		Map<Integer, DataValue> coordData = this.blockDataManager.getValuesForCoord(
				x, y, z);
		if(coordData.get(key) != null) {
			coordData.get(key).setData(value);
		} else {
			coordData.put(key, new DataValue(value));
		}
	}

	public String getDataString(int x, int y, int z, int key) {
		if(this.containsData(x, y, z, key))
			return this.blockDataManager.getValuesForCoord(x, y, z).get(key)
					.getDataString();

		return null;
	}

	public int getDataInt(int x, int y, int z, int key) {
		if(this.containsData(x, y, z, key))
			return this.blockDataManager.getValuesForCoord(x, y, z).get(key)
					.getDataInt();

		return 0;
	}

	public float getDataFloat(int x, int y, int z, int key) {
		if(this.containsData(x, y, z, key))
			return this.blockDataManager.getValuesForCoord(x, y, z).get(key)
					.getDataFloat();

		return 0;
	}

	public boolean getDataBoolean(int x, int y, int z, int key) {
		if(this.containsData(x, y, z, key))
			return this.blockDataManager.getValuesForCoord(x, y, z).get(key)
					.getDataBoolean();

		return false;
	}

	public boolean containsData(int x, int y, int z, int key) {
		if(!this.blockDataManager.containsData(x, y, z))
			return false;
		
		return (this.blockDataManager.getValuesForCoord(x, y, z).get(key) != null);
	}
	
	public void removeData(int x, int y, int z) {
		this.blockDataManager.getValuesForCoord(x, y, z).clear();
	}
	
	@Override
	public void onTick() {	
		GuiManager.getInstance().onTick();
		
		for(IRenderable r : this.renderables) {
			r.onTick();
		}
		
		for(Iterator<IRenderable> it = this.scheduledRenderablesAdding.iterator(); it
				.hasNext();) {
			this.renderables.add(it.next());
			it.remove();
		}
		
		for(Iterator<IRenderable> it = this.scheduledRenderablesRemoval.iterator(); it
				.hasNext();) {
			this.renderables.remove(it.next());
			it.remove();
		}
		
		for(Entry<Coord3D, Block> b : Block.tickingBlocks.entrySet()) {
			if(b.getValue().isRequiringTick())
				b.getValue().onTick(b.getKey().x, b.getKey().y, b.getKey().z, this);
		}

		
		for(Iterator<ScheduledUpdate> it = newlyScheduledUpdates.iterator(); it
				.hasNext();) {
			scheduledUpdates.add(it.next());
			it.remove();
		}

		Iterator<ScheduledUpdate> updateIterator = scheduledUpdates.iterator();
		while(updateIterator.hasNext()) {
			ScheduledUpdate s = updateIterator.next();
			if(s.ticks > 0) {
				s.ticks--;
			} else {
				this.updateBlock(s.x, s.y, s.z);
				updateIterator.remove();
			}
		}

		this.chunkProvider.updateLoadedChunksInRadius(
				((int) Math.floor(this.player.getPosX())) >> 4,
				((int) Math.floor(this.player.getPosZ())) >> 4,
				Settings.getInstance().drawDistance.getValue() + 1);

	}

	@Override
	public void onRenderTick(float partialTickTime) {
		for(IRenderable r : this.renderables) {
			r.onRenderTick(partialTickTime);
		}
		
		for(Entry<Coord3D, Block> b : Block.tickingBlocks.entrySet()) {
			if(b.getValue().isRequiringRenderTick())
				b.getValue().onRenderTick(partialTickTime, b.getKey().x, b.getKey().y, b.getKey().z, this);	
		}
		
		GuiManager.getInstance().onRenderTick(partialTickTime);
	}

	@Override
	public void renderWorld(float partialTickTime) {
		for(IRenderable r : this.renderables) {
			r.renderWorld(partialTickTime);
		}
		
		GuiManager.getInstance().renderWorld(partialTickTime);
	}

	@Override
	public void renderGui(float partialTickTime) {
		for(IRenderable r : this.renderables) {
			r.renderGui(partialTickTime);
		}
		
		GuiManager.getInstance().renderGui(partialTickTime);
	}
}
