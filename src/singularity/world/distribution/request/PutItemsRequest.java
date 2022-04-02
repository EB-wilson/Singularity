package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.type.ItemStack;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.ItemsBuffer;
import universecore.util.Empties;

/**向网络中写入物品，这一操作将物品写入网络的缓存中，处理结束由网络将缓存分配给网络中的子容器*/
public class PutItemsRequest extends DistRequestBase<ItemStack>{
  private final ItemsBuffer source;
  private ItemsBuffer destination;
  
  private final Seq<ItemStack> reqItems;
  private boolean all;
  
  public PutItemsRequest(DistMatrixUnitBuildComp sender, ItemsBuffer source){
    this(sender, source, Empties.nilSeq());
    allItemPut();
  }
  
  public PutItemsRequest(DistMatrixUnitBuildComp sender, ItemsBuffer source, Seq<ItemStack> items){
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
    destination = target.getCore().distCore().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public boolean handle(){
    if(all){
      for(ItemsBuffer.ItemPacket packet : source){
        int move = Math.min(packet.amount(), destination.remainingCapacity().intValue());
        if(move <= 0) continue;
        
        packet.remove(move);
        destination.put(packet.get(), move);
      }
      return true;
    }
    else{
      boolean blockTest = false;
      for(ItemStack stack : reqItems){
        int move = Math.min(stack.amount, source.get(stack.item));
        move = Math.min(move, destination.remainingCapacity().intValue());
        
        if(move <= 0) continue;
    
        source.remove(stack.item, move);
        destination.put(stack.item, move);
        blockTest = true;
      }
      return blockTest;
    }
  }
  
  @Override
  public Seq<ItemStack> getList(){
    return reqItems;
  }
}
