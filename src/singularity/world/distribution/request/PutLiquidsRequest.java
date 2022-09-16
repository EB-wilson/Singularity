package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.type.LiquidStack;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.LiquidsBuffer;

/**向网络中写入液体，这一操作液体写入网络的缓存中，处理结束由网络将缓存分配给网络中的子容器*/
public class PutLiquidsRequest extends DistRequestBase<LiquidStack>{
  protected final LiquidsBuffer source;
  protected LiquidsBuffer destination;

  protected final Seq<LiquidStack> reqLiquids;
  protected final boolean all;

  public PutLiquidsRequest(DistElementBuildComp sender, LiquidsBuffer source){
    this(sender, source, null);
  }

  public PutLiquidsRequest(DistElementBuildComp sender, LiquidsBuffer source, Seq<LiquidStack> req){
    super(sender);
    this.source = source;
    this.reqLiquids = req;
    all = req == null;
  }

  @Override
  public int priority(){
    return 64;
  }

  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    destination = target.getCore().distCore().getBuffer(DistBuffers.liquidBuffer);
  }

  @Override
  protected boolean preHandleTask(){
    return true;
  }

  @Override
  protected boolean handleTask(){
    boolean test = false;
    if(all){
      for(LiquidsBuffer.LiquidPacket packet: source){
        float move = Math.min(packet.amount(), destination.remainingCapacity());

        if(move < 0.001f) continue;
        packet.remove(move);
        destination.put(packet.get(), move);
      }
    }
    else{
      for(LiquidStack stack: reqLiquids){
        float move = Math.min(source.get(stack.liquid), destination.remainingCapacity());

        if(move < 0.001f) continue;
        source.remove(stack.liquid, move);
        destination.put(stack.liquid, move);
        test = true;
      }
    }
    return test;
  }

  @Override
  protected boolean afterHandleTask(){
    return true;
  }
}
