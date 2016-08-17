package cz.dat.oots.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class ChunkRendererVBO implements IChunkRenderer {

    private static final int BUFFER_SIZE_MEGS = 8;
    private static final int BUFFER_SIZE_BYTES = BUFFER_SIZE_MEGS * 1024 * 1024;
    private static final int BUFFER_SIZE = BUFFER_SIZE_BYTES >> 2;
    public static int total = 0;
    public static int sum = 0;
    private int handle;
    private int vertices = 0;
    private FloatBuffer drawBuffer;
    private float colorR = 1f;
    private float colorG = 1f;
    private float colorB = 1f;
    private float colorA = 1f;
    private float normalX = 0f;
    private float normalY = 0f;
    private float normalZ = 0f;
    private float textureS = 0f;
    private float textureT = 0f;
    private float aov = 1;
    private Map<Integer, Integer> vertexCounts = new HashMap<Integer, Integer>();

    public ChunkRendererVBO() {
        this.drawBuffer = BufferUtils.createFloatBuffer(BUFFER_SIZE);
    }

    @Override
    public void begin() {
        this.reset();
    }

    @Override
    public void end() {
        this.drawBuffer.flip();

        this.handle = GL15.glGenBuffers();

        long s = System.nanoTime();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.handle);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.drawBuffer, GL15.GL_STATIC_DRAW);

        long e = System.nanoTime();

        //System.out.println("Uploaded buffer with " + this.vertices + " vertices (" + this.vertices * 4 * 8 / 1024f + "KB), took " + (e - s) / 1000000f + "ms");

        sum += this.vertices * 32;
        total++;

        //System.out.println("Avg mesh size: " + (sum/total)/1024f + "kB");

        this.vertexCounts.put(this.handle, this.vertices);
    }

    private void reset() {
        this.drawBuffer.clear();

        this.handle = 0;
        this.vertices = 0;

        this.colorR = 1f;
        this.colorG = 1f;
        this.colorB = 1f;

        this.normalX = 0f;
        this.normalY = 0f;
        this.normalZ = 0f;

        this.textureS = 0f;
        this.textureT = 0f;
    }

    @Override
    public int getVertexCount() {
        return this.vertices;
    }

    @Override
    public int getHandle() {
        return this.handle;
    }

    @Override
    public void delete(int handle) {
        GL15.glDeleteBuffers(handle);
    }

    @Override
    public void render(int handle) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, handle);

        // Foku this shit, OpenGL doc sucks
        // data per vertex, type, stride (bytes), offset (bytes)

        int stride = 12 << 2;

        GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, 0);
        GL11.glColorPointer(4, GL11.GL_FLOAT, stride, 3 << 2);
        GL11.glNormalPointer(GL11.GL_FLOAT, stride, 7 << 2);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, 10 << 2);

        GL11.glDrawArrays(GL11.GL_QUADS, 0, this.vertexCounts.get(handle));
    }

    @Override
    public void vertex(float x, float y, float z) {
        vertices++;
        drawBuffer.put(x).put(y).put(z);
        drawBuffer.put(colorR).put(colorG).put(colorB).put(colorA);
        drawBuffer.put(normalX).put(normalY).put(normalZ);
        drawBuffer.put(textureS).put(textureT);
    }

    @Override
    public void vertexWithAO(float x, float y, float z, boolean s1, boolean s2,
                             boolean c) {
        this.vertexWithColoredAO(x, y, z, 1, 1, 1, s1, s2, c);
    }

    @Override
    public void vertexWithColoredAO(float x, float y, float z, float r,
                                    float g, float b, boolean s1, boolean s2, boolean c) {
        float ao;

        if (s1 && s2) {
            ao = 3;
        } else {
            ao = 0;
            if (s1)
                ao++;
            if (s2)
                ao++;
            if (c)
                ao++;
        }

        float aom = ao * this.aov;

        this.color(r - aom, g - aom, b - aom);
        this.vertex(x, y, z);
    }

    @Override
    public void color(float r, float b, float g) {
        this.color(r, g, b, 1);
    }

    @Override
    public void color(float r, float b, float g, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
    }

    @Override
    public void normal(float nx, float ny, float nz) {
        this.normalX = nx;
        this.normalY = ny;
        this.normalZ = nz;
    }

    @Override
    public void texCoord(float s, float t) {
        this.textureS = s;
        this.textureT = t;
    }

    @Override
    public void beforeRendering() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
    }

    @Override
    public void afterRendering() {
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
    }

    @Override
    public void beforeBuilding() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
    }

    @Override
    public void afterBuilding() {
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
    }

    @Override
    public void setAOIntensity(float value) {
        this.aov = value;
    }

}
