package cz.dat.oots.world;

import cz.dat.oots.Game;
import cz.dat.oots.Particle;
import cz.dat.oots.block.Block;
import cz.dat.oots.collisions.AABB;
import cz.dat.oots.data.DataManager;
import cz.dat.oots.data.IBlockDataManager;
import cz.dat.oots.data.IDataObject;
import cz.dat.oots.data.IItemDataManager;
import cz.dat.oots.gui.ingame.GuiManager;
import cz.dat.oots.item.Item;
import cz.dat.oots.movable.entity.PlayerEntity;
import cz.dat.oots.render.ITickListener;
import cz.dat.oots.render.RenderEngine;
import cz.dat.oots.util.Coord2D;
import cz.dat.oots.util.Coord3D;
import cz.dat.oots.util.GameMath;
import cz.dat.oots.world.chunk.Chunk;
import cz.dat.oots.world.chunk.ChunkProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class World implements ITickListener {

	public static final float GRAVITY = 0.06f;
	public static final int MAX_PARTICLES = 100000;
	public int size;
	public int sizeBlocks;
	public String name;
	public int chunksDrawn;
	private Map<Coord3D, ScheduledUpdate> scheduledUpdates;
	private Map<Coord3D, ScheduledUpdate> newlyScheduledUpdates;
	private List<ITickListener> tickListeners;
	private List<ITickListener> scheduledTickListenersRemoval;
	private List<ITickListener> scheduledTickListenersAdding;
	private Coord2D c2d;
	private PlayerEntity player;
	private ChunkProvider chunkProvider;
	private IBlockDataManager blockDataManager;
	private IItemDataManager itemDataManager;
	private IDRegister idRegister;
	private RenderEngine renderEngine;
	private ParticleEngine particleEngine;
	private Game game;
	private GuiManager gui;
	private Random rand = new Random();
	private int vertices;

	public World(Game game, String worldName, RenderEngine e) {
		this.gui = new GuiManager(game);
		this.name = worldName;
		this.game = game;
		this.renderEngine = e;
		this.particleEngine = new ParticleEngine(this);
		e.setWorld(this);

		this.idRegister = new IDRegister(this);
		this.idRegister.registerDefaultBlocks();
		this.idRegister.registerDefaultItems();

		this.player = new PlayerEntity(this, 0, 128, 0);
		game.getOverlayManager().addOverlay(this.player);

		this.chunkProvider = new ChunkProvider(this, true);

		this.c2d = new Coord2D(-1, -1);

		this.scheduledUpdates = new HashMap<Coord3D, ScheduledUpdate>();
		this.newlyScheduledUpdates = new HashMap<Coord3D, ScheduledUpdate>();
		this.tickListeners = new LinkedList<ITickListener>();
		this.scheduledTickListenersAdding = new LinkedList<ITickListener>();
		this.scheduledTickListenersRemoval = new LinkedList<ITickListener>();

		this.chunkProvider.updateLoadedChunksInRadius(
				(int) this.player.getPosX(), (int) this.player.getPosZ(),
				game.getSettings().drawDistance.getValue());
	}

	public int getVertices() {
		return this.vertices;
	}

	public RenderEngine getRenderEngine() {
		return this.renderEngine;
	}

	public IDRegister getRegister() {
		return this.idRegister;
	}

	public Block getBlockObject(int x, int y, int z) {
		return this.idRegister.getBlock(this.getBlock(x, y, z));
	}

	public Block getBlockObject(int id) {
		return this.idRegister.getBlock(id);
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

	public IBlockDataManager getBlockDataManager() {
		return this.blockDataManager;
	}

	public IItemDataManager getItemDataManager() {
		return this.itemDataManager;
	}

	public void createDataManagers(File blockDataFile, File itemDataFile) {
		try {
			DataManager n = new DataManager(blockDataFile, itemDataFile, this);
			this.itemDataManager = n;
			this.blockDataManager = n;
		} catch(IOException e) {
			Logger.getGlobal().warning("Can't create data file!");
		}
	}

	public void spawnParticleWithRandomDirectionFast(float x, float y, float z,
			float vel, float velFuzziness) {

		float velhalf = vel * 0.5f;

		float velX = velhalf - this.rand.nextFloat() * vel
				- this.rand.nextFloat() * velFuzziness;
		float velY = velhalf - this.rand.nextFloat() * vel
				- this.rand.nextFloat() * velFuzziness;
		float velZ = velhalf - this.rand.nextFloat() * vel
				- this.rand.nextFloat() * velFuzziness;
		Particle p = new Particle(this, x, y, z, velX, velY, velZ,
				(int) (50 + 50 * rand.nextFloat()), rand.nextFloat(),
				rand.nextFloat(), rand.nextFloat());
		this.particleEngine.addParticle(p);
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
				50 + rand.nextInt(20), rand.nextFloat(), rand.nextFloat(),
				rand.nextFloat());
		this.particleEngine.addParticle(p);
	}

	public void spawnParticleRepelled(float x, float y, float z, float rx,
			float ry, float rz, float rdist) {
		float velocity = 2.0f + this.rand.nextFloat() * 0.15f;

		float deltaX = rx - x;
		float deltaY = ry - y;
		float deltaZ = rz - z;

		float heading = (float) (Math.atan2(deltaX, deltaZ) * 180F / Math.PI);
		float tilt = (float) (Math.atan2(deltaY, deltaX) * 180F / Math.PI);

		// System.out.println("Particle coords: " + x + " " + y + " " + z +
		// " center coords: " + rx + " " + ry + " " + rz + " h: " + heading +
		// " t:" + tilt);

		float velY = (float) (Math.cos(tilt) * velocity);
		float mult = (float) (Math.sin(tilt));

		float velX = (float) (Math.cos(heading) * velocity * mult);
		float velZ = (float) (Math.sin(heading) * velocity * mult);

		Particle p = new Particle(this, x, y, z, velX, velY, velZ,
				50 + rand.nextInt(20), rand.nextFloat(), rand.nextFloat(),
				rand.nextFloat());
		this.particleEngine.addParticle(p);
	}

	public void registerNewTickListener(ITickListener l) {
		this.scheduledTickListenersAdding.add(l);
	}

	public void removeTickListener(ITickListener l) {
		if(!this.scheduledTickListenersRemoval.contains(l)) {
			this.scheduledTickListenersRemoval.add(l);
		}
	}

	public boolean isOccluder(int x, int y, int z) {
		int id = this.getBlock(x, y, z);
		return id > 0 ? this.getBlockObject(id).isOccluder() : false;
	}

	public void updateNeighbours(int x, int y, int z) {
		this.neighbourUpdate(x + 1, y, z);
		this.neighbourUpdate(x - 1, y, z);
		this.neighbourUpdate(x, y + 1, z);
		this.neighbourUpdate(x, y - 1, z);
		this.neighbourUpdate(x, y, z + 1);
		this.neighbourUpdate(x, y, z - 1);
	}

	public void updateBlock(int x, int y, int z, int type) {
		int id = this.getBlock(x, y, z);
		if(id > 0) {
			this.getBlockObject(id).onUpdate(x, y, z, type, this);
		}

	}

	public void neighbourUpdate(int x, int y, int z) {
		int id = this.getBlock(x, y, z);
		if(id > 0) {
			this.getBlockObject(id).onNeighbourUpdate(x, y, z, this);
		}
	}

	public void scheduleUpdate(int x, int y, int z, int ticks, int type) {
		Coord3D pos = new Coord3D(x, y, z);

		ScheduledUpdate u;
		if((u = this.scheduledUpdates.get(pos)) != null) {
			if(u.type == type && u.ticks <= ticks) {
				return;
			}
		} else if((u = this.newlyScheduledUpdates.get(pos)) != null) {
			if(u.type == type && u.ticks <= ticks) {
				return;
			}
		}

		this.newlyScheduledUpdates.put(pos, new ScheduledUpdate(type, ticks));
	}

	public void menuUpdate() {
		this.chunkProvider.updateLoadedChunksInRadius(
				((int) Math.floor(this.player.getPosX())) >> 4,
				((int) Math.floor(this.player.getPosZ())) >> 4,
				this.game.getSettings().drawDistance.getValue() + 1);
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

	public BlockHitResult hitscanBlock(double x, double y, double z, double ex,
			double ey, double ez, int maxSteps) {
		
		double dx = ex - x;
		double dy = ey - y;
		double dz = ez - z;
		
		double stepX = Math.signum(dx);
		double stepY = Math.signum(dy);
		double stepZ = Math.signum(dz);
		
		double tDeltaX = Math.abs(1.0d / dx);
		double tMaxX;
		
		if (stepX >= 0) {
			tMaxX = tDeltaX * (1.0d - Math.abs(x % 1));
		} else {
			tMaxX = tDeltaX * (Math.abs(x % 1));
		}	


		double tDeltaY = Math.abs(1.0d / dy);
		double tMaxY;
		
		if (stepY >= 0) {
			tMaxY = tDeltaY * (1.0d - Math.abs(y % 1));
		} else {
			tMaxY = tDeltaY * (Math.abs(y % 1));
		}	

		double tDeltaZ = Math.abs(1.0d / dz);
		double tMaxZ;
		
		if (stepZ >= 0) {
			tMaxZ = tDeltaZ * (1.0d - Math.abs(z % 1));
		} else {
			tMaxZ = tDeltaZ * (Math.abs(z % 1));
		}	

		int id = 0;
		
		int lxi, lyi, lzi;
		
		int xi = (int) x;
		int yi = (int) y;
		int zi = (int) z;
		
		do {
			if(tMaxX < tMaxY) {
				if(tMaxX < tMaxZ) {
					x = x + stepX;
					tMaxX = tMaxX + tDeltaX;
				} else {
					z = z + stepZ;
					tMaxZ = tMaxZ + tDeltaZ;
				}
			} else {
				if(tMaxY < tMaxZ) {
					y = y + stepY;
					if (y < 0 || y > Chunk.CHUNK_HEIGHT) {
						return new BlockHitResult();
					}
					tMaxY = tMaxY + tDeltaY;
				} else {
					z = z + stepZ;
					tMaxZ = tMaxZ + tDeltaZ;
				}
			}
			maxSteps--;
			
			lxi = xi;
			lyi = yi;
			lzi = zi;
			
			xi = (int) x;
			yi = (int) y;
			zi = (int) z;
			
			if (maxSteps <= 0) {
				return new BlockHitResult();
			}
			
			id = getBlock(xi, yi, zi);
		} while(id == 0);
		return new BlockHitResult(id, xi, yi, zi, lxi, lyi, lzi);
	}
	
	public BlockHitResult hitscanBlock(double x, double y, double z, double heading, double tilt, int maxSteps) {
		double yChange = Math.cos((-tilt + 90) / 180 * Math.PI);
		double ymult = Math.sin((-tilt + 90) / 180 * Math.PI);

        double xChange = Math.cos((-heading + 90) / 180 * Math.PI) * ymult;
        double zChange = -Math.sin((-heading + 90) / 180 * Math.PI) * ymult;
        
        
        double ex = x + xChange;
        double ey = y + yChange;
        double ez = z + zChange;

		return hitscanBlock(x, y, z, ex, ey, ez, maxSteps);
	}

	public void setBlock(int x, int y, int z, int id, boolean artificial,
			boolean notify) {
		int icx = x & 15;
		int icz = z & 15;

		int cx = x >> 4;
		int cz = z >> 4;

		Coord2D coord = this.getCoord2D(cx, cz);

		if(this.chunkProvider.isChunkLoaded(coord)) {
			Chunk c = this.chunkProvider.getChunk(coord);

			Coord3D pos = new Coord3D(x, y, z);
			if(!scheduledUpdates.containsKey(pos)) {
				scheduledUpdates.remove(pos);
			}

			Block before = this.getBlockObject(x, y, z);
			if(before != null) {
				before.onRemoved(x, y, z, this);
			}

			c.setBlock(icx, y, icz, id, true);
			c.changed = artificial;

			if(id != 0 && notify) {
				Block placed = this.getBlockObject(id);
				placed.onPlaced(x, y, z, this);
				this.neighbourUpdate(x, y, z);
			}

			if(notify)
				this.updateNeighbours(x, y, z);
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

		float d_x0 = bb.x0;
		float d_y0 = bb.y0;
		float d_x1 = bb.x1;
		float d_y1 = bb.y1;

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

		int x0 = (int) _x0;
		int x1 = (int) _x1;
		int y0 = (int) _y0;
		int y1 = (int) _y1;
		int z0 = (int) _z0;
		int z1 = (int) _z1;

		if(x0 <= 0) {
			x0--;
		}

		if(y0 <= 0) {
			y0--;
		}

		if(z0 <= 0) {
			z0--;
		}

		if(x1 >= 0) {
			x1++;
		}

		if(y1 >= 0) {
			y1++;
		}

		if(z1 >= 0) {
			z1++;
		}

		int v0 = 0;
		int v1 = 0;
		int v2 = 0;

		// v0 = Math.abs((x0 - x1) * (y0 - y1) * (z0 - z1));

		if(GameMath.shouldCareAbout(ym)) {
			for(int x = x0; x < x1; ++x) {
				for(int y = y0; y < y1; ++y) {
					for(int z = z0; z < z1; ++z) {
						int blockId = getBlock(x, y, z);
						if(blockId > 0) {
							Block block = this.getBlockObject(blockId);
							if(block.isCollidable()) {
								AABB blockBB = block.getOffsetAABB(x, y, z);
								ym = blockBB.clipYCollide(bb, ym);
							}
						}
					}
				}
			}
			bb.move(0.0F, ym, 0.0F);
		}

		// ///

		_y0 = d_y0;
		_y1 = d_y1;

		if(ym < 0.0F) {
			_y0 += ym;
		}

		if(ym > 0.0F) {
			_y1 += ym;
		}

		y0 = (int) _y0;
		y1 = (int) _y1;

		if(y0 <= 0) {
			y0--;
		}

		if(y1 >= 0) {
			y1++;
		}

		// ///

		// v1 = Math.abs((x0 - x1) * (y0 - y1) * (z0 - z1));

		if(GameMath.shouldCareAbout(xm)) {
			for(int x = x0; x < x1; ++x) {
				for(int y = y0; y < y1; ++y) {
					for(int z = z0; z < z1; ++z) {
						int blockId = getBlock(x, y, z);
						if(blockId > 0) {
							Block block = this.getBlockObject(blockId);
							if(block.isCollidable()) {
								AABB blockBB = block.getOffsetAABB(x, y, z);
								xm = blockBB.clipXCollide(bb, xm);
							}
						}
					}
				}
			}
			bb.move(xm, 0.0F, 0.0F);
		}

		// ///

		_x0 = d_x0;
		_x1 = d_x1;

		if(xm < 0.0F) {
			_x0 += xm;
		}

		if(xm > 0.0F) {
			_x1 += xm;
		}

		x0 = (int) _x0;
		x1 = (int) _x1;

		if(x0 <= 0) {
			x0--;
		}

		if(x1 >= 0) {
			x1++;
		}

		// ///

		// v2 = Math.abs((x0 - x1) * (y0 - y1) * (z0 - z1));

		if(GameMath.shouldCareAbout(zm)) {
			for(int x = x0; x < x1; ++x) {
				for(int y = y0; y < y1; ++y) {
					for(int z = z0; z < z1; ++z) {
						int blockId = getBlock(x, y, z);
						if(blockId > 0) {
							Block block = this.getBlockObject(blockId);
							if(block.isCollidable()) {
								AABB blockBB = block.getOffsetAABB(x, y, z);
								zm = blockBB.clipZCollide(bb, zm);
							}
						}
					}
				}
			}
			bb.move(0.0F, 0.0F, zm);
		}

		// System.out.println(v0 + " " + v1 + " " + v2);

		return new float[] { xm, ym, zm };
	}

	public void setAllChunksDirty() {
		for(Chunk c : this.chunkProvider.getAllLoadedChunks()) {
			c.setAllDirty();
		}
	}

	public void deleteAllRenderChunks() {
		for(Chunk c : this.chunkProvider.getAllLoadedChunks()) {
			c.deleteAllRenderChunks();
		}
	}

	public void saveAllChunks() {
		this.chunkProvider.loader.saveAll(this.game);

		try {
			this.idRegister.saveIDs(IDRegister.dataFile);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	// .... DATA ....
	public IDataObject createData(int x, int y, int z, Block block) {
		if(this.blockDataManager != null) {
			if(block != null) {
				IDataObject obj = block.createDataObject();
				this.blockDataManager.addDataForCoord(x, y, z, obj);
				return obj;
			}
		}
		return null;

	}

	public IDataObject getData(int x, int y, int z) {
		if(this.blockDataManager != null) {
			return this.blockDataManager.getDataForCoord(x, y, z);
		} else {
			return null;
		}
	}

	public boolean hasData(int x, int y, int z) {
		if(this.blockDataManager != null) {
			return this.blockDataManager.containsData(x, y, z);
		} else {
			return false;
		}
	}

	public void removeData(int x, int y, int z) {
		if(this.hasData(x, y, z)) {
			this.blockDataManager.removeDataForCoord(x, y, z);
		}
	}

	public IDataObject createData(Item item, int itemIdent) {
		if(this.itemDataManager != null) {
			IDataObject obj = item.createDataObject();
			this.itemDataManager.addDataForIdentificator(itemIdent, obj);
			return obj;
		} else {
			return null;
		}
	}

	public IDataObject getData(int itemIdent) {
		if(this.itemDataManager != null) {
			return this.itemDataManager.getDataForIdentificator(itemIdent);
		} else {
			return null;
		}
	}

	public boolean hasData(int itemIdent) {
		if(this.itemDataManager != null) {
			return this.itemDataManager.containsData(itemIdent);
		} else {
			return false;
		}
	}

	public void removeData(int itemIdent) {
		if(this.hasData(itemIdent)) {
			this.itemDataManager.removeDataForIdentificator(itemIdent);
		}
	}

	// .... OVERRIDE METHODS ....
	@Override
	public void onTick() {
		this.gui.onTick();
		this.player.onTick();

		for(ITickListener r : this.tickListeners) {
			r.onTick();
		}

		for(Iterator<ITickListener> it = this.scheduledTickListenersAdding
				.iterator(); it.hasNext();) {
			this.tickListeners.add(it.next());
			it.remove();
		}

		for(Iterator<ITickListener> it = this.scheduledTickListenersRemoval
				.iterator(); it.hasNext();) {
			this.tickListeners.remove(it.next());
			it.remove();
		}

		this.particleEngine.onTick();

		for(Entry<Coord3D, Block> b : Block.tickingBlocks.entrySet()) {
			if(b.getValue().isRequiringTick())
				b.getValue().onTick(b.getKey().x, b.getKey().y, b.getKey().z,
						this);
		}

		Set<Entry<Coord3D, ScheduledUpdate>> newUpdates = newlyScheduledUpdates
				.entrySet();

		for(Iterator<Entry<Coord3D, ScheduledUpdate>> it = newUpdates
				.iterator(); it.hasNext();) {

			Entry<Coord3D, ScheduledUpdate> e = it.next();

			scheduledUpdates.put(e.getKey(), e.getValue());
			it.remove();
		}

		Set<Entry<Coord3D, ScheduledUpdate>> updates = scheduledUpdates
				.entrySet();

		for(Iterator<Entry<Coord3D, ScheduledUpdate>> it = updates.iterator(); it
				.hasNext();) {

			Entry<Coord3D, ScheduledUpdate> e = it.next();

			Coord3D pos = e.getKey();
			ScheduledUpdate u = e.getValue();

			if(u.ticks > 0) {
				u.ticks--;
			}

			if(u.ticks <= 0) {
				this.updateBlock(pos.x, pos.y, pos.z, u.type);
				it.remove();
			}
		}

		this.chunkProvider.updateLoadedChunksInRadius(
				((int) Math.floor(this.player.getPosX())) >> 4,
				((int) Math.floor(this.player.getPosZ())) >> 4,
				this.game.getSettings().drawDistance.getValue() + 1);

	}

	@Override
	public void onRenderTick(float partialTickTime) {
		this.player.onRenderTick(partialTickTime);

		for(Entry<Coord3D, Block> b : Block.tickingBlocks.entrySet()) {
			if(b.getValue().isRequiringRenderTick())
				b.getValue().onRenderTick(partialTickTime, b.getKey().x,
						b.getKey().y, b.getKey().z, this);
		}

		this.gui.onRenderTick(partialTickTime);
	}

	public ParticleEngine getParticleEngine() {
		return particleEngine;
	}

	public Game getGame() {
		return this.game;
	}

	public GuiManager getGui() {
		return this.gui;
	}

	public void exit() {
		this.renderEngine.cleanup();
		this.chunkProvider.cleanup();
		this.deleteAllRenderChunks();
		this.saveAllChunks();
	}

}
