package singularity.type;

import arc.struct.ObjectMap;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;

public class Ability extends UnlockableContent{
  public int maxLevel = 1;
  
  protected ObjectMap<Class<?>, Runnable> listener;
  
  public Ability(String name){
    super(name);
  }
  
  @Override
  public ContentType getContentType(){
    return SglContents.ability;
  }
  
  public void trigger(){}
  
  public void listen(){}
}
