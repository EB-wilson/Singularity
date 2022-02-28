package singularity.type;

import singularity.contents.Gases;

public class GasStack implements Comparable<GasStack>{
  public static final GasStack[] empty = {};
  
  public Gas gas;
  public float amount;
  
  public GasStack(Gas gas, float amount){
    if(gas == null) gas = Gases.CH4;
    this.gas = gas;
    this.amount = amount;
  }
  
  public GasStack set(Gas gas, float amount){
    this.gas = gas;
    this.amount = amount;
    return this;
  }
  
  public GasStack copy(){
    return new GasStack(gas, amount);
  }
  
  public boolean equals(GasStack other){
    return other != null && other.gas == gas && other.amount == amount;
  }
  
  public static GasStack[] mult(GasStack[] stacks, float amount){
    GasStack[] copy = new GasStack[stacks.length];
    for(int i = 0; i < copy.length; i++){
      copy[i] = new GasStack(stacks[i].gas, stacks[i].amount * amount);
    }
    return copy;
  }
  
  public static GasStack[] with(Object... gases){
    GasStack[] stacks = new GasStack[gases.length / 2];
    for(int i = 0; i < gases.length; i += 2){
      stacks[i / 2] = new GasStack((Gas)gases[i], ((Number)gases[i + 1]).floatValue());
    }
    return stacks;
  }
  
  @Override
  public int compareTo(GasStack gasStack){
    return gas.compareTo(gasStack.gas);
  }
  
  @Override
  public String toString(){
    return "gasStack{" +
      "gas=" + gas +
      ", amount=" + amount +
      '}';
  }
}
