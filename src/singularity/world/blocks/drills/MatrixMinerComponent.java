package singularity.world.blocks.drills;

import arc.Core;
import arc.func.Boolf;
import arc.func.Floatf;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.type.Liquid;
import mindustry.ui.LiquidDisplay;
import mindustry.world.meta.Stat;
import mindustry.world.meta.Stats;
import universecore.util.UncLiquidStack;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.ConsumeLiquidCond;
import universecore.world.consumers.ConsumeType;

public class MatrixMinerComponent extends MatrixMinerPlugin{
  public MatrixMinerComponent(String name){
    super(name);
  }

  public void newBoost(float baseBoostScl, float attributeMultiplier, Boolf<Liquid> filter, float usageBase){
    newBoost(
        liquid -> baseBoostScl + (liquid.heatCapacity*1.2f - (liquid.temperature - 0.35f)*0.6f)*attributeMultiplier,
        liquid -> !liquid.gas && liquid.coolant && filter.get(liquid),
        usageBase,
        liquid -> usageBase/(liquid.heatCapacity*0.7f)
    );
  }

  @SuppressWarnings({"rawtypes"})
  public void newBoost(Floatf<Liquid> boostEff, Boolf<Liquid> filters, float usageBase, Floatf<Liquid> usageMult){
    newOptionalConsume((MatrixMinerComponentBuild e, BaseConsumers c) -> {}, (s, c) -> {
      s.add(Stat.booster, t -> {
        t.row();
        if (c.get(ConsumeType.liquid) instanceof ConsumeLiquidCond cons){
          for (UncLiquidStack stack : cons.getCons()) {
            Liquid liquid = stack.liquid;

            t.add(new LiquidDisplay(liquid, usageBase*usageMult.get(liquid)*60, true)).padRight(10).left().top();
            t.table(Tex.underline, bt -> {
              bt.left().defaults().padRight(3).left();
              bt.add("[lightgray]" + Core.bundle.get("misc.efficiency") + "[accent]" + Strings.autoFixed(boostEff.get(liquid)*100, 2) + "%[]");
            }).left().padTop(-9);
            t.row();
          }
        }
      });
    });
    consume.optionalAlwaysValid = false;
    consume.add(new ConsumeLiquidCond<MatrixMinerComponentBuild>(){
      {
        liquidEfficiency = boostEff;
        filter = filters;
        usage = usageBase;
        usageMultiplier = usageMult;

        maxFlammability = 0.1f;
      }

      @Override
      public void display(Stats stats){}
    });
  }

  public class MatrixMinerComponentBuild extends MatrixMinerPluginBuild{
    public float progress;

    @Override
    public void updateTile(){
      super.updateTile();
      BaseConsumers curr = consumer.current;
      if(curr == null) return;

      if(consumeValid()){
        progress += (1/curr.craftTime)*curr.delta(this)*warmup;
        while(progress > 1){
          progress--;
          consumer.trigger();
        }
      }
    }

    @Override
    public float boost(){
      if(consumer.optionalCurr == null) return consEfficiency();
      return Math.max(consumer.getOptionalEff(consumer.optionalCurr), 1)*consEfficiency();
    }

    @Override
    public void updatePlugin(MatrixMiner.MatrixMinerBuild owner){}

    @Override
    public void write(Writes write){
      super.write(write);
      write.f(progress);
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      progress = read.f();
    }
  }
}
