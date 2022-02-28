package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.ItemSeq;
import mindustry.type.ItemStack;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.ItemsBuffer;

/**从网络中读取物品，此操作将物品从网络缓存读出并写入到目标缓存，网络缓存会优先提供已缓存物品，若不足则从网络子容器申请物品到网络缓存再分配*/
public class ReadItemsRequest extends DistRequestBase<ItemStack>{
  private final ItemsBuffer destination;
  private ItemsBuffer source;
  
  private final Seq<ItemStack> reqItems;
  private static final ItemSeq tempItems = new ItemSeq();
  
  public ReadItemsRequest(DistMatrixUnitBuildComp sender, ItemsBuffer destination, Seq<ItemStack> items){
    super(sender);
    this.destination = destination;
    reqItems = items;
  }
  
  @Override
  public int priority(){
    return 64;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    source = target.getCore().distributor().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public void preHandle(){
    tempItems.clear();
    
    for(ItemStack stack : reqItems){
      int req = stack.amount - source.get(stack.item);
      if(req > 0) tempItems.set(stack.item, req);
    }
  
    int move;
    itemFor: for(ItemStack stack : tempItems){
      for(MatrixGrid grid : target.grids){
        for(Building entity : grid.get(Building.class, GridChildType.container, e -> e.items.get(stack.item) > 0)){
          if(tempItems.get(stack.item) <= 0) continue itemFor;
          if(destination.remainingCapacity().intValue() <= 0) break itemFor;
          move = Math.min(entity.items.get(stack.item), tempItems.get(stack.item));
          if(move > 0){
            entity.removeStack(stack.item, move);
            destination.put(stack.item, move);
            tempItems.remove(stack.item, move);
          }
        }
      }
    }
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
