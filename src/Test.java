import mindustry.mod.Mod;

public class Test extends Mod{
  public static void main(String[] args){
    System.out.println(System.getenv("ANDROID_HOME"));
  }

  private static String getColor(double d){
    int n = (int) (d*16);
    String s = n + (n >= 10? "": " ");

    int b = n >= 8? 100 + n - 8: 40 + n;

    return "\033[" + b + ";97m" + s + "\033[0m";
  }
}
