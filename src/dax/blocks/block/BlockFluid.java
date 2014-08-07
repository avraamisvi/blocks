package dax.blocks.block;

import dax.blocks.block.renderer.BlockRendererFluid;
import dax.blocks.render.RenderPass;
import dax.blocks.world.IDRegister;
import dax.blocks.world.UpdateType;
import dax.blocks.world.World;

public class BlockFluid extends BlockBasic {

	public BlockFluid(String name, IDRegister r) {
		super(name, r);
		this.setCullSame(true);
		this.setOpaque(false);
		this.setOccluder(false);
		this.setRenderPass(RenderPass.TRANSLUCENT);
		this.setCollidable(false);
		this.setRenderer(new BlockRendererFluid());
	}

	@Override
	public void onNeighbourUpdate(int x, int y, int z, World world) {
		world.scheduleUpdate(x, y, z, 4, UpdateType.WATER_FLOW);
	}

	@Override
	public void onUpdate(int x, int y, int z, int type, World world) {
		if(type == UpdateType.WATER_FLOW) {

			if(world.getBlockObject(x, y - 1, z) == IDRegister.water) {
				return;
			}

			Block b = world.getBlockObject(x, y - 1, z);
			boolean isPlant = (b != null && b instanceof BlockPlant);

			if(world.getBlock(x, y - 1, z) == 0 || isPlant) {
				world.setBlock(x, y - 1, z, IDRegister.water.getID(), true,
						true);
				return;
			}
			if(world.getBlock(x + 1, y, z) == 0) {
				world.setBlock(x + 1, y, z, IDRegister.water.getID(), true,
						true);
			}
			if(world.getBlock(x - 1, y, z) == 0) {
				world.setBlock(x - 1, y, z, IDRegister.water.getID(), true,
						true);
			}
			if(world.getBlock(x, y, z + 1) == 0) {
				world.setBlock(x, y, z + 1, IDRegister.water.getID(), true,
						true);
			}
			if(world.getBlock(x, y, z - 1) == 0) {
				world.setBlock(x, y, z - 1, IDRegister.water.getID(), true,
						true);
			}
		}
	}

}
