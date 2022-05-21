package singularity.world.modules;

import arc.func.Cons;
import arc.util.io.Writes;
import mindustry.world.modules.BlockModule;
import singularity.world.blocks.chains.ChainsContainer;
import singularity.world.components.ChainsBuildComp;

public class ChainsModule extends BlockModule{
  public ChainsBuildComp entity;
  public ChainsContainer container;
  
  public ChainsModule(ChainsBuildComp entity){
    this.entity = entity;
  }
  
  public ChainsContainer newContainer(){
    ChainsContainer old = entity.chains().container;

    entity.chains().container = new ChainsContainer();
    entity.containerCreated(old);

    entity.chains().container.add(entity);

    return entity.chains().container;
  }
  
  public void each(Cons<ChainsBuildComp> cons){
    for(ChainsBuildComp other: container.all){
      cons.get(other);
    }
  }
  
  public void putVar(String key, Object obj){
    container.putVar(key, obj);
  }
  
  public <T> T getVar(String key){
    return container.getVar(key);
  }
  
  public void update(){
  
  }
  
  @Override
  public void write(Writes write){
  
  }
  
}
