package cz.dat.oots.overlay;

import cz.dat.oots.FontManager;
import cz.dat.oots.Game;
import cz.dat.oots.profiler.Profiler;
import cz.dat.oots.profiler.Section;
import cz.dat.oots.render.IOverlayRenderer;
import cz.dat.oots.util.GLHelper;
import cz.dat.oots.world.World;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

import java.util.Locale;

public class InfoOverlay implements IOverlayRenderer {

    private Profiler profiler;
    private World world;
    private Game game;

    public InfoOverlay(Game game) {
        this.profiler = game.getProfiler();
        this.world = game.getWorldsManager().getWorld();
        this.game = game;
    }

    @Override
    public void renderOverlay(float partialTickTime) {
        TrueTypeFont font = FontManager.getFont();

        if (this.game.s().debug.getValue()) {
            GL11.glLineWidth(1);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_LINES);

            int offset = Display.getWidth() - Section.MAX_RECORDS;

            float[] tick = this.profiler.tick.getTimes();
            float[] render = this.profiler.render.getTimes();
            float[] build = this.profiler.build.getTimes();

            for (int i = 0; i < Section.MAX_RECORDS; i++) {
                GL11.glColor4f(0, 1, 0, 1.0f);
                GL11.glVertex2f(offset + i, Display.getHeight());
                GL11.glVertex2f(offset + i, Display.getHeight() - tick[i] * 10);

                GL11.glColor4f(0, 0, 1, 0.5f);
                GL11.glVertex2f(offset + i, Display.getHeight());
                GL11.glVertex2f(offset + i, Display.getHeight() - render[i]
                        * 10);

                GL11.glColor4f(1, 0, 0, 0.4f);
                GL11.glVertex2f(offset + i, Display.getHeight());
                GL11.glVertex2f(offset + i, Display.getHeight() - build[i] * 10);
            }

            float avgTick = this.profiler.tick.avg();
            float avgRender = this.profiler.render.avg();
            float avgBuild = this.profiler.build.avg();

            GL11.glColor4f(0.3f, 1.0f, 0, 1.0f);
            GL11.glVertex2f(Display.getWidth() - Section.MAX_RECORDS,
                    Display.getHeight() - avgTick * 10);
            GL11.glVertex2f(Display.getWidth(), Display.getHeight() - avgTick
                    * 10);

            GL11.glColor4f(0.3f, 0, 1.0f, 1.0f);
            GL11.glVertex2f(Display.getWidth() - Section.MAX_RECORDS,
                    Display.getHeight() - avgRender * 10);
            GL11.glVertex2f(Display.getWidth(), Display.getHeight() - avgRender
                    * 10);

            GL11.glColor4f(1.0f, 0, 0.3f, 1.0f);

            GL11.glVertex2f(Display.getWidth() - Section.MAX_RECORDS,
                    Display.getHeight() - avgBuild * 10);
            GL11.glVertex2f(Display.getWidth(), Display.getHeight() - avgBuild
                    * 10);

            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            String tickText = String.format(Locale.ENGLISH, "avg tick %.2f",
                    avgTick) + "ms";
            FontManager
                    .getFont()
                    .drawString(
                            (int) (offset
                                    - FontManager.getFont().getWidth(tickText) - 2),
                            (int) (Display.getHeight() - avgTick * 10 - FontManager
                                    .getFont().getLineHeight() * 0.75f),
                            tickText);

            String renderText = String.format(Locale.ENGLISH,
                    "avg render %.2f", avgRender) + "ms";
            FontManager
                    .getFont()
                    .drawString(
                            (int) (offset
                                    - FontManager.getFont()
                                    .getWidth(renderText) - 2),
                            (int) (Display.getHeight() - avgRender * 10 - FontManager
                                    .getFont().getLineHeight() * 0.75f),
                            renderText);

            String buildText = String.format(Locale.ENGLISH, "avg build %.2f",
                    avgBuild) + "ms";
            FontManager
                    .getFont()
                    .drawString(
                            (int) (offset
                                    - FontManager.getFont().getWidth(buildText) - 2),
                            (int) (Display.getHeight() - avgBuild * 10 - FontManager
                                    .getFont().getLineHeight() * 0.75f),
                            buildText);
        }

        GL11.glColor4f(1, 1, 1, 1);

        Runtime runtime = Runtime.getRuntime();

        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        String fpsString = "FPS: " + game.getFPS() + ", Ticks: "
                + game.getTPS();
        int stringWidth = font.getWidth(fpsString);
        font.drawString(Display.getWidth() - stringWidth - 2,
                font.getHeight() * 2, fpsString);

        font.drawString(2, 0, "X Position: " + world.getPlayer().getPosX()
                + " (laX: " + world.getPlayer().getLookingAtX() + ")");
        font.drawString(2, font.getHeight(), "Y Position: "
                + world.getPlayer().getPosY() + " (laY: "
                + world.getPlayer().getLookingAtY() + ")");
        font.drawString(2, font.getHeight() * 2, "Z Position: "
                + world.getPlayer().getPosZ() + " (laZ: "
                + world.getPlayer().getLookingAtZ() + ")");
        font.drawString(
                2,
                font.getHeight() * 3,
                "Biome: "
                        + world.getChunkProvider()
                        .getBiomeAtLocation(
                                (int) world.getPlayer().getPosX(),
                                (int) world.getPlayer().getPosZ())
                        .getName());
        font.drawString(2, font.getHeight() * 4, "Lives: "
                + ((int) (world.getPlayer().getLifes() * 100)));

        String memory = "Used memory: "
                + (allocatedMemory / (1024 * 1024) - freeMemory / (1024 * 1024))
                + "MB" + "/" + allocatedMemory / (1024 * 1024) + "MB";
        int memoryWidth = font.getWidth(memory);
        font.drawString(Display.getWidth() - memoryWidth - 2, 0, memory);

        String chunks = "Chunks drawn: " + world.getRenderEngine().chunksDrawn
                + "/" + world.getRenderEngine().chunksLoaded;
        font.drawString(Display.getWidth() - font.getWidth(chunks) - 2,
                font.getHeight(), chunks);
        String vertices = "Vertices: " + (world.getRenderEngine().vertices + world.getParticleEngine().getCount());
        font.drawString(
                this.game.getSettings().resolution.width()
                        - font.getWidth(vertices) - 2, font.getHeight() * 3, vertices);

        String particles = "Particles: " + world.getParticleEngine().getCount();
        font.drawString(
                this.game.getSettings().resolution.width()
                        - font.getWidth(particles) - 2, font.getHeight() * 4, particles);

        if (world.getChunkProvider().loading) {
            font.drawString(
                    Display.getWidth() - font.getWidth("Loading chunks...") - 2,
                    Display.getHeight() - font.getHeight(),
                    "Loading chunks...", Color.white);
        }

        if (world.getRenderEngine().building) {
            font.drawString(
                    Display.getWidth() - font.getWidth("Building chunks...")
                            - 2, Display.getHeight() - font.getHeight()
                            * (world.getChunkProvider().loading ? 2 : 1),
                    "Building chunks...", Color.white);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GLHelper.drawLine(Display.getWidth() / 2, Display.getWidth() / 2,
                (Display.getHeight() / 2) - 10, (Display.getHeight() / 2) + 10,
                2, 0, 0, 0, 0.5f);
        GLHelper.drawLine((Display.getWidth() / 2) - 10,
                (Display.getWidth() / 2) + 10, Display.getHeight() / 2,
                Display.getHeight() / 2, 2, 0, 0, 0, 0.5f);

    }

}
