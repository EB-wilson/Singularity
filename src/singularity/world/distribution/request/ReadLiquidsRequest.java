package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.LiquidStack;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.LiquidsBuffer;

/**从网络中读取液体，此操作将液体从网络缓存读出并写入到目标缓存，网络缓存会优先提供已缓存液体，若不足则从网络子容器申请液体到网络缓存再分配*/
public class ReadLiquidsRequest extends DistRequestBase<LiquidStack>{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private final LiquidsBuffer destination;
  private LiquidsBuffer source;

  private final Seq<LiquidStack> reqLiquids;
  private static final Seq<LiquidStack> tempLiquid = new Seq<>();

  public ReadLiquidsRequest(DistMatrixUnitBuildComp sender, LiquidsBuffer destination, Seq<LiquidStack> req){
    super(sender);
    this.destination = destination;
    this.reqLiquids = req;
  }

  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    source = target.getCore().distCore().getBuffer(DistBuffers.liquidBuffer);
  }

  @Override
  public boolean preHandle(){
    tempLiquid.clear();
    for(LiquidStack stack: reqLiquids){
      float req = stack.amount - source.get(stack.liquid);
      if(req > 0){
        tempLiquid.add(new LiquidStack(stack.liquid, req));
      }
    }

    liquidFor: for(LiquidStack stack: tempLiquid){
      for(MatrixGrid grid: target.grids){
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(
            GridChildType.container,
            (e, c) -> e.liquids.get(stack.liquid) > 0.001f && c.get(GridChildType.container, stack.liquid),
            temp)){
          if(stack.amount < 0.001f) continue liquidFor;
          if(source.remainingCapacity().floatValue() < 0.001f) break liquidFor;

          float cont = Math.min(stack.amount, entry.entity.liquids.get(stack.liquid));
          cont = Math.min(cont, source.remainingCapacity().floatValue());

          if(cont > 0.001f){
            entry.entity.liquids.remove(stack.liquid, cont);
            source.put(stack.liquid, cont);
            stack.amount -= cont;
          }
        }
      }
    }
    return true;
  }

  @Override
  public boolean handle(){
    boolean blockTest = false;
    for(LiquidStack stack : reqLiquids){
      float move = Math.min(stack.amount, source.get(stack.liquid));
      move = Math.min(move, destination.remainingCapacity().floatValue());
      if(move <= 0) continue;

      source.remove(stack.liquid, move);
      destination.put(stack.liquid, move);
      blockTest = true;
    }
    return blockTest;
  }

  @Override
  public Seq<LiquidStack> getList(){
    return reqLiquids;
  }
}
