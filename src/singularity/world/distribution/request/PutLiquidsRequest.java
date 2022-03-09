package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.type.LiquidStack;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.LiquidsBuffer;

/**向网络中写入液体，这一操作液体写入网络的缓存中，处理结束由网络将缓存分配给网络中的子容器*/
public class PutLiquidsRequest extends DistRequestBase<LiquidStack>{
  private final LiquidsBuffer source;
  private LiquidsBuffer destination;

  private final Seq<LiquidStack> reqLiquids;
  private final boolean all;

  public PutLiquidsRequest(DistMatrixUnitBuildComp sender, LiquidsBuffer source){
    this(sender, source, null);
  }

  public PutLiquidsRequest(DistMatrixUnitBuildComp sender, LiquidsBuffer source, Seq<LiquidStack> req){
    super(sender);
    this.source = source;
    this.reqLiquids = req;
    all = req == null;
  }

  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    destination = target.getCore().distCore().getBuffer(DistBuffers.liquidBuffer);
  }

  @Override
  public boolean handle(){
    boolean test = false;
    if(all){
      for(LiquidsBuffer.LiquidPacket packet: source){
        float move = Math.min(packet.amount(), destination.remainingCapacity().floatValue());

        if(move < 0.001f) continue;
        packet.remove(move);
        destination.put(packet.get(), move);
      }
    }
    else{
      for(LiquidStack stack: reqLiquids){
        float move = Math.min(stack.amount, source.get(stack.liquid));
        move = Math.min(move, destination.remainingCapacity().floatValue());

        if(move < 0.001f) continue;
        source.remove(stack.liquid, move);
        destination.put(stack.liquid, move);
        test = true;
      }
    }
    return test;
  }

  @Override
  public Seq<LiquidStack> getList(){
    return reqLiquids;
  }
}
