package singularity.world;

import arc.math.geom.Mat3D;
import arc.math.geom.Quat;
import arc.math.geom.Vec3;

import static universecore.annotations.Annotations.BindField;

public interface Transform {
  @BindField(value = "tmpQuat", initialize = "new Quat()")
  default Quat tmpQuat() { return null; }

  @BindField("x")
  default float getX(){ return 0; }
  @BindField("y")
  default float getY(){ return 0; }
  @BindField("z")
  default float getZ(){ return 0; }
  default Vec3 getPos(Vec3 result){
    return result.set(getX(), getY(), getZ());
  }
  @BindField("x")
  default void setX(float x){}
  @BindField("y")
  default void setY(float y){}
  @BindField("z")
  default void setZ(float z){}
  default void set(float x, float y, float z){
    setX(x);
    setY(y);
    setZ(z);
  }
  default void set(Vec3 vec3){
    set(vec3.x, vec3.y, vec3.z);
  }
  default void transform(float x, float y, float z){
    set(getX() + x, getY() + y, getZ() + z);
  }
  default void transform(Vec3 vec3) {
    transform(vec3.x, vec3.y, vec3.z);
  }

  @BindField("eulerX")
  default float getEulerX(){ return 0; }
  @BindField("eulerY")
  default float getEulerY(){ return 0; }
  @BindField("eulerZ")
  default float getEulerZ(){ return 0; }
  default Quat getEuler(Quat result){
    return getRotation(result);
  }
  @BindField("eulerX")
  void setEulerX(float x);
  @BindField("eulerY")
  void setEulerY(float y);
  @BindField("eulerZ")
  void setEulerZ(float z);
  default void setEuler(float x, float y, float z){
    setEulerX(x);
    setEulerY(y);
    setEulerZ(z);
  }
  default void setEuler(Vec3 vec3) {
    setEuler(vec3.x, vec3.y, vec3.z);
  }
  default void rotate(float x, float y, float z){
    setEuler(getEulerX() + x, getEulerY() + y, getEulerZ() + z);
  }
  default void rotate(Vec3 vec3){
    rotate(vec3.x, vec3.y, vec3.z);
  }

  @BindField("scaleX")
  default float getScaleX(){ return 0; }
  @BindField("scaleY")
  default float getScaleY(){ return 0; }
  @BindField("scaleZ")
  default float getScaleZ(){ return 0; }
  default Vec3 getScale(Vec3 result){
    return result.set(getScaleX(), getScaleY(), getScaleZ());
  }
  @BindField("scaleX")
  default void setScaleX(float x){}
  @BindField("scaleY")
  default void setScaleY(float y){}
  @BindField("scaleZ")
  default void setScaleZ(float z){}
  default void setScale(float x, float y, float z){
    setScaleX(x);
    setScaleY(y);
    setScaleZ(z);
  }
  default void setScale(Vec3 vec3) {
    setScale(vec3.x, vec3.y, vec3.z);
  }

  default Quat getRotation(Quat result){
    return result.setEulerAngles(getEulerY(), getEulerX(), getEulerZ());
  }
  default void setRotation(Quat quat){
    setEuler(quat.getPitch(), quat.getYaw(), quat.getRoll());
  }
  default Mat3D getTransform(Mat3D result){
    Quat q = getRotation(tmpQuat());
    return result.set(
        getX(), getY(), getZ(),
        q.x, q.y, q.z, q.w,
        getScaleX(), getScaleY(), getScaleZ()
    );
  }
}
