package singularity.type;

import arc.struct.Seq;
import mindustry.ctype.ContentType;

import static mindustry.Vars.content;

public class SglContents{
  public static final ContentType gas = SglContentType.gas.value;
  public static final ContentType ability = SglContentType.ability.value;
  public static final ContentType structure = SglContentType.structure.value;
  public static final ContentType reaction = SglContentType.reaction.value;
  public static final ContentType atomSchematic = SglContentType.atomSchematic.value;

  public static Seq<Gas> gases(){
    return content.getBy(gas);
  }
  
  public static Gas gas(int id){
    return content.getByID(gas, id);
  }
  
  public static Gas gas(String name){
    return content.getByName(gas, name);
  }
  
  public static Seq<Ability> abilities(){
    return content.getBy(ability);
  }
  
  public static Ability ability(int id){
    return content.getByID(ability, id);
  }
  
  public static Ability ability(String name){
    return content.getByName(ability, name);
  }
  
  public static Seq<Reaction<?, ?>> structures(){
    return content.getBy(structure);
  }
  
  public static Reaction<?, ?> structure(int id){
    return content.getByID(structure, id);
  }
  
  public static Reaction<?, ?> structure(String name){
    return content.getByName(structure, name);
  }
  
  public static Seq<Reaction<?, ?>> reactions(){
    return content.getBy(reaction);
  }
  
  public static Reaction<?, ?> reaction(int id){
    return content.getByID(reaction, id);
  }
  
  public static Reaction<?, ?> reaction(String name){
    return content.getByName(reaction, name);
  }

  public static Seq<AtomSchematic> atomSchematics(){
    return content.getBy(atomSchematic);
  }

  public static AtomSchematic atomSchematic(int id){
    return content.getByID(atomSchematic, id);
  }

  public static AtomSchematic atomSchematic(String name){
    return content.getByName(atomSchematic, name);
  }
}
