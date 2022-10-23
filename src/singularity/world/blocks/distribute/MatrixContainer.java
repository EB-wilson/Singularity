package singularity.world.blocks.distribute;

import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.type.Item;
import mindustry.type.Liquid;
import singularity.Sgl;
import singularity.world.blocks.SglBlock;
import singularity.world.distribution.DistBufferType;
import singularity.world.distribution.DistSupportContainerTable.Container;

public class MatrixContainer extends SglBlock{
  public boolean isIntegrate;

  public MatrixContainer(String name){
    super(name);

    update = false;
    destructible = true;
    unloadable = false;
    outputItems = false;
  }

  @Override
  public void init(){
    super.init();
    setDistSupport();
  }

  public void setDistSupport(){
    Container cont = Sgl.matrixContainers.getContainer(this, () -> new Container(this, isIntegrate));
    if(hasItems) cont.setCapacity(DistBufferType.itemBuffer, itemCapacity);
    if(hasLiquids) cont.setCapacity(DistBufferType.liquidBuffer, liquidCapacity);
  }

  public class MatrixContainerBuild extends SglBuilding{
    @Override
    public boolean acceptItem(Building source, Item item){
      if(!isIntegrate) return super.acceptItem(source, item);
      return interactable(source.team) && items.total() < itemCapacity;
    }

    @Override
    public int acceptStack(Item item, int amount, Teamc source){
      if(!isIntegrate) return super.acceptStack(item, amount, source);
      return interactable(source.team())? Math.min(amount, itemCapacity - items.total()): 0;
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      if(!isIntegrate) return super.acceptLiquid(source, liquid);
      return interactable(source.team) && liquids().total() < liquidCapacity;
    }
  }
}
