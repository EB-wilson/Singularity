package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.type.ItemStack;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.ItemsBuffer;

public class ReadItemsRequest extends DistRequestBase{
  protected ItemsBuffer itemsBuffer;
  protected ItemsBuffer destination;
  
  public int require;
  public Seq<ItemStack> reqItems;
  
  public ReadItemsRequest(Seq<ItemStack> items, ItemsBuffer dest){
    reqItems = items;
    for(ItemStack s: items){
      require += s.amount;
    }
    destination = dest;
  }
  
  @Override
  public boolean finished(){
    return killed || require <= 0;
  }
  
  @Override
  public int priority(){
    return 50;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    itemsBuffer = target.cores.get(0).distributor().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public void preHandle(){
    itemsBuffer.containerRequire(target, reqItems);
  }
  
  @Override
  public void handle(){
    ItemsBuffer.ItemPacket packet, buffer;
    for(ItemStack stack: reqItems){
      if(stack.amount <= 0) continue;
      packet = new ItemsBuffer.ItemPacket(stack.item, stack.amount);
      buffer = itemsBuffer.get(stack.item.id);
      int move = Math.min(buffer == null? 0: buffer.occupation(), packet.occupation());
      move = Math.min(move, destination.space());
      
      if(move > packet.unit()){
        packet.obj.amount = (int)Math.floor((float)move/packet.unit());
        stack.amount -= packet.obj.amount;
        require -= packet.obj.amount;
        destination.put(stack.item, packet.obj.amount);
        itemsBuffer.remove(packet);
      }
    }
  }
  
}
