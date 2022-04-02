package singularity.world.distribution.request;

import arc.struct.IntMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemSeq;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;

public class RequestFactories{
  public interface RequestFactory{
    void addParseConfig(TargetConfigure cfg);

    DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender);
    
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

  private static abstract class AbstractLiquidRequestFactory implements RequestFactory{
    protected IntMap<LiquidStack> liquids = new IntMap<>();
    protected float total;

    @Override
    public void reset(){
      liquids.clear();
      total = 0;
    }

    protected void addParseConfig(TargetConfigure cfg, GridChildType type){
      Building build = Vars.world.build(cfg.position);
      assert build != null;
      float capacity = build.block.liquidCapacity, total = 0;
      for(UnlockableContent ignored: cfg.get(type, ContentType.liquid)){
        total++;
      }
      if(total > 0){
        for(UnlockableContent liquid: cfg.get(type, ContentType.liquid)){
          LiquidStack stack = liquids.get(liquid.id);
          if(stack == null){
            liquids.put(liquid.id, new LiquidStack((Liquid) liquid, capacity*(1/total)));
          }
          else stack.amount += capacity*(1/total);
        }
      }
    }
  }

  /*
  * items
  * */
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
      for(MatrixGrid.BuildingEntry<?> entry : target.matrixGrid().get(GridChildType.output, (e, c) -> e instanceof IOPointBlock.IOPoint)){
        if(entry.config == null) return;
        IOPointBlock.IOPoint ioPoint = (IOPointBlock.IOPoint) entry.entity;
        for(UnlockableContent item : ioPoint.config.get(GridChildType.output, ContentType.item)){
          if(target.getBuilding().items.has((Item)item) && ioPoint.acceptItemSuper(target.getBuilding(), (Item) item)){
            int i = target.getBuilding().removeStack((Item) item, 1);
            ioPoint.handleStack((Item) item, i, target.getBuilding());
          }
        }
      }
    }
  }

  /*
  * liquids
  * */
  public static class AcceptLiquidRequestFactory extends AbstractLiquidRequestFactory{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      addParseConfig(cfg, GridChildType.container);
    }

    @Override
    public DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender){
      return liquids.isEmpty()? null: new PutLiquidsRequest(sender, (LiquidsBuffer) sender.buffers().get(DistBuffers.liquidBuffer), new Seq<>(liquids.values().toArray()));
    }
  }

  public static class PutLiquidRequestFactory extends AbstractLiquidRequestFactory{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      addParseConfig(cfg, GridChildType.input);
    }

    @Override
    public DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender){
      return liquids.isEmpty()? null: new PutLiquidsRequest(sender, (LiquidsBuffer) sender.buffers().get(DistBuffers.liquidBuffer), new Seq<>(liquids.values().toArray()));
    }
  }

  public static class ReadLiquidRequestFactory extends AbstractLiquidRequestFactory{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      addParseConfig(cfg, GridChildType.output);
    }

    @Override
    public DistRequestBase<?> makeRequest(DistMatrixUnitBuildComp sender){
      return liquids.isEmpty()? null: new ReadLiquidsRequest(sender, (LiquidsBuffer) sender.buffers().get(DistBuffers.liquidBuffer), new Seq<>(liquids.values().toArray()));
    }

    @Override
    public void updateIO(DistMatrixUnitBuildComp target){
      for(MatrixGrid.BuildingEntry<?> entry : target.matrixGrid().get(GridChildType.output, (e, c) -> e instanceof IOPointBlock.IOPoint)){
        if(entry.config == null) return;
        IOPointBlock.IOPoint ioPoint = (IOPointBlock.IOPoint) entry.entity;
        float total = 0;
        for(UnlockableContent ignored: ioPoint.config.get(GridChildType.output, ContentType.liquid)){
          total++;
        }
        for(UnlockableContent liquid : ioPoint.config.get(GridChildType.output, ContentType.liquid)){
          float amount;
          if((amount = Math.min(ioPoint.block.liquidCapacity*(1/total), target.getBuilding().liquids.get((Liquid) liquid))) > 0
              && ioPoint.acceptLiquidSuper(target.getBuilding(), (Liquid) liquid)){
            amount = Math.min(amount, ioPoint.block.liquidCapacity - ioPoint.liquids.get((Liquid) liquid));
            target.liquids().remove((Liquid) liquid, amount);
            ioPoint.handleLiquid(target.getBuilding(), (Liquid) liquid, amount);
          }
        }
      }
    }
  }
}
