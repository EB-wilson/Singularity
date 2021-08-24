package singularity.core;

import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.MappableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.type.Gas;
import singularity.type.Reaction;
import singularity.type.SglContentType;

public class Reactions{
  public final Seq<IntMap<Reaction<?, ?, ?>>[]> itemReactMap = new Seq<>(Vars.content.items().size);
  public final Seq<IntMap<Reaction<?, ?, ?>>[]> liquidReactMap = new Seq<>(Vars.content.liquids().size);
  public final Seq<IntMap<Reaction<?, ?, ?>>[]> gasReactMap = new Seq<>(Vars.content.getBy(SglContentType.gas.value).size);
  
  @SuppressWarnings("unchecked")
  public <RA extends MappableContent, RB extends MappableContent> Reaction<RA, RB, ?> match(RA a, RB b){
    RuntimeException exception = new RuntimeException("try use invalid type to get a reaction");
    int type = b instanceof Item? 0: b instanceof Liquid? 1: b instanceof Gas? 2: -1;
    if(type == -1) throw exception;
    
    IntMap<Reaction<?, ?, ?>> map;
    if(a instanceof Item){
      map = itemReactMap.get(a.id)[type];
    }
    else if(a instanceof Liquid){
      map = liquidReactMap.get(a.id)[type];
    }
    else if(a instanceof Gas){
      map = gasReactMap.get(a.id)[type];
    }
    else throw exception;
    
    if(map != null) return (Reaction<RA, RB, ?>)map.get(b.id);
    return null;
  }
  
  public void signupReaction(Reaction<?, ?, ?> reaction){
    if(reaction.reactantA.reactant instanceof Item) signupItem((Item)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantB.reactant instanceof Item) signupItem((Item)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantA.reactant instanceof Liquid) signupLiquid((Liquid)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantB.reactant instanceof Liquid) signupLiquid((Liquid)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantA.reactant instanceof Gas) signupGas((Gas)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
    if(reaction.reactantB.reactant instanceof Gas) signupGas((Gas)reaction.reactantA.reactant, reaction.reactantB.reactant, reaction);
  }
  
  private void signupItem(Item item, MappableContent key, Reaction<?, ?, ?> reaction){
    int type = key instanceof Item? 0: key instanceof Liquid? 1: key instanceof Gas? 2: -1;
    if(type == -1) throw new RuntimeException("try use invalid type to get a reaction");
    IntMap<Reaction<? ,? ,?>> map = itemReactMap.get(item.id)[type];
    if(map == null){
      map = new IntMap<>();
      itemReactMap.get(item.id)[type] = map;
    }
    map.put(key.id, reaction);
  }
  
  private void signupLiquid(Liquid liquid, MappableContent key, Reaction<?, ?, ?> reaction){
    int type = key instanceof Item? 0: key instanceof Liquid? 1: key instanceof Gas? 2: -1;
    if(type == -1) throw new RuntimeException("try use invalid type to get a reaction");
    IntMap<Reaction<? ,? ,?>> map = liquidReactMap.get(liquid.id)[type];
    if(map == null){
      map = new IntMap<>();
      liquidReactMap.get(liquid.id)[type] = map;
    }
    map.put(key.id, reaction);
  }
  
  private void signupGas(Gas gas, MappableContent key, Reaction<?, ?, ?> reaction){
    int type = key instanceof Item? 0: key instanceof Liquid? 1: key instanceof Gas? 2: -1;
    if(type == -1) throw new RuntimeException("try use invalid type to get a reaction");
    IntMap<Reaction<? ,? ,?>> map = gasReactMap.get(gas.id)[type];
    if(map == null){
      map = new IntMap<>();
      gasReactMap.get(gas.id)[type] = map;
    }
    map.put(key.id, reaction);
  }
}
