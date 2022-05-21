package singularity.world.distribution.buffers;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.ItemModule;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;

import static mindustry.Vars.content;

public class ItemsBuffer extends BaseBuffer<ItemStack, Item, ItemsBuffer.ItemPacket>{
  private final ItemPacket tmp = new ItemPacket(Items.copper, 0);
  Seq<MatrixGrid.BuildingEntry<CoreBlock.CoreBuild>> cores = new Seq<>();

  public void put(Item item, int amount){
    tmp.obj.item = item;
    tmp.obj.amount = amount;
    put(tmp);
  }
  
  public void remove(Item item, int amount){
    tmp.obj.item = item;
    tmp.obj.amount = amount;
    remove(tmp);
  }
  
  public void remove(Item item){
    remove(item.id);
  }
  
  public int get(Item item){
    ItemPacket p = get(item.id);
    return p != null? p.amount(): 0;
  }

  @Override
  public void deReadFlow(Item ct, Number amount){
    tmp.obj.item = ct;
    tmp.obj.amount = amount.intValue();
    deReadFlow(tmp);
  }

  @Override
  public void dePutFlow(Item ct, Number amount){
    tmp.obj.item = ct;
    tmp.obj.amount = amount.intValue();
    dePutFlow(tmp);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void bufferContAssign(DistributeNetwork network){
    itemRead: for(ItemPacket packet : this){
      cores.clear();
      for(MatrixGrid grid : network.grids){
        Building handler = grid.handler.getBuilding();
        for(MatrixGrid.BuildingEntry<? extends Building> entry: grid.<Building>get(
            GridChildType.container,
            (e, c) -> e.acceptItem(handler, packet.get()) && c.get(GridChildType.container, packet.get()))){
          if(entry.entity instanceof CoreBlock.CoreBuild) cores.add((MatrixGrid.BuildingEntry<CoreBlock.CoreBuild>) entry);

          if(packet.amount() <= 0) continue itemRead;
          int amount = Math.min(packet.amount(), entry.entity.acceptStack(packet.get(), packet.amount(), handler));

          packet.remove(amount);
          packet.deRead(amount);
          entry.entity.handleStack(packet.get(), amount, handler);
        }
      }

      //最后，分配额外的核心物资用来统计地区资源增量
      for(MatrixGrid.BuildingEntry<CoreBlock.CoreBuild> core: cores){
        if(packet.amount() <= 0) return;
        int amount = Math.min(packet.amount(), core.entity.acceptStack(packet.get(), packet.amount(), network.getCore().getBuilding()));
        core.entity.handleStack(packet.get(), amount, network.getCore().getBuilding());
        packet.remove(amount);
        packet.deRead(amount);
      }
    }
  }

  @Override
  public void bufferContAssign(DistributeNetwork network, Item ct){
    bufferContAssign(network, ct, get(ct));
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public void bufferContAssign(DistributeNetwork network, Item ct, Number amount){
    cores.clear();
    int counter = amount.intValue();

    ItemPacket packet = get(ct.id);
    if(packet == null) return;
    for(MatrixGrid grid: network.grids){
      for(MatrixGrid.BuildingEntry<? extends Building> entry: grid.<Building>get(GridChildType.container, (e, c) -> c.get(GridChildType.container, ct)
          && e.acceptItem(grid.handler.getBuilding(), ct))){

        if(entry.entity instanceof CoreBlock.CoreBuild) cores.add((MatrixGrid.BuildingEntry<CoreBlock.CoreBuild>) entry);

        if(packet.amount() <= 0 || counter <= 0) return;
        int move = Math.min(packet.amount(), entry.entity.acceptStack(packet.get(), packet.amount(), grid.handler.getBuilding()));
        move = Math.min(move, counter);

        packet.remove(move);
        packet.deRead(move);
        counter -= move;
        entry.entity.handleStack(packet.get(), move, grid.handler.getBuilding());
      }
    }

    //最后，分配额外的核心物资用来统计地区资源增量
    if(counter > 0){
      for(MatrixGrid.BuildingEntry<CoreBlock.CoreBuild> entry: cores){
        if(counter <= 0) return;
        int rem = Math.min(packet.amount(), counter);
        rem = Math.min(entry.entity.acceptStack(packet.get(), packet.amount(), network.getCore().getBuilding()), rem);
        entry.entity.handleStack(ct, rem, network.getCore().getBuilding());
        packet.remove(rem);
        packet.deRead(rem);
        counter -= rem;
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

  @Override
  public String localization(){
    return Core.bundle.get("misc.item");
  }

  @Override
  public Color displayColor(){
    return Pal.accent;
  }

  public class ItemPacket extends Packet<ItemStack, Item>{
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
    public Color color(){
      return obj.item.color;
    }

    @Override
    public String localization(){
      return obj.item.localizedName;
    }

    @Override
    public TextureRegion icon(){
      return obj.item.fullIcon;
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
    public void setZero(){
      readCaching += occupation();
      obj.amount = 0;
    }
  
    @Override
    public void merge(Packet<ItemStack, Item> other){
      if(other.id() == id()){
        obj.amount += other.obj.amount;
        putCaching += other.occupation();
      }
    }

    public void put(int amount){
      tmp.obj.item = obj.item;
      tmp.obj.amount = amount;
      ItemsBuffer.this.put(tmp);
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
        readCaching += other.occupation();
      }
    }

    public void deRead(int amount){
      tmp.obj.item = obj.item;
      tmp.obj.amount = amount;
      ItemsBuffer.this.deReadFlow(tmp);
    }

    public void dePut(int amount){
      tmp.obj.item = obj.item;
      tmp.obj.amount = amount;
      ItemsBuffer.this.dePutFlow(tmp);
    }

    @Override
    public Packet<ItemStack, Item> copy(){
      return new ItemPacket(obj.item, obj.amount);
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
