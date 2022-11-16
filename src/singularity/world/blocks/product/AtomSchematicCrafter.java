package singularity.world.blocks.product;

import arc.func.Cons2;
import mindustry.world.meta.Stats;
import singularity.type.AtomSchematic;
import singularity.type.SglContents;
import singularity.world.products.Producers;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsumers;

public class AtomSchematicCrafter extends MediumCrafter{
  public AtomSchematicCrafter(String name){
    super(name);
  }

  @Override
  public void init(){
    for(AtomSchematic atomSchematic: SglContents.atomSchematics()){
      consumers().add(atomSchematic.request);
      super.newProduce();
      produce.item(atomSchematic.item, 1);
    }

    super.init();
  }

  @Override
  public BaseConsumers newConsume(){
    return null;
  }

  @Override
  public <T extends ConsumerBuildComp> BaseConsumers newOptionalConsume(Cons2<T, BaseConsumers> validDef, Cons2<Stats, BaseConsumers> displayDef){
    return null;
  }

  @Override
  public Producers newProduce(){
    return null;
  }
}
