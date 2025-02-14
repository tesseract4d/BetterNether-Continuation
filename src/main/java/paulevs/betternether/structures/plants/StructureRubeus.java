package paulevs.betternether.structures.plants;

import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import paulevs.betternether.BlocksHelper;
import paulevs.betternether.MHelper;
import paulevs.betternether.blocks.BlockRubeusLog;
import paulevs.betternether.blocks.BlocksRegistry;
import paulevs.betternether.config.ConfigLoader;
import paulevs.betternether.structures.StructureFuncScatter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import static net.minecraft.block.BlockLog.LOG_AXIS;
import static paulevs.betternether.blocks.BlockRubeusLog.VARIANT;

public class StructureRubeus extends StructureFuncScatter {
    private static final float[] CURVE_X = new float[]{9F, 7F, 1.5F, 0.5F, 3F, 7F};
    private static final float[] CURVE_Y = new float[]{20F, 17F, 12F, 4F, 0F, -2F};
    private static final int MIDDLE_Y = 10;
    private static final Set<BlockPos> POINTS = new HashSet<BlockPos>();
    private static final Set<BlockPos> MIDDLE = new HashSet<BlockPos>();
    private static final Set<BlockPos> TOP = new HashSet<BlockPos>();

    public StructureRubeus() {
        super(7);
    }
    @Override
    public void grow(World world, BlockPos pos, Random random) {
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 0);
        float scale = MHelper.randRange(0.5F, 1F, random);
        int minCount = scale < 0.75 ? 3 : 4;
        int maxCount = scale < 0.75 ? 5 : 7;
        int count = MHelper.randRange(minCount, maxCount, random);
        for (int n = 0; n < count; n++) {
            float branchSize = MHelper.randRange(0.5F, 0.8F, random) * scale;
            float angle = n * MHelper.PI2 / count;
            float radius = CURVE_X[0] * branchSize;
            int x1 = Math.round(pos.getX() + radius * (float) Math.cos(angle) + MHelper.randRange(-2F, 2F, random) * branchSize);
            int y1 = Math.round(pos.getY() + CURVE_Y[0] * branchSize + MHelper.randRange(-2F, 2F, random) * branchSize);
            int z1 = Math.round(pos.getZ() + radius * (float) Math.sin(angle) + MHelper.randRange(-2F, 2F, random) * branchSize);
            float crownR = 5 * branchSize;
            if (crownR < 1.5F)
                crownR = 1.5F;
            crown(world, x1, y1 + 1, z1, crownR, random);

            int middle = Math.round(pos.getY() + (MIDDLE_Y + MHelper.randRange(-2, 2, random)) * branchSize);
            boolean generate = true;
            for (int i = 1; i < CURVE_X.length && generate; i++) {
                radius = CURVE_X[i] * branchSize;
                int x2 = Math.round(pos.getX() + radius * (float) Math.cos(angle) + MHelper.randRange(-2F, 2F, random) * branchSize);
                int y2 = Math.round(pos.getY() + CURVE_Y[i] * branchSize + (CURVE_Y[i] > 0 ? MHelper.randRange(-2F, 2F, random) * branchSize : 0));
                int z2 = Math.round(pos.getZ() + radius * (float) Math.sin(angle) + MHelper.randRange(-2F, 2F, random) * branchSize);

                if (CURVE_Y[i] <= 0) {
                    if (!isGround(world.getBlockState(POS.setPos(x2, y2, z2)))) {
                        boolean noGround = true;
                        for (int d = 1; d < 3; d++) {
                            if (isGround(world.getBlockState(POS.setPos(x2, y2 - d, z2)))) {
                                y2 -= d;
                                noGround = false;
                                break;
                            }
                        }
                        if (noGround) {
                            x2 = pos.getX();
                            y2 = pos.getY();
                            generate = false;
                        }
                    }
                }
                if(y1 < y2)
                    return;
                line(world, x1, y1, z1, x2, y2, z2, middle);
                x1 = x2;
                y1 = y2;
                z1 = z2;
            }
        }

        IBlockState state;
        Iterator<BlockPos> iterator = TOP.iterator();
        while (iterator.hasNext()) {
            BlockPos bpos = iterator.next();
            if (bpos != null) {
                if (POINTS.contains(bpos.up()) && !TOP.contains(bpos.up()))
                    iterator.remove();
            }
        }

        iterator = MIDDLE.iterator();
        while (iterator.hasNext()) {
            BlockPos bpos = iterator.next();
            if (bpos != null) {
                BlockPos up = bpos.up();
                if (MIDDLE.contains(up) || (!TOP.contains(up) && POINTS.contains(up)))
                    iterator.remove();
            }
            else
                iterator.remove();
        }

        for (BlockPos bpos : POINTS) {
            state = BlocksRegistry.BLOCK_RUBEUS_LOG.getBlockState().getBaseState().withProperty(LOG_AXIS, BlockLog.EnumAxis.NONE);
            if (MIDDLE.contains(bpos))
                setCondition(world, bpos, pos.getY(), state.withProperty(VARIANT, BlockRubeusLog.EnumType.MIDDLE));
            else if (TOP.contains(bpos))
                setCondition(world, bpos, pos.getY(), state.withProperty(VARIANT, BlockRubeusLog.EnumType.TOP));
                else setCondition(world, bpos, pos.getY(), state.withProperty(VARIANT, BlockRubeusLog.EnumType.BOTTOM));
        }

        POINTS.clear();
        MIDDLE.clear();
        TOP.clear();
    }

    @Override
    public void generate(World world, BlockPos pos, Random random) {
        int length = BlocksHelper.upRay(world, pos, StructureStalagnate.MAX_LENGTH + 2);
        if (length >= StructureStalagnate.MAX_LENGTH) super.generate(world, pos, random);
    }

    @Override
    protected boolean isStructure(IBlockState state) {
        return state.getBlock() == BlocksRegistry.BLOCK_RUBEUS_LOG;
    }

    @Override
    protected boolean isGround(IBlockState state) {
        return ConfigLoader.isTerrain(state.getBlock());
    }

    private void line(World world, int x1, int y1, int z1, int x2, int y2, int z2, int middleY) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int dz = z2 - z1;
        int mx = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
        float fdx = (float) dx / mx;
        float fdy = (float) dy / mx;
        float fdz = (float) dz / mx;
        float px = x1;
        float py = y1;
        float pz = z1;

        BlockPos pos = POS.setPos(x1, y1, z1).toImmutable();
        POINTS.add(pos);
        if (pos.getY() == middleY) MIDDLE.add(pos);
        else if (pos.getY() > middleY) TOP.add(pos);

        pos = POS.setPos(x2, y2, z2).toImmutable();
        POINTS.add(pos);
        if (pos.getY() == middleY) MIDDLE.add(pos);
        else if (pos.getY() > middleY) TOP.add(pos);

        for (int i = 0; i < mx; i++) {
            px += fdx;
            py += fdy;
            pz += fdz;

            POS.setPos(Math.round(px), Math.round(py), Math.round(pz));
            pos = POS.toImmutable();
            POINTS.add(pos);
            if (POS.getY() == middleY) MIDDLE.add(pos);
            else if (POS.getY() > middleY) TOP.add(pos);
        }
    }


    private void crown(World world, int x, int y, int z, float radius, Random random) {
        IBlockState leaves = BlocksRegistry.BLOCK_RUBEUS_LEAVES.getDefaultState();
        float halfR = radius * 0.5F;
        float r2 = radius * radius;
        int start = (int) Math.floor(-radius);
        for (int cy = start; cy <= radius; cy++) {
            int cy2_out = cy * cy;
            float cy2_in = cy + halfR;
            cy2_in *= cy2_in;
            POS.setY((int) (y + cy - halfR));
            for (int cx = start; cx <= radius; cx++) {
                int cx2 = cx * cx;
                POS.x = (x + cx);
                for (int cz = start; cz <= radius; cz++) {
                    int cz2 = cz * cz;
                    if (cx2 + cy2_out + cz2 < r2 && cx2 + cy2_in + cz2 > r2) {
                        POS.z = (z + cz);
                        setIfAirLeaves(world, POS, leaves);
                    }
                }
            }
        }
    }

    private void setCondition(World world, BlockPos pos, int y, IBlockState state) {
        if (pos.getY() > y)
            setIfAir(world, pos, state);
        else
            setIfGroundOrAir(world, pos, state);
    }

    private void setIfAir(World world, BlockPos pos, IBlockState state) {
        IBlockState bState = world.getBlockState(pos);
        if (world.isAirBlock(pos) || bState.getBlock() == BlocksRegistry.BLOCK_RUBEUS_LEAVES || bState.getMaterial().isReplaceable())
            BlocksHelper.setWithoutUpdate(world, pos, state);
    }

    private void setIfGroundOrAir(World world, BlockPos pos, IBlockState state) {
        IBlockState bState = world.getBlockState(pos);
        if (world.isAirBlock(pos) || bState.getBlock() == BlocksRegistry.BLOCK_RUBEUS_LEAVES || bState.getMaterial().isReplaceable() || BlocksHelper.isNetherGround(bState))
            BlocksHelper.setWithoutUpdate(world, pos, state);
    }

    private void setIfAirLeaves(World world, BlockPos pos, IBlockState state) {
        IBlockState bState = world.getBlockState(pos);
        if (world.isAirBlock(pos) || bState.getMaterial().isReplaceable())
            BlocksHelper.setWithoutUpdate(world, pos, state);
    }
}
