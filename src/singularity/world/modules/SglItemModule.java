package singularity.world.modules;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.modules.ItemModule;

import java.util.Arrays;

public class SglItemModule extends ItemModule{
  
  public final boolean dyCapacity;
  protected Seq<ItemStack> inventory = new Seq<>();
  protected int[] idMapper = new int[Vars.content.items().size];
  
  public SglItemModule(Item[] accepts){
    dyCapacity = false;
    inventory.setSize(accepts.length);
    Arrays.fill(idMapper, -1);
    for(int i=0; i<accepts.length; i++){
      inventory.set(i, new ItemStack(accepts[i], 0));
      idMapper[accepts[i].id] = i;
    }
  }
  
  public SglItemModule(){
    dyCapacity = true;
  }
  
  protected void updateContain(int item) throws UnacceptedItemInputException{
    if(idMapper[item] == -1){
      if(!dyCapacity) throw new UnacceptedItemInputException("This module's inventory is static, but something try to add an item that unaccepted.");
      idMapper[item] = inventory.size;
      inventory.add(new ItemStack(Vars.content.item(item), 0));
    }
  }
  
  @Override
  public void update(boolean showFlow){
  
  }
  
  @Override
  public SglItemModule copy(){
    SglItemModule out = dyCapacity? new SglItemModule(inventory.map(e -> e.item).toArray()): new SglItemModule();
    out.set(this);
    return out;
  }
  
  @Override
  public void set(ItemModule module){
    if(module instanceof SglItemModule){
      SglItemModule other = (SglItemModule) module;
      total = other.total;
      takeRotation = other.takeRotation;
      inventory = other.inventory.copy();
      System.arraycopy(other.idMapper, 0, idMapper, 0, other.idMapper.length);
    }
  }
  
  @Override
  public int length(){
    return inventory.size;
  }
  
  @Override
  public void each(ItemConsumer cons){
    for(ItemStack stack: inventory){
      cons.accept(stack.item, stack.amount);
    }
  }
  
  @Override
  @Nullable
  public Item first(){
    return inventory.get(0).item;
  }
  
  @Override
  public float sum(ItemCalculator calc){
    float sum = 0f;
    for(int i = 0; i < inventory.size; i++){
      if(inventory.get(i).amount > 0){
        sum += calc.get(inventory.get(i).item, inventory.get(i).amount);
      }
    }
    return sum;
  }
  
  @Override
  @Nullable
  public Item take(){
    for(int i=0; i<inventory.size; i++){
      int index = i + takeRotation;
      index %= inventory.size;
      if(inventory.get(index).amount > 0){
        inventory.get(index).amount--;
        total--;
        takeRotation = index + 1;
        return inventory.get(index).item;
      }
    }
    return null;
  }
  
  @Override
  @Nullable
  public Item takeIndex(int takeRotation){
    for(int i=0; i<inventory.size; i++){
      int index = i + takeRotation;
      index %= inventory.size;
      if(inventory.get(index).amount > 0){
        return inventory.get(index).item;
      }
    }
    return null;
  }
  
  @Override
  public int nextIndex(int takeRotation){
    for(int i=0; i<inventory.size; i++){
      int index = i + takeRotation;
      index %= inventory.size;
      if(inventory.get(index).amount > 0){
        return (takeRotation + i) % inventory.size;
      }
    }
    return takeRotation;
  }
  
  @Override
  public int get(int id){
    return inventory.get(idMapper[id]).amount;
  }
  
  @Override
  public int get(Item item){
    return inventory.get(idMapper[item.id]).amount;
  }
  
  @Override
  public void set(Item item, int amount){
    try{
      updateContain(item.id);
    }
    catch(UnacceptedItemInputException e){
      Log.err(e);
    }
    int delta = amount - inventory.get(idMapper[item.id]).amount;
    inventory.get(idMapper[item.id]).amount += amount;
    total += delta;
  }
  
  @Override
  public void add(Item item, int amount){
    try{
      updateContain(item.id);
    }
    catch(UnacceptedItemInputException e){
      Log.err(e);
    }
    inventory.get(idMapper[item.id]).amount++;
    total += amount;
  }
  
  @Override
  public void add(ItemModule items){
    items.each(this::add);
  }
  
  @Override
  public void remove(Item item, int amount){
    try{
      updateContain(item.id);
    }
    catch(UnacceptedItemInputException e){
      Log.err(e);
    }
    amount = Math.min(inventory.get(idMapper[item.id]).amount, amount);
    
    inventory.get(idMapper[item.id]).amount -= amount;
    total -= amount;
  }
  
   @Override
  public void write(Writes write){
    write.i(inventory.size);
    
    for(int i=0; i<inventory.size; i++){
      write.s(inventory.get(i).item.id);
      write.i(inventory.get(i).amount);
    }
  }

  @Override
  public void read(Reads read, boolean legacy){
    int count = legacy? read.ub(): read.i();
    total = 0;
    
    for(int i=0; i<count; i++){
      int itemid = legacy? read.ub(): read.s();
      int itemamount = read.i();
      inventory.add(new ItemStack(Vars.content.item(itemid), itemamount));
      idMapper[itemid] = i;
      total += itemamount;
    }
  }
  
  static class UnacceptedItemInputException extends Exception{
    public UnacceptedItemInputException(String msg) {
      super(msg);
    }
  }
}