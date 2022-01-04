package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.type.ItemStack;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.buffers.ItemsBuffer;

public class PutItemsRequest extends DistRequestBase{
  protected ItemsBuffer itemsBuffer;
  protected ItemsBuffer source;
  
  public int require;
  public int[] reqItems = new int[Vars.content.items().size];
  
  public PutItemsRequest(Seq<ItemStack> items, ItemsBuffer source){
    for(ItemStack s: items){
      reqItems[s.item.id] = s.amount;
      require += s.amount;
    }
    this.source = source;
  }
  
  @Override
  public boolean finished(){
    return killed || require <= 0;
  }
  
  @Override
  public int priority(){
    return 100;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    itemsBuffer = target.cores.get(0).distributor().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public void handle(){
    ItemsBuffer.ItemPacket packet, sourceBuffer;
    for(int i=0; i<reqItems.length; i++){
      if(reqItems[i] <= 0) continue;
      packet = new ItemsBuffer.ItemPacket(Vars.content.item(i), reqItems[i]);
      int move = Math.min(itemsBuffer.space(), packet.occupation());
      sourceBuffer = source.get(i);
      move = Math.min(move, sourceBuffer == null? 0: sourceBuffer.occupation());
      
      if(move > packet.unit()){
        packet.obj.amount = (int)Math.floor((float)move/packet.unit());
        reqItems[i] -= packet.obj.amount;
        require -= packet.obj.amount;
        source.remove(Vars.content.item(i), packet.obj.amount);
        itemsBuffer.put(packet);
      }
    }
  }
  
}
