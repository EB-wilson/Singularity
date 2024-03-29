package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.LiquidsBuffer;

import java.util.Arrays;

/**从网络中读取液体，此操作将液体从网络缓存读出并写入到目标缓存，网络缓存会优先提供已缓存液体，若不足则从网络子容器申请液体到网络缓存再分配*/
public class ReadLiquidsRequest extends DistRequestBase{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private static float[] tempLiquid;

  private final LiquidsBuffer destination;
  private LiquidsBuffer source;

  private final Seq<LiquidStack> reqLiquids;

  public ReadLiquidsRequest(DistElementBuildComp sender, LiquidsBuffer destination, Seq<LiquidStack> req){
    super(sender);
    this.destination = destination;
    this.reqLiquids = req; 
  }

  @Override
  public int priority(){
    return 128;
  }

  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    source = target.getCore().getBuffer(DistBufferType.liquidBuffer);
  }

  @Override
  public boolean preHandleTask(){
    if (tempLiquid == null || tempLiquid.length != Vars.content.liquids().size){
      tempLiquid = new float[Vars.content.liquids().size];
    }

    Arrays.fill(tempLiquid, 0);
    for(LiquidStack stack: reqLiquids){
      tempLiquid[stack.liquid.id] = stack.amount*destination.maxCapacity() - destination.get(stack.liquid) - source.get(stack.liquid);
    }

    liquidFor: for(int id = 0; id<tempLiquid.length; id++){
      if(tempLiquid[id] <= 0) continue;
      Liquid liquid = Vars.content.liquid(id);
      for(MatrixGrid grid: target.grids){
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(
            GridChildType.container,
            (e, c) -> e.block.hasLiquids
                && e.liquids != null
                && e.liquids.get(liquid) > 0.001f
                && c.get(GridChildType.container, liquid), temp)){
          if(tempLiquid[id] < 0.001f || source.remainingCapacity() < 0.001f) continue liquidFor;

          float move = Math.min(tempLiquid[id], entry.entity.liquids.get(liquid));
          move = Math.min(move, source.remainingCapacity());

          move -= move%LiquidsBuffer.LiquidIntegerStack.packMulti;
          if(move > 0.001f){
            entry.entity.liquids.remove(liquid, move);
            source.put(liquid, move);
            source.dePutFlow(liquid, move);
            tempLiquid[id] -= move;
          }
        }
      }
    }
    return true;
  }

  @Override
  public boolean handleTask(){
    boolean blockTest = false;
    for(LiquidStack stack : reqLiquids){
      float targetAmount = stack.amount*destination.maxCapacity();

      float move = Math.min(targetAmount - destination.get(stack.liquid), source.get(stack.liquid));

      move -= move%LiquidsBuffer.LiquidIntegerStack.packMulti;
      if(move <= 0) continue;

      source.remove(stack.liquid, move);
      destination.put(stack.liquid, move);
      blockTest = true;
    }
    return blockTest;
  }

  @Override
  protected boolean afterHandleTask(){
    return true;
  }
}
