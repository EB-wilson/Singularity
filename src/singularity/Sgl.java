package singularity;

import singularity.world.atmosphere.Atmospheres;
import singularity.world.atmosphere.GasAreas;
import singularity.world.reaction.ReactionPoints;
import singularity.world.reaction.Reactions;

public class Sgl{
  /**所有大气的全局存储对象，提供了关于大气层的一些参数与操作*/
  public static Atmospheres atmospheres;
  /**气体云的全局存储对象，提供了气体散逸成云的功能和关于气体云的集中操作*/
  public static GasAreas gasAreas;
  /**反应的全局存储对象，保存了所有的反应类型，并提供了匹配反应的方法*/
  public static Reactions reactions;
  /**所有反应点的全局存储对象，用于保存和统一操作反应点*/
  public static ReactionPoints reactionPoints;
  
  public static void load(){
    atmospheres = new Atmospheres();
    gasAreas = new GasAreas();
    reactions = new Reactions();
    reactionPoints = new ReactionPoints();
  }
}
