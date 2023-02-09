package singularity.util;

import arc.func.Floatc2;
import arc.math.geom.Vec2;
import arc.util.Tmp;

public class MathTransform{
  private static final Vec2 tmp = new Vec2();
  
  /**使用极坐标形式的傅里叶级数计算向量坐标
   *
   * @param time 为傅里叶级数传入的参数（或者插值）
   * @param params 参数组，每三个数据确定一个sin函数，参数格式：{@code {角速度, 初相位, 极值, ...}}
   * @return 指定的傅里叶级数在给定插值下计算的向量，为正交坐标系形式*/
  public static Vec2 fourierTransform(float time, float... params){
    tmp.setZero();
    for(int i = 0; i < params.length; i+=3){
      float w = params[i];
      float f = params[i + 1];
      float l = params[i + 2];

      tmp.add(Tmp.v1.set(l, 0).setAngle(f + time*w));
    }

    return tmp;
  }

  /**使用极坐标形式的傅里叶级数计算向量坐标，计算结果通过函数回调
   *
   * @param transRecall 计算结果的回调函数
   * @param time 为傅里叶级数传入的参数（或者插值）
   * @param params 参数组，每三个数据确定一个sin函数，参数格式：{@code {角速度, 初相位, 极值, ...}}*/
  public static void fourierTransform(Floatc2 transRecall, float time, float... params){
    Vec2 v = fourierTransform(time, params);
    transRecall.get(v.x, v.y);
  }
}
