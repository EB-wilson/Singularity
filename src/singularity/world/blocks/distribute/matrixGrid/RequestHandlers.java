package singularity.world.blocks.distribute.matrixGrid;

import arc.func.Boolp;
import arc.struct.IntMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.type.*;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.world.blocks.distribute.IOPointBlock;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;
import singularity.world.distribution.request.*;

import java.util.Arrays;

/**包含了用于矩阵网格发出网络请求的一些辅助类*/
public class RequestHandlers{
  /**矩阵网格发出网络请求使用的辅助类接口，为矩阵网格提供将配置转化为网络请求的帮助*/
  public interface RequestHandler<R extends DistRequestBase>{
    /**添加一个要分析的io配置，所有配置项取求和
     * @param cfg 添加的配置项*/
    void addParseConfig(TargetConfigure cfg);

    /**由标记的配置生成网络请求
     * @param sender 此请求的目的发出者*/
    R makeRequest(DistMatrixUnitBuildComp sender);

    /**重置所有已添加的配置缓存*/
    void reset();

    /**请求的处理前回调方法*/
    default boolean preCallBack(DistMatrixUnitBuildComp sender, R request, Boolp task){
      return task.get();
    }

    default boolean callBack(DistMatrixUnitBuildComp sender, R request, Boolp task){
      return task.get();
    }

    default boolean afterCallBack(DistMatrixUnitBuildComp sender, R request, Boolp task){
      return task.get();
    }
  }
  
  private static abstract class AbstractItemRequestHandler{
    protected ItemSeq items = new ItemSeq();

    public void reset(){
      items.clear();
    }
  }

  private static abstract class AbstractLiquidRequestHandler{
    protected IntMap<LiquidStack> liquids = new IntMap<>();
    protected float total;

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
  public static class AcceptItemRequestHandler extends AbstractItemRequestHandler implements RequestHandler<PutItemsRequest>{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      for(UnlockableContent item : cfg.get(GridChildType.acceptor, ContentType.item)){
        items.add((Item) item, 1);
      }
    }
    
    @Override
    public PutItemsRequest makeRequest(DistMatrixUnitBuildComp sender){
      return items.toSeq().isEmpty()? null: new PutItemsRequest(sender, sender.getBuffer(DistBuffers.itemBuffer), items.toSeq());
    }
  }
  
  public static class PutItemRequestHandler extends AbstractItemRequestHandler implements RequestHandler<PutItemsRequest>{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      for(UnlockableContent item : cfg.get(GridChildType.input, ContentType.item)){
        items.add((Item) item, 1);
      }
    }
    
    @Override
    public PutItemsRequest makeRequest(DistMatrixUnitBuildComp sender){
      return items.toSeq().isEmpty()? null: new PutItemsRequest(sender, sender.getBuffer(DistBuffers.itemBuffer), items.toSeq());
    }
  }
  
  public static class ReadItemRequestHandler extends AbstractItemRequestHandler implements RequestHandler<ReadItemsRequest>{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      for(UnlockableContent item : cfg.get(GridChildType.output, ContentType.item)){
        items.add((Item) item, 1);
      }
    }
    
    @Override
    public ReadItemsRequest makeRequest(DistMatrixUnitBuildComp sender){
      Seq<ItemStack> seq = items.toSeq();
      ReadItemsRequest result = seq.isEmpty()? null: new ReadItemsRequest(sender, sender.getBuffer(DistBuffers.itemBuffer), seq);
      if(result == null) return null;
      result.waker = (DistMatrixUnitBuildComp e) -> {
        for(IntMap.Entry<IOPointBlock.IOPoint> point: e.ioPoints()){
          for(ItemStack stack: seq){
            if(point.value.config != null
                && point.value.config.get(GridChildType.output, stack.item)
                && point.value.acceptItemOut(e.getBuilding(), stack.item)) return true;
          }
        }

        return false;
      };
      return result;
    }

    private final int[] tmp = new int[Vars.content.items().size];
    private final ObjectSet<Item> tmpItems = new ObjectSet<>();

    @Override
    public boolean callBack(DistMatrixUnitBuildComp sender, ReadItemsRequest request, Boolp task){
      Arrays.fill(tmp, 0);
      tmpItems.clear();

      ItemsBuffer buffer = sender.getBuffer(DistBuffers.itemBuffer);
      for(ItemsBuffer.ItemPacket packet: buffer){
        tmp[packet.id()] = packet.amount();
        tmpItems.add(packet.get());
      }

      boolean taskStatus = task.get();

      for(MatrixGrid.BuildingEntry<?> entry : sender.matrixGrid().get(GridChildType.output, (e, c) -> e instanceof IOPointBlock.IOPoint)){
        if(entry.config == null) continue;
        IOPointBlock.IOPoint ioPoint = (IOPointBlock.IOPoint) entry.entity;
        if(ioPoint.config == null) continue;
        for(UnlockableContent item : ioPoint.config.get(GridChildType.output, ContentType.item)){
          if(buffer.get((Item)item) > 0 && ioPoint.acceptItemOut(sender.getBuilding(), (Item) item)){
            int amount = ioPoint.output((Item) item, 1);
            if(amount == 0) continue;
            buffer.remove((Item) item, 1);
            buffer.deReadFlow((Item) item, 1);

            tmpItems.add((Item) item);
          }
        }
      }

      ItemsBuffer coreBuffer = sender.distributor().core().distCore().getBuffer(DistBuffers.itemBuffer);
      for(int id = 0; id < tmp.length; id++){
        ItemsBuffer.ItemPacket packet = buffer.get(id);
        if(packet != null){
          if(!tmpItems.contains(packet.get())) continue;
          int transBack = Math.min(packet.amount() - tmp[id], coreBuffer.remainingCapacity().intValue());
          if(transBack <= 0) continue;

          packet.remove(transBack);
          packet.dePut(transBack);
          packet.deRead(transBack);
          coreBuffer.put(packet.get(), transBack);
          coreBuffer.dePutFlow(packet.get(), transBack);
          coreBuffer.deReadFlow(packet.get(), transBack);

          coreBuffer.bufferContAssign(sender.distributor().network, packet.get(), transBack);
        }
      }

      return taskStatus;
    }
  }

  /*
  * liquids
  * */
  public static class AcceptLiquidRequestHandler extends AbstractLiquidRequestHandler implements RequestHandler<PutLiquidsRequest>{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      addParseConfig(cfg, GridChildType.container);
    }

    @Override
    public PutLiquidsRequest makeRequest(DistMatrixUnitBuildComp sender){
      return liquids.isEmpty()? null: new PutLiquidsRequest(sender, sender.getBuffer(DistBuffers.liquidBuffer), new Seq<>(liquids.values().toArray()));
    }
  }

  public static class PutLiquidRequestHandler extends AbstractLiquidRequestHandler implements RequestHandler<PutLiquidsRequest>{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      addParseConfig(cfg, GridChildType.input);
    }

    @Override
    public PutLiquidsRequest makeRequest(DistMatrixUnitBuildComp sender){
      return liquids.isEmpty()? null: new PutLiquidsRequest(sender, sender.getBuffer(DistBuffers.liquidBuffer), new Seq<>(liquids.values().toArray()));
    }
  }

  public static class ReadLiquidRequestHandler extends AbstractLiquidRequestHandler implements RequestHandler<ReadLiquidsRequest>{
    @Override
    public void addParseConfig(TargetConfigure cfg){
      addParseConfig(cfg, GridChildType.output);
    }

    @Override
    public ReadLiquidsRequest makeRequest(DistMatrixUnitBuildComp sender){
      Seq<LiquidStack> seq = new Seq<>(liquids.values().toArray());
      ReadLiquidsRequest result = liquids.isEmpty()? null: new ReadLiquidsRequest(sender, sender.getBuffer(DistBuffers.liquidBuffer), seq);
      if(result == null) return null;
      result.waker = (DistMatrixUnitBuildComp e) -> {
        for(IntMap.Entry<IOPointBlock.IOPoint> point: e.ioPoints()){
          for(LiquidStack stack: seq){
            if(point.value.acceptLiquidOut(e.getBuilding(), stack.liquid)) return true;
          }
        }
        return false;
      };
      return result;
    }

    private final float[] tmp = new float[Vars.content.liquids().size];

    @Override
    public boolean callBack(DistMatrixUnitBuildComp sender, ReadLiquidsRequest request, Boolp task){
      Arrays.fill(tmp, 0);
      for(LiquidsBuffer.LiquidPacket packet: sender.getBuffer(DistBuffers.liquidBuffer)){
        tmp[packet.id()] = packet.amount();
      }

      boolean taskStatus = task.get();

      LiquidsBuffer buffer = sender.getBuffer(DistBuffers.liquidBuffer);
      for(LiquidsBuffer.LiquidPacket packet: buffer){
        tmp[packet.id()] = packet.amount() - tmp[packet.id()];
      }

      for(MatrixGrid.BuildingEntry<?> entry : sender.matrixGrid().get(GridChildType.output, (e, c) -> e instanceof IOPointBlock.IOPoint)){
        if(entry.config == null) return taskStatus;
        IOPointBlock.IOPoint ioPoint = (IOPointBlock.IOPoint) entry.entity;
        for(UnlockableContent liquid : ioPoint.config.get(GridChildType.output, ContentType.item)){
          float amount;
          if((amount = buffer.get((Liquid)liquid)) > 0.01f && ioPoint.acceptLiquidOut(sender.getBuilding(), (Liquid)liquid)){
            amount = ioPoint.output((Liquid) liquid, amount);
            buffer.remove((Liquid) liquid, amount);
            buffer.deReadFlow((Liquid) liquid, amount);

            tmp[liquid.id]--;
          }
        }
      }

      LiquidsBuffer coreBuffer = sender.distributor().core().distCore().getBuffer(DistBuffers.liquidBuffer);
      for(int id = 0; id < tmp.length; id++){
        if(tmp[id] <= 0) continue;
        LiquidsBuffer.LiquidPacket packet = coreBuffer.get(id);
        float transBack = Math.min(tmp[id], coreBuffer.remainingCapacity().floatValue());
        if(transBack <= 0) continue;

        if(packet != null){
          packet.remove(transBack);
          packet.dePut(transBack);
          packet.deRead(transBack);
          coreBuffer.put(packet.get(), transBack);
          coreBuffer.dePutFlow(packet.get(), transBack);
          coreBuffer.deReadFlow(packet.get(), transBack);
        }
      }

      return taskStatus;
    }
  }
}
