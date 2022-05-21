import java.util.Arrays;

public class Test{
  private static int cursor;
  private static float[] values = new float[10];
  private static float[] displayValues = new float[10];

  public static void main(String[] args){
    float lerpTime = 0.1f;

    Arrays.fill(values, Float.NaN);
    Arrays.fill(displayValues, Float.NaN);



    for(int i = 0; i < 30; i++){
      putValue(i);
      for(int l = 0; l < values.length; l++){
        if(Float.isNaN(displayValues[l]) && !Float.isNaN(values[l])){
          System.out.println("mod");
          displayValues[l] = 0;
        }
        displayValues[l] = lerpDelta(displayValues[l], values[l], lerpTime);
      }
      System.out.println(Arrays.toString(displayValues));
    }
  }

  private static float lerpDelta(float fromValue, float toValue, float progress){
    return fromValue + (toValue - fromValue) * progress;
  }

  public static void putValue(int value){
    if(cursor > values.length - 1){
      System.arraycopy(values, 1, values, 0, values.length - 1);
      values[cursor - 1] = value;
    }
    else{
      values[cursor++] = value;
    }
  }
}
