package singularity.type;

import mindustry.type.Category;
import universeCore.util.handler.EnumHandler;
import universeCore.util.handler.FieldHandler;

public class SglCategory{
  private static final EnumHandler<Category> handler = new EnumHandler<>(Category.class);
  
  /**各种生产/传输核能的设备; 消耗核能的工厂应当保持在crafting中*/
  public static final Category nuclear = handler.addEnumItem("nuclear", 5),
  /**调试方块，在配置game debug mod开启时，在沙盒模式可见*/
  debugging = handler.addEnumItemTail("debugging");
  
  public static final Category[] all = {nuclear, debugging};
  
  static{
    FieldHandler.setValue(Category.class, "all", null, Category.values());
  }
  
  public static Category[] values(){
    return all;
  }
}
