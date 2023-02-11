package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.ItemsBuffer;

import java.util.Arrays;

/**从网络中读取物品，此操作将物品从网络缓存读出并写入到目标缓存，网络缓存会优先提供已缓存物品，若不足则从网络子容器申请物品到网络缓存再分配*/
public class ReadItemsRequest extends DistRequestBase{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private static int[] tempItems;

  private final ItemsBuffer destination;
  private ItemsBuffer source;
  
  private final Seq<ItemStack> reqItems;
  
  public ReadItemsRequest(DistElementBuildComp sender, ItemsBuffer destination, Seq<ItemStack> items){
    super(sender);
    this.destination = destination;
    reqItems = items;
  }
  
  @Override
  public int priority(){
    return 128;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    source = target.getCore().distCore().getBuffer(DistBufferType.itemBuffer);
  }
  
  @Override
  public boolean preHandleTask(){
    if (tempItems == null || tempItems.length != Vars.content.items().size){
      tempItems = new int[Vars.content.items().size];
    }

    Arrays.fill(tempItems, 0);
    
    for(ItemStack stack : reqItems){
      tempItems[stack.item.id] = stack.amount - source.get(stack.item);
    }

    itemFor: for(int id = 0; id<tempItems.length; id++){
      if(tempItems[id] <= 0) continue;
      Item item = Vars.content.item(id);
      for(MatrixGrid grid : target.grids){
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(GridChildType.container,
            (e, c) -> e.block.hasItems && e.items != null && e.items.get(item) > 0
                && c.get(GridChildType.container, item),
            temp)){

          if(tempItems[id] <= 0) continue itemFor;
          if(source.remainingCapacity() <= 0) {
            break itemFor;
          }

          int move = Math.min(entry.entity.items.get(item), tempItems[id]);
          move = Math.min(move, source.remainingCapacity());

          if(move > 0){
            move = entry.entity.removeStack(item, move);
            source.put(item, move);
            source.dePutFlow(item, move);
            tempItems[id] -= move;
          }
        }
      }
    }
    
    return true;
  }

  @Override
  protected boolean handleTask(){
    boolean blockTest = false;
    for(ItemStack stack : reqItems){
      int move = Math.min(stack.amount, source.get(stack.item));
      move = Math.min(move, destination.remainingCapacity());
      if(move <= 0) continue;

      source.remove(stack.item, move);
      destination.put(stack.item, move);
      blockTest = true;
    }
    return blockTest;
  }

  @Override
  protected boolean afterHandleTask(){
    return true;
  }
}
