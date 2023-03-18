package singularity.world.distribution.request;

import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.LiquidsBuffer;

import java.util.Arrays;

/**向网络中写入液体，这一操作液体写入网络的缓存中，处理结束由网络将缓存分配给网络中的子容器*/
public class PutLiquidsRequest extends DistRequestBase{
  protected final LiquidsBuffer source;
  protected LiquidsBuffer destination;

  protected final Seq<LiquidStack> reqLiquids;
  protected final boolean all;

  protected float[] lastHandles = new float[Vars.content.liquids().size];

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
    destination = target.getCore().distCore().getBuffer(DistBufferType.liquidBuffer);
  }

  @Override
  protected boolean preHandleTask(){
    return true;
  }

  @Override
  protected boolean handleTask(){
    Arrays.fill(lastHandles, 0);
    if(all){
      for(LiquidsBuffer.LiquidPacket packet: source){
        float move = Math.min(packet.amount(), destination.remainingCapacity());
        move -= move%LiquidsBuffer.LiquidIntegerStack.packMulti;

        if(move < 0.001f) continue;

        lastHandles[packet.id()] = move;

        packet.remove(move);
        destination.put(packet.get(), move);
      }
      return true;
    }
    else{
      boolean test = false;
      for(LiquidStack stack: reqLiquids){
        float move = Math.min(stack.amount, Math.min(source.get(stack.liquid), destination.remainingCapacity()));
        move -= move%LiquidsBuffer.LiquidIntegerStack.packMulti;

        if(move < 0.001f) continue;

        lastHandles[stack.liquid.id] = move;

        source.remove(stack.liquid, move);
        destination.put(stack.liquid, move);
        test = true;
      }
      return test;
    }
  }

  @Override
  protected boolean afterHandleTask(){
    for(int id = 0; id < lastHandles.length; id++){
      Liquid liquid = Vars.content.liquid(id);

      float rem = Math.min(lastHandles[id], destination.get(liquid));
      rem = Math.min(rem, source.remainingCapacity());

      rem -= rem%LiquidsBuffer.LiquidIntegerStack.packMulti;
      if (rem <= 0) continue;

      destination.remove(liquid, rem);
      destination.deReadFlow(liquid, rem);
      source.put(liquid, rem);
      source.dePutFlow(liquid, rem);
    }

    return true;
  }
}
