package cz.dat.oots.block;

import cz.dat.oots.block.renderer.BlockRendererBasic;
import cz.dat.oots.block.renderer.IBlockRenderer;
import cz.dat.oots.collisions.AABB;
import cz.dat.oots.data.IDataObject;
import cz.dat.oots.render.RenderPass;
import cz.dat.oots.sound.SoundManager;
import cz.dat.oots.util.Coord3D;
import cz.dat.oots.world.IDRegister;
import cz.dat.oots.world.World;

import java.util.HashMap;
import java.util.Map;

public abstract class Block {
    public static Map<Coord3D, Block> tickingBlocks = new HashMap<>();
    private int topTexture = 0, sideTexture = 0, bottomTexture = 0;
    private int fallHurt;
    private int renderPass = RenderPass.OPAQUE;
    private float density = 1f;
    private float colorR = 1, colorG = 1, colorB = 1;
    private boolean opaque = true;
    private boolean cullSame = false;
    private boolean occluder = true;
    private boolean collidable = true;
    private boolean requiresRenderTick = false;
    private boolean requiresTick = false;
    private boolean requiresRandomTick = false;
    private String[] footStepSound;
    private String fallSound;
    private String name;
    private String showedName;
    private AABB aabb = new AABB(0, 0, 0, 1, 1, 1);
    private IBlockRenderer renderer;
    private int id;

    public Block(String blockName, IDRegister register) {
        this.id = register.getIDForName(blockName);
        this.name = blockName;
        this.showedName = new String(this.name);

        this.renderer = new BlockRendererBasic();
        this.fallHurt = 5;
        this.footStepSound = SoundManager.footstep_dirt;
        this.fallSound = "fall_hard";
    }

    public void onRandomTick(int x, int y, int z, World world) {}

    public abstract void onUpdate(int x, int y, int z, int type, World world);

    public abstract void onNeighbourUpdate(int x, int y, int z, World world);

    public abstract void onTick(int x, int y, int z, World world);

    public abstract void onRenderTick(float partialTickTime, int x, int y,
                                      int z, World world);

    public abstract void onClick(int mouseButton, int x, int y, int z,
                                 World world);

    public void onPlaced(int x, int y, int z, World world) {
        if (this.isRequiringTick() || this.isRequiringRenderTick()) {
            Block.tickingBlocks.put(new Coord3D(x, y, z), this);
        }
    }

    public void onRemoved(int x, int y, int z, World world) {
        Coord3D c = new Coord3D(x, y, z);
        if (Block.tickingBlocks.get(c) != null) {
            Block.tickingBlocks.remove(c);
        }

        world.removeData(x, y, z);
    }

    public void updateColor(int x, int y, int z, World world) {

    }

    public void restoreColor() {
        this.colorR = 1;
        this.colorG = 1;
        this.colorB = 1;
    }

    public IDataObject createDataObject() {
        return null;
    }

    public String getShowedName() {
        return this.showedName;
    }

    public Block setShowedName(String name) {
        this.showedName = name;
        return this;
    }

    public boolean isRequiringTick() {
        return this.requiresTick;
    }

    public boolean isRequiringRenderTick() {
        return this.requiresRenderTick;
    }

    public boolean isRequiringRandomTick() { return this.requiresRandomTick; }

    public Block requireTick() {
        this.requiresTick = true;
        return this;
    }

    public Block requireRenderTick() {
        this.requiresRenderTick = true;
        return this;
    }

    public Block requireRandomTick() {
        this.requiresRandomTick = true;
        return this;
    }

    public Block setCullSame(boolean cull) {
        this.cullSame = cull;
        return this;
    }

    public Block setAllTextures(int texture) {
        this.topTexture = texture;
        this.sideTexture = texture;
        this.bottomTexture = texture;
        return this;
    }

    public AABB getAABB() {
        this.aabb.resetOffset();
        return this.aabb;
    }

    public void setAABB(AABB bb) {
        this.aabb = bb;
    }

    public AABB getOffsetAABB(float x, float y, float z) {
        this.aabb.resetOffset();
        this.aabb.setOffset(x, y, z);
        return this.aabb;
    }

    public int getID() {
        return this.id;
    }

    public boolean shouldCullSame() {
        return this.cullSame;
    }

    public boolean isOpaque() {
        return this.opaque;
    }

    public Block setOpaque(boolean opaque) {
        this.opaque = opaque;
        return this;
    }

    public int getSideTexture() {
        return this.sideTexture;
    }

    public Block setSideTexture(int sideTexture) {
        this.sideTexture = sideTexture;
        return this;
    }

    public int getTopTexture() {
        return this.topTexture;
    }

    public Block setTopTexture(int topTexture) {
        this.topTexture = topTexture;
        return this;
    }

    public int getBottomTexture() {
        return this.bottomTexture;
    }

    public Block setBottomTexture(int bottomTexture) {
        this.bottomTexture = bottomTexture;
        return this;
    }

    public float getColorR() {
        return this.colorR;
    }

    public float getColorG() {
        return this.colorG;
    }

    public float getColorB() {
        return this.colorB;
    }

    public Block setColor(float r, float g, float b) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        return this;
    }

    public int getFallHurt() {
        return this.fallHurt;
    }

    public Block setFallHurt(int fallHurt) {
        this.fallHurt = fallHurt;
        return this;
    }

    public boolean isOccluder() {
        return this.occluder;
    }

    public Block setOccluder(boolean occluder) {
        this.occluder = occluder;
        return this;
    }

    public boolean isCollidable() {
        return this.collidable;
    }

    public Block setCollidable(boolean collidable) {
        this.collidable = collidable;
        return this;
    }

    public String[] getFootStepSound() {
        return this.footStepSound;
    }

    public Block setFootStepSound(String[] footStepSound) {
        this.footStepSound = footStepSound;
        return this;
    }

    public String getFallSound() {
        return fallSound;
    }

    public Block setFallSound(String fallSound) {
        this.fallSound = fallSound;
        return this;
    }

    public float getDensity() {
        return this.density;
    }

    public Block setDensity(float density) {
        this.density = density;
        return this;
    }

    public int getRenderPass() {
        return this.renderPass;
    }

    public Block setRenderPass(int renderPass) {
        this.renderPass = renderPass;
        return this;
    }

    public IBlockRenderer getRenderer() {
        return this.renderer;
    }

    public Block setRenderer(IBlockRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public String getName() {
        return this.name;
    }
}
