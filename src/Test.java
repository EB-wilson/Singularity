import mindustry.mod.Mod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@AnnoTest(87)
public class Test extends Mod{
  public static void main(String[] args){

  }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface AnnoTest{
  int value();
}
