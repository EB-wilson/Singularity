package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.type.ItemStack;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.ItemsBuffer;
import universecore.util.Empties;

/**向网络中写入物品，这一操作将物品写入网络的缓存中，处理结束由网络将缓存分配给网络中的子容器*/
public class PutItemsRequest extends DistRequestBase{
  protected final ItemsBuffer source;
  protected ItemsBuffer destination;

  protected final Seq<ItemStack> reqItems;
  protected boolean all;
  
  public PutItemsRequest(DistElementBuildComp sender, ItemsBuffer source){
    this(sender, source, Empties.nilSeq());
    allItemPut();
  }
  
  public PutItemsRequest(DistElementBuildComp sender, ItemsBuffer source, Seq<ItemStack> items){
    super(sender);
    this.source = source;
    this.reqItems = items;
  }
  
  public void allItemPut(){
    all = true;
  }
  
  @Override
  public int priority(){
    return 64;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    destination = target.getCore().distCore().getBuffer(DistBufferType.itemBuffer);
  }

  @Override
  protected boolean preHandleTask(){
    return true;
  }

  @Override
  public boolean handleTask(){
    if(all){
      for(ItemsBuffer.ItemPacket packet : source){
        int move = Math.min(packet.amount(), destination.remainingCapacity());
        if(move <= 0) continue;

        packet.remove(move);
        destination.put(packet.get(), move);
      }
      return true;
    }
    else{
      boolean blockTest = false;
      for(ItemStack stack : reqItems){
        int move = Math.min(source.get(stack.item), destination.remainingCapacity());

        if(move <= 0) continue;

        source.remove(stack.item, move);
        destination.put(stack.item, move);
        blockTest = true;
      }
      return blockTest;
    }
  }

  @Override
  protected boolean afterHandleTask(){
    return true;
  }
}
