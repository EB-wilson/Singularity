package singularity.world.blocks.function;

import arc.Core;
import arc.func.Cons2;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.meta.Stats;
import singularity.type.AtomSchematic;
import singularity.type.SglContents;
import singularity.world.blocks.product.NormalCrafter;
import singularity.world.products.Producers;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsumers;

public class Destructor extends NormalCrafter{
  public Destructor(String name){
    super(name);
  }

  @Override
  public void init(){
    for(AtomSchematic atomSchematic: SglContents.atomSchematics()){
      super.newConsume();
      consume.item(atomSchematic.item, 1);
      consume.energy(8);
      consume.selectable = atomSchematic::researchVisibility;
      consume.trigger = e -> {
        atomSchematic.destructing(1);
      };
      consume.time(6);
    }

    super.init();
  }

  @Override
  public void setBars(){
    super.setBars();
    bars.add("progress", (NormalCrafterBuild e) -> {
      AtomSchematic schematic = e.consumeCurrent() == -1? null: SglContents.atomSchematics().get(e.consumeCurrent());
      return new Bar(
          () -> schematic != null? Core.bundle.format("bar.destructProgress", schematic.destructed(), schematic.researchConsume)
          : Core.bundle.get("bar.noSelect"),
          () -> Pal.bar,
          () -> schematic == null? 0: schematic.researchProgress()
      );
    });
  }

  @Override
  public BaseConsumers newConsume(){
    return null;
  }

  @Override
  public BaseConsumers newOptionalConsume(Cons2<ConsumerBuildComp, BaseConsumers> validDef, Cons2<Stats, BaseConsumers> displayDef){
    return null;
  }

  @Override
  public Producers newProduce(){
    return null;
  }
}
