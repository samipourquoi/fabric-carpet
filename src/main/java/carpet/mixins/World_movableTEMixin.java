package carpet.mixins;

import carpet.fakes.WorldChunkInterface;
import carpet.fakes.WorldInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_5269;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class World_movableTEMixin implements WorldInterface, IWorld
{
    @Shadow
    @Final
    public boolean isClient;
    @Shadow
    @Final
    protected class_5269 properties; //LevelProperties

    @Shadow
    public abstract WorldChunk getWorldChunk(BlockPos blockPos_1);
    
    @Shadow
    public abstract BlockState getBlockState(BlockPos blockPos_1);
    
    //@Shadow
    //public abstract ChunkManager getChunkManager();
    
    @Shadow
    public abstract void checkBlockRerender(BlockPos blockPos_1, BlockState s1, BlockState s2);
    
    @Shadow
    public abstract void updateListeners(BlockPos var1, BlockState var2, BlockState var3, int var4);
    
    @Shadow
    public abstract void updateNeighborsAlways(BlockPos blockPos_1, Block block_1);
    
    @Shadow
    public abstract void onBlockChanged(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2);

    @Shadow public abstract Profiler getProfiler();

    @Shadow public abstract void updateComparators(BlockPos pos, Block block);

    @Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags);

    @Shadow public abstract boolean method_27982(); //isDebug()

    /**
     * @author 2No2Name
     */
    public boolean setBlockStateWithBlockEntity(BlockPos blockPos_1, BlockState blockState_1, BlockEntity newBlockEntity, int int_1)
    {
        if ((Object) this instanceof EmptyChunk)
            return false;
        
        if (World.isHeightInvalid(blockPos_1))
        {
            return false;
        }
        else if (!this.isClient && method_27982())
        {
            return false;
        }
        else
        {
            WorldChunk worldChunk_1 = this.getWorldChunk(blockPos_1);
            Block block_1 = blockState_1.getBlock();
            
            BlockState blockState_2;
            if (newBlockEntity != null && block_1 instanceof BlockEntityProvider)
                blockState_2 = ((WorldChunkInterface) worldChunk_1).setBlockStateWithBlockEntity(blockPos_1, blockState_1, newBlockEntity, (int_1 & 64) != 0);
            else
                blockState_2 = worldChunk_1.setBlockState(blockPos_1, blockState_1, (int_1 & 64) != 0);
            
            if (blockState_2 == null)
            {
                return false;
            }
            else
            {
                BlockState blockState_3 = this.getBlockState(blockPos_1);
                
                if (blockState_3 != blockState_2 && (blockState_3.getOpacity((BlockView) this, blockPos_1) != blockState_2.getOpacity((BlockView) this, blockPos_1) || blockState_3.getLuminance() != blockState_2.getLuminance() || blockState_3.hasSidedTransparency() || blockState_2.hasSidedTransparency()))
                {
                    Profiler profiler = getProfiler();
                    profiler.push("queueCheckLight");
                    this.getChunkManager().getLightingProvider().checkBlock(blockPos_1);
                    profiler.pop();
                }
                
                if (blockState_3 == blockState_1)
                {
                    if (blockState_2 != blockState_3)
                    {
                        this.checkBlockRerender(blockPos_1, blockState_2, blockState_3);
                    }
                    
                    if ((int_1 & 2) != 0 && (!this.isClient || (int_1 & 4) == 0) && (this.isClient || worldChunk_1.getLevelType() != null && worldChunk_1.getLevelType().isAfter(ChunkHolder.LevelType.TICKING)))
                    {
                        this.updateListeners(blockPos_1, blockState_2, blockState_1, int_1);
                    }
                    
                    if (!this.isClient && (int_1 & 1) != 0)
                    {
                        this.updateNeighborsAlways(blockPos_1, blockState_2.getBlock());
                        if (blockState_1.hasComparatorOutput())
                        {
                            updateComparators(blockPos_1, block_1);
                        }
                    }
                    
                    if ((int_1 & 16) == 0)
                    {
                        int int_2 = int_1 & -2;
                        blockState_2.prepare((net.minecraft.world.IWorld) this, blockPos_1, int_2);
                        blockState_1.updateNeighbors((net.minecraft.world.IWorld) this, blockPos_1, int_2);
                        blockState_1.prepare((net.minecraft.world.IWorld) this, blockPos_1, int_2);
                    }
                    this.onBlockChanged(blockPos_1, blockState_2, blockState_3);
                }
                return true;
            }
        }
    }
}
