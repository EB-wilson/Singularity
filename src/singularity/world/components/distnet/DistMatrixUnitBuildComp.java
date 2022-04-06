package singularity.world.components.distnet;

import arc.struct.IntMap;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.gen.Building;
import mindustry.world.Tile;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.components.GasBuildComp;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.BaseBuffer;
import singularity.world.distribution.request.DistRequestBase;
import singularity.world.distribution.request.RequestFactories.RequestFactory;
import universecore.annotations.Annotations;
import universecore.util.Empties;

@SuppressWarnings("rawtypes")
public interface DistMatrixUnitBuildComp extends DistElementBuildComp{
  @Annotations.BindField(value = "grid", initialize = "new singularity.world.distribution.MatrixGrid(this)")
  default MatrixGrid matrixGrid(){
    return null;
  }
  
  @Annotations.BindField("buffers")
  default ObjectMap<DistBuffers<?>, BaseBuffer<?, ?, ?>> buffers(){
    return null;
  }
  
  @Annotations.BindField("ioPoints")
  default IntMap<IOPointBlock.IOPoint> ioPoints(){
    return null;
  }

  @Annotations.MethodEntry(entryMethod = "update")
  default void updateGrid(){
    if(gridValid()) matrixGrid().update();
  }
  
  default void initBuffers(){
    for(DistBuffers<?> buffer : DistBuffers.all){
      buffers().put(buffer, buffer.get(getMatrixBlock().bufferCapacity()));
    }
  }
  
  default boolean gridValid(){
    return true;
  }
  
  @Override
  default int priority(){
    return matrixGrid().priority;
  }
  
  @Override
  default void priority(int priority){
    matrixGrid().priority = priority;
    distributor().network.priorityModified(this);
  }
  
  default boolean configValid(Building entity){
    if(entity instanceof IOPointBlock.IOPoint && ((IOPointBlock.IOPoint) entity).parent == this) return true;
    return (entity.block.hasItems)
        || (entity.block.hasLiquids)
        || (entity instanceof GasBuildComp && ((GasBuildComp) entity).getGasBlock().hasGases());
  }
  
  default void addConfig(GridChildType type, ContentType contType, DistTargetConfigTable.TargetConfigure cfg){
    RequestFactory factory = getMatrixBlock().requestFactories().get(type, Empties.nilMapO()).get(contType);
    if(factory != null) factory.addParseConfig(cfg);
  }
  
  default DistRequestBase createRequest(GridChildType type, ContentType contType){
    RequestFactory factory = getMatrixBlock().requestFactories().get(type, Empties.nilMapO()).get(contType);
    if(factory == null) return null;
    DistRequestBase result = factory.makeRequest(this);
    factory.reset();
    return result;
  }
  
  default DistMatrixUnitComp getMatrixBlock(){
    return getBlock(DistMatrixUnitComp.class);
  }
  
  void ioPointConfigBackEntry(IOPointBlock.IOPoint ioPoint);
  
  boolean tileValid(Tile tile);
  
  void drawValidRange();
  
  default void removeIO(int pos){
    ioPoints().remove(pos);
    matrixGrid().remove(Vars.world.build(pos));
  }
}
