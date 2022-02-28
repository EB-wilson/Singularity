package singularity.world.distribution.request;

import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitBuildComp;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.buffers.ItemsBuffer;

public class RequestFactories{
  public interface RequestFactory{
    DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender);
    
    void addParseConfig(TargetConfigure cfg);
    
    void reset();
    
    default void updateIO(DistMatrixUnitBuildComp target){}
  }
  
  private static abstract class AbstractItemRequestFactory implements RequestFactory{
    protected ItemSeq items = new ItemSeq();
  
    @Override
    public void reset(){
      items.clear();
    }
  }
  
  public static class AcceptItemRequestFactory extends AbstractItemRequestFactory{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      for(UnlockableContent item : cfg.get(GridChildType.acceptor, ContentType.item)){
        items.add((Item) item, 1);
      }
    }
    
    @Override
    public DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender){
      return items.toSeq().isEmpty()? null: new PutItemsRequest(sender, (ItemsBuffer) sender.buffers().get(DistBuffers.itemBuffer), items.toSeq());
    }
  }
  
  public static class PutItemRequestFactory extends AbstractItemRequestFactory{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      for(UnlockableContent item : cfg.get(GridChildType.input, ContentType.item)){
        items.add((Item) item, 1);
      }
    }
    
    @Override
    public DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender){
      return items.toSeq().isEmpty()? null: new PutItemsRequest(sender, (ItemsBuffer) sender.buffers().get(DistBuffers.itemBuffer), items.toSeq());
    }
  }
  
  public static class ReadItemRequestFactory extends AbstractItemRequestFactory{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      for(UnlockableContent item : cfg.get(GridChildType.output, ContentType.item)){
        items.add((Item) item, 1);
      }
    }
    
    @Override
    public DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender){
      return items.toSeq().isEmpty()? null: new ReadItemsRequest(sender, (ItemsBuffer) sender.buffers().get(DistBuffers.itemBuffer), items.toSeq());
    }
  
    @Override
    public void updateIO(DistMatrixUnitBuildComp target){
      for(IOPointBlock.IOPoint<?> ioPoint : target.ioPoints().values()){
        if(ioPoint.config == null) return;
        for(UnlockableContent item : ioPoint.config.get(GridChildType.output, ContentType.item)){
          if(target.getBuilding().items.has((Item)item) && ioPoint.acceptItemSuper(target.getBuilding(), (Item) item)){
            int i = target.getBuilding().removeStack((Item) item, 1);
            ioPoint.handleStack((Item) item, i, target.getBuilding());
          }
        }
      }
    }
  }
}
