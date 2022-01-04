package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.IntMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

public class ItemsBuffer extends BaseBuffer<ItemStack, ItemsBuffer.ItemPacket>{
  public int[] containerRequired = new int[Vars.content.items().size];
  
  public void put(Item item, int amount){
    put(new ItemsBuffer.ItemPacket(item, amount));
  }
  
  public void remove(Item item, int amount){
    remove(new ItemsBuffer.ItemPacket(item, amount));
  }
  
  public void remove(Item item){
    remove(item.id);
  }
  
  public float get(Item item){
    ItemPacket p = get(item.id);
    return p != null? p.obj.amount: 0;
  }
  
  @Override
  public Integer remainingCapacity(){
    return space()/8;
  }
  
  @Override
  public void containerPut(DistributeNetwork network){
    for(IntMap.Entry<ItemPacket> entry : memory){
      ItemStack stack = entry.value.obj;
      for(MatrixGrid grid :network.grids){
        if(stack.amount <= 0) break;
        Building entity = grid.handler.getBuilding();
        for(Building building : grid.get(Building.class, GridChildType.container, e -> e.acceptItem(entity, stack.item))){
          int amount = Math.min(building.block.itemCapacity - building.items.get(stack.item), stack.amount);
          stack.amount -= amount;
          entity.items.add(stack.item, amount);
        }
      }
    }
  }
  
  @Override
  public void containerRequire(DistributeNetwork network, Seq<ItemStack> requires){
    ItemPacket packet, buffer;
    item:for(ItemStack stack: requires){
      containerRequired[stack.item.id] += stack.amount;
      for(MatrixGrid grid: network.grids){
        if(get(stack.item) >= containerRequired[stack.item.id] || space() <= 0) continue item;
        
        for(Building entity: grid.get(Building.class, GridChildType.container, e -> e.items.has(stack.item))){
          if(get(stack.item) >= containerRequired[stack.item.id] || space() <= 0) continue item;
          packet = new ItemPacket(stack.item, stack.amount);
          packet.obj.amount = Math.min(space(), packet.occupation());
          packet.obj.amount = (int)Math.floor((float)Math.min(entity.items.get(stack.item)*packet.unit(), packet.occupation())/packet.unit());
          
          entity.items.remove(stack.item, packet.obj.amount);
          buffer = memory.get(stack.item.id);
          if(buffer == null){
            memory.put(stack.item.id, packet);
          }
          else buffer.obj.amount += packet.obj.amount;
          
          used += packet.occupation();
        }
      }
    }
  }
  
  public static class ItemPacket extends Packet<ItemStack>{
    WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
    float putCaching, readCaching;
    float putRate = -1, readRate= -1;
    
    public ItemPacket(Item item, int amount){
      obj = new ItemStack(item, amount);
      putCaching += amount;
    }
  
    @Override
    public int unit(){
      return 8;
    }
  
    @Override
    public int id(){
      return obj.item.id;
    }
  
    @Override
    public int occupation(){
      return obj.amount*unit();
    }
    
    @Override
    public void merge(Packet<ItemStack> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
    
    @Override
    public void remove(Packet<ItemStack> other){
      if(other.id() == id()){
        obj.amount -= other.obj.amount;
        readCaching += obj.amount;
      }
    }
    
    @Override
    public void calculateDelta(){
      putMean.add(putCaching);
      putCaching = 0;
      if(putMean.hasEnoughData()) putRate = putMean.mean();
      
      readMean.add(readCaching);
      readCaching = 0;
      if(readMean.hasEnoughData()) readRate = readMean.mean();
    }
    
    @Override
    public float delta(){
      return 0;
    }
  }
}
