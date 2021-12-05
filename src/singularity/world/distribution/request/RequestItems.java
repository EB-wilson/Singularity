package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.modules.ItemModule;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

import java.util.Arrays;

public class RequestItems extends DistRequestBase{
  private static final int[] temp = new int[Vars.content.items().size];
  
  protected ItemModule itemsBuffer;
  protected ItemModule getItems;
  
  public Seq<ItemStack> reqItems;
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    itemsBuffer = target.cores.get(0).distributor().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public void handle(){
    Arrays.fill(temp, 0);
    for(ItemStack stack: reqItems){
      int bufferAmount = itemsBuffer.get(stack.item);
      temp[stack.item.id] = Math.max(stack.amount - bufferAmount, 0);
      itemsBuffer.remove(stack.item, Math.min(bufferAmount, stack.amount));
    }
    
    int amount;
    for(int i=0; i<temp.length; i++){
      amount = temp[i];
      if(amount <= 0) continue;
      Item item = Vars.content.item(i);
      
      for(MatrixGrid grid: target.grids){
        if(amount <= 0) break;
        Building next = grid.get(Building.class, GridChildType.container, e -> e.block.hasItems && e.items.get(item) > 0);
        int a = Math.min(amount, next.items.get(item));
        next.items.remove(item, a);
        amount -= a;
      }
    }
  }
  
  @Override
  public boolean valid(){
    for(ItemStack stack: reqItems){
      if(itemsBuffer.get(stack.item) > 0) return true;
    }
    
    return false;
  }
}
