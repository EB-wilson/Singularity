import universecore.util.handler.MethodHandler;

public class Test{
  public static void main(String[] args){
    Test1 t = new Test1();

    MethodHandler.invokeDefault(t, "run1", Void.class);
    MethodHandler.invokeDefault(t, "run2", Void.class, "testing");
    MethodHandler.invokeDefault(Test1.class, "runStatic", Void.class, "static testing");
  }

  static class Test1{
    public void run1(){
      System.out.println("run1");
    }

    public void run2(String inf){
      System.out.println("run2: " + inf);
    }

    public static void runStatic(Object p){
      System.out.println(p);
    }
  }
}
