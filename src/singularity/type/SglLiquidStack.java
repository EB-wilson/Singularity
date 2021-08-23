package singularity.type;

import mindustry.content.Liquids;
import mindustry.type.Liquid;

public class SglLiquidStack implements Comparable<SglLiquidStack>{
  public static final SglLiquidStack[] empty = {};

  public Liquid liquid;
  public float amount;

  public SglLiquidStack(Liquid liquid, float amount){
    if(liquid == null) liquid = Liquids.water;
    this.liquid = liquid;
    this.amount = amount;
  }

  public SglLiquidStack set(Liquid liquid, float amount){
    this.liquid = liquid;
    this.amount = amount;
    return this;
  }

  public SglLiquidStack copy(){
    return new SglLiquidStack(liquid, amount);
  }

  public boolean equals(SglLiquidStack other){
    return other != null && other.liquid == liquid && other.amount == amount;
  }

  public static SglLiquidStack[] mult(SglLiquidStack[] stacks, float amount){
    SglLiquidStack[] copy = new SglLiquidStack[stacks.length];
    for(int i = 0; i < copy.length; i++){
      copy[i] = new SglLiquidStack(stacks[i].liquid, stacks[i].amount * amount);
    }
    return copy;
  }

  public static SglLiquidStack[] with(Object... liquids){
    SglLiquidStack[] stacks = new SglLiquidStack[liquids.length / 2];
    for(int i = 0; i < liquids.length; i += 2){
      stacks[i / 2] = new SglLiquidStack((Liquid)liquids[i], ((Number)liquids[i + 1]).floatValue());
    }
    return stacks;
  }

  @Override
  public int compareTo(SglLiquidStack liquidStack){
    return liquid.compareTo(liquidStack.liquid);
  }
  
  @Override
  public String toString(){
    return "LiquidStack{" +
    "liquid=" + liquid +
    ", amount=" + amount +
    '}';
  }
}
