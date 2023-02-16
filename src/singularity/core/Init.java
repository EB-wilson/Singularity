package singularity.core;

import arc.Events;
import arc.struct.Seq;
import dynamilize.DynamicClass;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.input.InputHandler;
import mindustry.world.Block;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.liquid.Conduit;
import singularity.Sgl;
import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglAttribute;

/**改动游戏原内容重初始化，用于对游戏已定义的实例进行操作*/
public class Init{
  public static final DynamicClass InputHandlerAspect = DynamicClass.get("InputHandlerAspect");

  static {
    final SglEventTypes.BuildPlanRotateEvent rotateEvent = new SglEventTypes.BuildPlanRotateEvent();

    InputHandlerAspect.setFunction("rotatePlans", (s, su, a) -> {
      rotateEvent.plans = a.get(0);
      rotateEvent.direction = a.get(1);
      for (BuildPlan plan : rotateEvent.plans) {
        if (plan.block instanceof SglBlock sglBlock){
          sglBlock.onPlanRotate(plan, rotateEvent.direction);
        }
      }
      Events.fire(SglEventTypes.BuildPlanRotateEvent.class, rotateEvent);
      su.invokeFunc("rotatePlans", a);
    }, Seq.class, int.class);
  }

  public static void init(){
    //取代输入处理器
    final InputHandler oldInput = Vars.control.input;
    Vars.control.input = Sgl.classes.getDynamicMaker().newInstance(oldInput.getClass(), InputHandlerAspect).castGet();

    //设置方块及地板属性
    Blocks.stone.attributes.set(SglAttribute.bitumen, 0.5f);

    //禁用所有超速器
    for (Block block : Vars.content.blocks()) {
      if (block instanceof OverdriveProjector over){
        over.placeablePlayer = false;
        over.update = false;
        over.breakable = true;
      }
    }
  }
  
  /**内容重载器，用于对已加载的内容做出变更(或者覆盖)*/
  public static void reloadContent(){
    //为液体装卸器保证不从(常规)导管中提取液体
    for(Block target: Vars.content.blocks()){
      if(target instanceof Conduit) target.unloadable = false;
    }
  }
}
