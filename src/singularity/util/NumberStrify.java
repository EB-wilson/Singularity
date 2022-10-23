package singularity.util;

public class NumberStrify{
  private static final String[] byteUnit = {
      "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"
  };

  @SuppressWarnings("StringRepeatCanBeUsed")
  public static String toByteFixNonUnit(double number, int retain){
    boolean isNegative = false;
    if(number < 0){
      number = -number;
      isNegative = true;
    }

    double base = 1;
    for(int i = 0; i<byteUnit.length; i++){
      if(base*1024 > number){
        break;
      }
      base *= 1024;
    }

    String[] arr = Double.toString(number/base).split("\\.");
    int realRetain = Math.min(retain, arr[1].length());

    StringBuilder end = new StringBuilder();
    for(int i = 0; i < retain - realRetain; i++){
      end.append("0");
    }

    return (isNegative? "-": "") + arr[0] + (retain == 0? "": "." + arr[1].substring(0, realRetain) + end);
  }

  @SuppressWarnings("StringRepeatCanBeUsed")
  public static String toByteFix(double number, int retain){
    boolean isNegative = false;
    if(number < 0){
      number = -number;
      isNegative = true;
    }

    int index = 0;
    double base = 1;
    for(int i = 0; i<byteUnit.length; i++){
      if(base*1024 > number){
        break;
      }
      base *= 1024;
      index++;
    }

    String[] arr = Double.toString(number/base).split("\\.");
    int realRetain = Math.min(retain, arr[1].length());

    StringBuilder end = new StringBuilder();
    for(int i = 0; i < retain - realRetain; i++){
      end.append("0");
    }

    return (isNegative? "-": "") + arr[0] + (retain == 0? "": "." + arr[1].substring(0, realRetain) + end + byteUnit[index]);
  }
}
