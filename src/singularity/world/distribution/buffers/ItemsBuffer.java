package singularity.world.distribution.buffers;

import arc.math.WindowedMean;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.modules.ItemModule;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

import static mindustry.Vars.content;

public class ItemsBuffer extends BaseBuffer<ItemStack, Item, ItemsBuffer.ItemPacket>{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();

  public void put(Item item, int amount){
    put(new ItemsBuffer.ItemPacket(item, amount));
  }
  
  public void remove(Item item, int amount){
    remove(new ItemsBuffer.ItemPacket(item, amount));
  }
  
  public void remove(Item item){
    remove(item.id);
  }
  
  public int get(Item item){
    ItemPacket p = get(item.id);
    return p != null? p.amount(): 0;
  }
  
  @Override
  public void bufferContAssign(DistributeNetwork network){
    itemRead: for(ItemPacket packet : this){
      for(MatrixGrid grid : network.grids){
        Building handler = grid.handler.getBuilding();
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(
            GridChildType.container,
            (e, c) -> e.acceptStack(packet.get(), packet.amount(), handler) > 0 && c.get(GridChildType.container, packet.get()),
            temp)){
          if(packet.amount() <= 0) continue itemRead;
          int amount = Math.min(packet.amount(), entry.entity.acceptStack(packet.get(), packet.amount(), handler));

          packet.remove(amount);
          entry.entity.handleStack(packet.get(), amount, handler);
        }
      }
    }
  }
  
  @Override
  public int unit(){
    return 8;
  }
  
  @Override
  public ItemModule generateBindModule(){
    return new BufferItemModule();
  }
  
  private final ItemPacket tmp = new ItemPacket(Items.copper, 0);
  
  public class ItemPacket extends Packet<ItemStack, Item>{
    WindowedMean putMean = new WindowedMean(6), readMean = new WindowedMean(6);
    float putCaching, readCaching;
    float putRate = -1, readRate= -1;
    
    public ItemPacket(Item item, int amount){
      obj = new ItemStack(item, amount);
      putCaching += amount;
    }
  
    @Override
    public int id(){
      return obj.item.id;
    }
  
    @Override
    public Item get(){
      return obj.item;
    }
  
    @Override
    public int occupation(){
      return obj.amount*unit();
    }
  
    @Override
    public Integer amount(){
      return obj.amount;
    }
  
    @Override
    public void merge(Packet<ItemStack, Item> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += obj.amount;
      }
    }
    
    public void remove(int amount){
      tmp.obj.item = obj.item;
      tmp.obj.amount = amount;
      ItemsBuffer.this.remove(tmp);
    }
    
    @Override
    public void remove(Packet<ItemStack, Item> other){
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
  
  public class BufferItemModule extends ItemModule{
    @Override
    public void add(ItemModule items){
      items.each(ItemsBuffer.this::put);
    }
    
    @Override
    public void add(Item item, int amount){
      ItemsBuffer.this.put(item, amount);
    }
    
    @Override
    public int get(int id){
      return get(Vars.content.item(id));
    }
    
    @Override
    public int get(Item item){
      return ItemsBuffer.this.get(item);
    }
    
    @Override
    public void remove(Item item, int amount){
      ItemsBuffer.this.remove(item, amount);
    }
  
    @Override
    public void set(Item item, int amount){
      ItemsBuffer.this.set(new ItemPacket(item, amount));
    }
  
    @Override
    public void set(ItemModule other){
      other.each(this::set);
    }
  
    @Override
    public int total(){
      return ItemsBuffer.this.usedCapacity().intValue();
    }
    
    @Override
    public boolean empty(){
      return total() == 0;
    }
    
    @Override
    public boolean any(){
      return total() > 0;
    }
    
    @Override
    @Nullable
    public Item first(){
      for(int i = 0; i < items.length; i++){
        if(get(i) > 0){
          return content.item(i);
        }
      }
      return null;
    }
  
    @Override
    @Nullable
    public Item take(){
      for(int i = 0; i < items.length; i++){
        int index = (i + takeRotation);
        if(index >= items.length) index -= items.length;
        if(get(index) > 0){
          Item item = content.item(index);
          remove(item, 1);
          takeRotation = index + 1;
          return item;
        }
      }
      return null;
    }
    
    @Override
    @Nullable
    public Item takeIndex(int takeRotation){
      for(int i = 0; i < items.length; i++){
        int index = (i + takeRotation);
        if(index >= items.length) index -= items.length;
        if(get(index) > 0){
          return content.item(index);
        }
      }
      return null;
    }
    
    @Override
    public int nextIndex(int takeRotation){
      for(int i = 1; i < items.length; i++){
        int index = (i + takeRotation);
        if(index >= items.length) index -= items.length;
        if(get(index) > 0){
          return (takeRotation + i) % items.length;
        }
      }
      return takeRotation;
    }
  
    @Override
    public void each(ItemConsumer cons){
      for(ItemPacket packet : ItemsBuffer.this){
        cons.accept(packet.get(), packet.amount());
      }
    }
  
    @Override
    public float sum(ItemCalculator calc){
      float sum = 0f;
      for(ItemsBuffer.ItemPacket packet: ItemsBuffer.this){
        sum += calc.get(packet.get(), packet.amount());
      }
      return sum;
    }
  
    @Override
    public void read(Reads read, boolean l){
      memory = new IntMap<>();
      used = 0;
      int length = l? read.ub(): read.s();
      for(int i = 0; i < length; i++){
        int id = l? read.ub(): read.s();
        int amount = read.i();
        put(content.item(id), amount);
      }
    }
  
    @Override
    public void write(Writes write){
      write.s(memory.size);
      for(ItemPacket value : memory.values()){
        write.s(value.id());
        write.i(value.amount());
      }
    }
  }
}
