package singularity.type;

import arc.struct.Seq;
import mindustry.ctype.ContentType;

import static mindustry.Vars.content;

public class SglContents{
  public static final ContentType ability = SglContentType.ability.value;
  public static final ContentType structure = SglContentType.structure.value;
  public static final ContentType atomSchematic = SglContentType.atomSchematic.value;
  
  public static Seq<Ability> abilities(){
    return content.getBy(ability);
  }
  
  public static Ability ability(int id){
    return content.getByID(ability, id);
  }
  
  public static Ability ability(String name){
    return content.getByName(ability, name);
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
