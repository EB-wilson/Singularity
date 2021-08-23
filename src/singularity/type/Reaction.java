package singularity.type;

import arc.func.Boolf;
import arc.func.Cons;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.Sgl;
import singularity.world.reaction.ReactContainer;

public class Reaction<R1 extends MappableContent, R2 extends MappableContent, P extends MappableContent> extends MappableContent{
  public Participant<R1> reactantA;
  public Participant<R2> reactantB;
  public Participant<P> product;
  
  public float deltaHeat;
  
  public boolean itemReaction = false;
  public boolean liquidReaction = false;
  public boolean gasReaction = false;
  
  public Boolf<ReactContainer> reactRequireDef = entity -> {
    Reaction.Participant<Item>[] rItems = getItem();
    Reaction.Participant<Liquid>[] rLiquid = getLiquid();
    Reaction.Participant<Gas>[] rGas = getGas();
    for(Reaction.Participant<Item> part: rItems){
      if(entity.inItems().get(part.reactant) < part.amount) return false;
    }
    for(Reaction.Participant<Liquid> part: rLiquid){
      if(entity.inLiquids().get(part.reactant) < part.amount) return false;
    }
    for(Reaction.Participant<Gas> part: rGas){
      if(entity.inGases().get(part.reactant) < part.amount) return false;
    }
    return true;
  };
  
  public Cons<ReactContainer> onReactionDef = entity -> {
    Reaction.Participant<Item>[] rItems = getItem();
    Reaction.Participant<Liquid>[] rLiquid = getLiquid();
    Reaction.Participant<Gas>[] rGas = getGas();
    for(Reaction.Participant<Item> part: rItems){
      entity.inItems().remove(part.reactant, (int)part.amount);
    }
    for(Reaction.Participant<Liquid> part: rLiquid){
      entity.inLiquids().remove(part.reactant, (int)part.amount);
    }
    for(Reaction.Participant<Gas> part: rGas){
      entity.inGases().remove(part.reactant, (int)part.amount);
    }
    if(product.isItem) entity.outItems().add((Item)product.reactant, (int)product.amount);
    if(product.isLiquid) entity.outLiquids().add((Liquid)product.reactant, product.amount);
    if(product.isGas) entity.outGases().add((Gas)product.reactant, product.amount);
  };
  
  private byte iReactionCount = -1;
  private byte lReactionCount = -1;
  private byte gReactionCount = -1;
  
  public Reaction(Participant<R1> a, Participant<R2> b, Participant<P> out){
    super(a.toString() + " + " + b.toString() + " = " + out.toString());
    reactantA = a;
    reactantB = b;
    product = out;
  }
  
  public Reaction(R1 a, float b, R2 c, float d, P e, float f){
    this(new Participant<>(a, b), new Participant<>(c, d), new Participant<>(e, f));
  }
  
  /**自定义项目*/
  public Reaction(String name){
    super(name);
  }
  
  @Override
  public void init(){
    super.init();
    Sgl.reactions.signupReaction(this);
    if(reactantA.reactant instanceof Item) iReactionCount++;
    if(reactantB.reactant instanceof Item) if(++iReactionCount >= 1) itemReaction = true;
    if(reactantA.reactant instanceof Liquid) lReactionCount++;
    if(reactantB.reactant instanceof Liquid) if(++lReactionCount >= 1) liquidReaction = true;
    if(reactantA.reactant instanceof Gas) gReactionCount++;
    if(reactantB.reactant instanceof Gas) if(++gReactionCount >= 1) gasReaction = true;
  }
  
  public void doReact(ReactContainer entity, Boolf<ReactContainer> reactRequire, Cons<ReactContainer> onReaction){
    if(reactRequire.get(entity)){
      onReaction.get(entity);
    }
  }
  
  public void doReact(ReactContainer entity){
    doReact(entity, reactRequireDef, onReactionDef);
  }
  
  public Participant<?>[] getAllPart(){
    return new Participant[]{reactantA, reactantB, product};
  }
  
  public boolean accept(MappableContent target){
    return reactantA.reactant == target || reactantB.reactant == target;
  }
  
  @SuppressWarnings("unchecked")
  public Participant<Item>[] getItem(){
    if(iReactionCount == -1) return new Participant[]{};
    if(itemReaction) return new Participant[]{reactantA, reactantB};
    Participant<?>[] result = new Participant[1];
    if(reactantA.reactant instanceof Item){
      result[0] = reactantA;
    }
    else result[0] = reactantB;
    return (Participant<Item>[])result;
  }
  
  @SuppressWarnings("unchecked")
  public Participant<Liquid>[] getLiquid(){
    if(lReactionCount == -1) return new Participant[]{};
    if(liquidReaction) return new Participant[]{reactantA, reactantB};
    Participant<?>[] result = new Participant[1];
    if(reactantA.reactant instanceof Liquid){
      result[0] = reactantA;
    }
    else result[0] = reactantB;
    return (Participant<Liquid>[])result;
  }
  
  @SuppressWarnings("unchecked")
  public Participant<Gas>[] getGas(){
    if(gReactionCount == -1) return new Participant[]{};
    if(gasReaction) return new Participant[]{reactantA, reactantB};
    Participant<?>[] result = new Participant[1];
    if(reactantA.reactant instanceof Gas){
      result[0] = reactantA;
    }
    else result[0] = reactantB;
    return (Participant<Gas>[])result;
  }
  
  @Override
  public ContentType getContentType(){
    return SglContentType.reaction.value;
  }
  
  public static class Participant<Type extends MappableContent>{
    public final Type reactant;
    public final Class<Type> clazz;
    public final float amount;
    
    public boolean isItem;
    public boolean isLiquid;
    public boolean isGas;
    
    @SuppressWarnings("unchecked")
    public Participant(Type reactant, float amount){
      if(reactant instanceof Item || reactant instanceof Liquid || reactant instanceof Gas){
        isItem = reactant instanceof Item;
        isLiquid = reactant instanceof Liquid;
        isGas = reactant instanceof Gas;
        this.reactant = reactant;
        this.clazz = (Class<Type>) reactant.getClass();
        this.amount = amount;
      }
      else{
        throw new RuntimeException("Error participant! class >" + reactant.getClass().getName() + "< can not apply to reaction");
      }
    }
    
    public Type get(){
      return reactant;
    }
  
    @Override
    public String toString(){
      return amount + reactant.name;
    }
  }
}
