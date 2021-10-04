package singularity.core;

import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.ctype.MappableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.type.Gas;
import singularity.type.Reaction;

/**化学反应组管理类*/
public class Reactions{
  public final IntMap<IntMap<Reaction<?, ?, ?>>[]> itemReactMap = new IntMap<>();
  public final IntMap<IntMap<Reaction<?, ?, ?>>[]> liquidReactMap = new IntMap<>();
  public final IntMap<IntMap<Reaction<?, ?, ?>>[]> gasReactMap = new IntMap<>();
  
  protected final Seq<Item> allReactItem = new Seq<>();
  protected final Seq<Liquid> allReactLiquid = new Seq<>();
  protected final Seq<Gas> allReactGases = new Seq<>();
  
  @SuppressWarnings("unchecked")
  public <RA extends MappableContent, RB extends MappableContent> Reaction<RA, RB, ?> match(RA a, RB b){
    RuntimeException exception = new RuntimeException("try use invalid type to get a reaction");
    int type = b instanceof Item? 0: b instanceof Liquid? 1: b instanceof Gas? 2: -1;
    if(type == -1) throw exception;
    
    IntMap<Reaction<?, ?, ?>> map;
    if(a instanceof Item){
      if(!allReactItem.contains((Item)a)) return null;
      map = itemReactMap.get(a.id)[type];
    }
    else if(a instanceof Liquid){
      if(!allReactLiquid.contains((Liquid)a)) return null;
      map = liquidReactMap.get(a.id)[type];
    }
    else if(a instanceof Gas){
      if(!allReactGases.contains((Gas)a)) return null;
      map = gasReactMap.get(a.id)[type];
    }
    else throw exception;
    
    if(map != null) return (Reaction<RA, RB, ?>)map.get(b.id);
    return null;
  }
  
  public void signupReaction(Reaction<?, ?, ?> reaction){
    Log.info("singing: " + reaction);
    if(reaction.reactantA.reactant instanceof Item) signupItem((Item)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantB.reactant instanceof Item) signupItem((Item)reaction.reactantB.reactant, reaction.reactantA.reactant, reaction);
    if(reaction.reactantA.reactant instanceof Liquid) signupLiquid((Liquid)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantB.reactant instanceof Liquid) signupLiquid((Liquid)reaction.reactantB.reactant, reaction.reactantA.reactant, reaction);
    if(reaction.reactantA.reactant instanceof Gas) signupGas((Gas)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantB.reactant instanceof Gas) signupGas((Gas)reaction.reactantB.reactant, reaction.reactantA.reactant, reaction);
  }
  
  @SuppressWarnings("unchecked")
  private void signupItem(Item item, MappableContent key, Reaction<?, ?, ?> reaction){
    int type = key instanceof Item? 0: key instanceof Liquid? 1: key instanceof Gas? 2: -1;
    if(type == -1) throw new RuntimeException("try use invalid type to get a reaction");
    IntMap<Reaction<? ,? ,?>>[] arr = itemReactMap.get(item.id, new IntMap[]{new IntMap<>(), new IntMap<>(), new IntMap<>()});
    if(!itemReactMap.containsValue(arr, true)) itemReactMap.put(item.id, arr);
    
    arr[type].put(key.id, reaction);
    
    if(!allReactItem.contains(item)) allReactItem.add(item);
  }
  
  @SuppressWarnings("unchecked")
  private void signupLiquid(Liquid liquid, MappableContent key, Reaction<?, ?, ?> reaction){
    int type = key instanceof Item? 0: key instanceof Liquid? 1: key instanceof Gas? 2: -1;
    if(type == -1) throw new RuntimeException("try use invalid type to get a reaction");
    IntMap<Reaction<? ,? ,?>>[] arr = liquidReactMap.get(liquid.id, new IntMap[]{new IntMap<>(), new IntMap<>(), new IntMap<>()});
    if(!liquidReactMap.containsValue(arr, true)) liquidReactMap.put(liquid.id, arr);
    
    arr[type].put(key.id, reaction);
    
    if(!allReactLiquid.contains(liquid)) allReactLiquid.add(liquid);
  }
  
  @SuppressWarnings("unchecked")
  private void signupGas(Gas gas, MappableContent key, Reaction<?, ?, ?> reaction){
    int type = key instanceof Item? 0: key instanceof Liquid? 1: key instanceof Gas? 2: -1;
    if(type == -1) throw new RuntimeException("try use invalid type to get a reaction");
    IntMap<Reaction<? ,? ,?>>[] arr = gasReactMap.get(gas.id, new IntMap[]{new IntMap<>(), new IntMap<>(), new IntMap<>()});
    if(!gasReactMap.containsValue(arr, true)) gasReactMap.put(gas.id, arr);
    
    arr[type].put(key.id, reaction);
  
    if(!allReactGases.contains(gas)) allReactGases.add(gas);
  }
}
