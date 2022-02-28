package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.type.ItemStack;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.ItemsBuffer;

/**向网络中写入物品，这一操作将物品写入网络的缓存中，处理结束由网络将缓存分配给网络中的子容器*/
public class PutItemsRequest extends DistRequestBase<ItemStack>{
  private final ItemsBuffer source;
  private ItemsBuffer destination;
  
  private final Seq<ItemStack> reqItems;
  
  public PutItemsRequest(DistMatrixUnitBuildComp sender, ItemsBuffer source, Seq<ItemStack> items){
    super(sender);
    this.source = source;
    this.reqItems = items;
  }
  
  @Override
  public int priority(){
    return 128;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    destination = target.getCore().distributor().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public void handle(){
    for(ItemStack stack : reqItems){
      int move = Math.min(stack.amount, source.get(stack.item));
      move = Math.min(move, destination.remainingCapacity().intValue());
      if(move <= 0) continue;
      
      source.remove(stack.item, move);
      destination.put(stack.item, move);
    }
  }
  
  @Override
  public Seq<ItemStack> getList(){
    return reqItems;
  }
}
