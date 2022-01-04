package singularity.world.blocks.distribute;

import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import singularity.type.GasStack;
import singularity.type.SglContents;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.ui.tables.DistTargetConfigTable.TargetConfigure;
import singularity.world.blockComp.SecondableConfigBuildComp;
import singularity.world.blockComp.distributeNetwork.DistMatrixUnitComp;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.GasesBuffer;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;
import singularity.world.distribution.request.PutItemsRequest;
import singularity.world.distribution.request.ReadItemsRequest;
import universeCore.util.DataPackable;

import java.util.PriorityQueue;

import static mindustry.ctype.ContentType.item;
import static mindustry.ctype.ContentType.liquid;

public class MatrixGridBlock extends DistNetBlock{
  public MatrixGridBlock(String name){
    super(name);
  }
  
  @Override
  public void appliedConfig(){
    super.appliedConfig();
    config(TargetConfigure.class, (MatrixGridBuild e, TargetConfigure c) -> {
      if(c.any()){
        e.configs.add(c);
        if(e.grid != null){
          Building t = Vars.world.build(c.position);
          if(t != null) e.grid.add(t, c.type, c.priority);
        }
      }
      else{
        e.configs.remove(c);
        if(e.grid != null){
          Building t = Vars.world.build(c.position);
          if(t != null) e.grid.remove(t);
        }
      }
    });
  }
  
  protected static final ObjectMap<ContentType, float[]> requires = new ObjectMap<>();
  protected static final ObjectMap<ContentType, float[]> inputs = new ObjectMap<>();
  
  protected static final Seq<ItemStack> tempItems = new Seq<>();
  protected static final Seq<LiquidStack> tempLiquids = new Seq<>();
  protected static final Seq<GasStack> tempGases = new Seq<>();
  public class MatrixGridBuild extends DistNetBuild implements DistMatrixUnitComp, SecondableConfigBuildComp{
    public @Nullable MatrixGrid grid;
    
    protected PriorityQueue<TargetConfigure> configs = new PriorityQueue<>((a, b) -> a.priority - b.priority);
    
    public PutItemsRequest lastItemPut;
    public ReadItemsRequest lastItemRead;
    
    public ItemsBuffer itemsBuffer = new ItemsBuffer();
    public LiquidsBuffer liquidsBuffer = new LiquidsBuffer();
    public GasesBuffer gasesBuffer = new GasesBuffer();
    
    @Override
    public MatrixGrid matrixGrid(){
      return grid;
    }
  
    @Override
    public void buildSecondaryConfig(Table table, Building target){
      table.table(Tex.pane, root -> root.add(new DistTargetConfigTable(target, new ContentType[]{item, liquid, SglContents.gas}, this::configure)));
    }
  
    @Override
    public void updateTile(){
      for(ContentType type: ContentType.all){
        requires.put(type, new float[Vars.content.getBy(type).size]);
        inputs.put(type, new float[Vars.content.getBy(type).size]);
      }
  
      Building entity;
      for(TargetConfigure c: configs){
        entity = Vars.world.build(c.position);
        if(entity == null) continue;
        
        int id = 0;
        switch(c.type){
          case input:{
            for(ContentType t: ContentType.all){
              for(float amount: c.get(t)){
                handleInput(entity, t, id, amount);
                inputs.get(t)[id] += amount;
                id++;
              }
            }
            break;
          }
          case output:{
            for(ContentType t: ContentType.all){
              for(float amount: c.get(t)){
                handleOutput(entity, t, id, amount);
                requires.get(t)[id] += amount;
                id++;
              }
            }
            break;
          }
        }
      }
      
      releaseRequest();
    }
  
    public void handleOutput(Building entity, ContentType type, int id, float amount){
      if(type == item){
        int move = (int)Math.min(itemsBuffer.get(id).obj.amount, amount);
        move = Math.min(move, entity.block.itemCapacity - entity.items.get(id));
    
        if(move > 0){
          Item item = Vars.content.item(id);
      
          if(entity.acceptItem(this, item)){
            itemsBuffer.remove(item, move);
            for(int i = 0; i < move; i++) entity.handleItem(this, item);
          }
        }
      }
      else if(type == liquid){
      
      }
      else if(type == SglContents.gas){
      
      }
    }
    
    public void handleInput(Building entity, ContentType type, int id, float amount){
      if(type == item){
        int get = (int)Math.min(entity.items.get(id), amount);
        get = Math.min(get, itemsBuffer.remainingCapacity());
        
        if(get > 0){
          Item item = Vars.content.item(id);
          itemsBuffer.put(item, get);
          entity.items.remove(item, get);
        }
      }
    }
  
    public void releaseRequest(){
      tempItems.clear();
      if(lastItemRead == null || lastItemRead.finished()){
        int id = 0;
        for(float amount: requires.get(item)){
          tempItems.add(new ItemStack(Vars.content.item(id), (int)amount));
          id++;
        }
        lastItemRead = new ReadItemsRequest(tempItems, itemsBuffer);
        distributor.assign(lastItemRead);
      }
      
      tempItems.clear();
      if(lastItemPut == null || lastItemPut.finished()){
        int id = 0;
        for(float amount: inputs.get(item)){
          tempItems.add(new ItemStack(Vars.content.item(id), (int)amount));
          id++;
        }
        lastItemPut = new PutItemsRequest(tempItems, itemsBuffer);
        distributor.assign(lastItemRead);
      }
  
      tempLiquids.clear();
      
      tempGases.clear();
    }
  }
  
  public static class ConfigureStruct implements DataPackable{
    @Override
    public void write(Writes write){
    
    }
  
    @Override
    public void read(Reads read){
    
    }
  }
}
