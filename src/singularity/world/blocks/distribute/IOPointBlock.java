package singularity.world.blocks.distribute;

import arc.util.Nullable;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.blocks.SglBlock;
import singularity.world.blocks.distribute.matrixGrid.RequestHandlers;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.IOPointBlockComp;
import singularity.world.components.distnet.IOPointComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.buffers.ItemsBuffer;
import singularity.world.distribution.buffers.LiquidsBuffer;
import universecore.annotations.Annotations;

import static mindustry.Vars.*;

/**非content类，方块标记，不进入contents，用于创建矩阵网络IO接口点的标记类型*/
@Annotations.ImplEntries
public class IOPointBlock extends SglBlock implements IOPointBlockComp{
  public GridChildType[] configTypes = {GridChildType.output, GridChildType.input, GridChildType.acceptor};
  public ContentType[] supportContentType = {ContentType.item, ContentType.liquid};
  @Nullable protected DistMatrixUnitBuildComp currPlacement;
  
  public IOPointBlock(String name){
    super(name);
    size = 1;
    update = true;
    hasItems = hasLiquids = true;
    displayFlow = false;
    
    outputItems = outputsLiquid = true;
    
    allowConfigInventory = false;
    
    itemCapacity = 16;
    liquidCapacity = 16;

    buildCostMultiplier = 0;

    setFactory(GridChildType.output, ContentType.item, new RequestHandlers.ReadItemRequestHandler());
    setFactory(GridChildType.input, ContentType.item, new RequestHandlers.PutItemRequestHandler());
    setFactory(GridChildType.acceptor, ContentType.item, new RequestHandlers.AcceptItemRequestHandler());

    //liquids
    setFactory(GridChildType.output, ContentType.liquid, new RequestHandlers.ReadLiquidRequestHandler());
    setFactory(GridChildType.input, ContentType.liquid, new RequestHandlers.PutLiquidRequestHandler());
    setFactory(GridChildType.acceptor, ContentType.liquid, new RequestHandlers.AcceptLiquidRequestHandler());
  }
  
  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotate){
    return currPlacement != null && currPlacement.tileValid(tile);
  }

  @Override
  public void setStats(){
    stats.add(Stat.size, "@x@", size, size);
  }

  public void resetCurrPlacement(){
    currPlacement = null;
    buildVisibility = BuildVisibility.hidden;
  }
  
  public void setCurrPlacement(DistMatrixUnitBuildComp parent){
    currPlacement = parent;
    buildVisibility = BuildVisibility.shown;
  }

  @Annotations.ImplEntries
  public class IOPoint extends SglBuilding implements IOPointComp{
    public DistMatrixUnitBuildComp parent;
    public DistTargetConfigTable.TargetConfigure config;

    public ItemModule outItems;
    public LiquidModule outLiquid;

    protected boolean siphoning;
    
    public IOPoint(){
      this.parent = currPlacement;
    }

    @Override
    public Object config(){
      return super.config();
    }

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      outItems = new ItemModule();
      outLiquid = new LiquidModule();
      return this;
    }

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      if(parent != null) parent.ioPoints().put(pos(), this);
      return this;
    }
  
    @Override
    public void remove(){
      if(parent != null) parent.removeIO(pos());
      super.remove();
    }
  
    @Override
    public void updateTile(){
      if(parent == null || !parent.getBuilding().isAdded()){
        remove();
        return;
      }
      if(parent.gridValid() && config != null){
        resourcesDump();
        resourcesSiphon();
        transBack();
      }
    }

    public int output(Item item, int amount){
      int add = Math.min(amount, itemCapacity - outItems.get(item));
      outItems.add(item, add);
      return add;
    }

    public float output(Liquid liquid, float amount){
      float add = Math.min(amount, liquidCapacity - outLiquid.get(liquid));
      outLiquid.add(liquid, add);
      return add;
    }
    
    public byte getDirectBit(Building other){
      byte result = 1;
      for(byte i = 0; i < 4; i++){
        if(nearby(i) == other) return result;
        result*=2;
      }
      return -1;
    }
  
    public void applyConfig(DistTargetConfigTable.TargetConfigure config){
      this.config = config;
    }

    @Override
    public boolean valid(DistMatrixUnitBuildComp unit, GridChildType type, Content content){
      if(content instanceof Item item){
        return config.get(GridChildType.output, item) && acceptItemOut(unit.getBuilding(), item);
      }
      else if(content instanceof Liquid liquid){
        return config.get(GridChildType.output, liquid) && acceptLiquid(unit.getBuilding(), liquid);
      }

      return false;
    }

    public void resourcesDump(){
      if(config == null) return;
      for(UnlockableContent item : config.get(GridChildType.output, ContentType.item)){
        dump((Item) item);
      }
      for(UnlockableContent liquid : config.get(GridChildType.output, ContentType.liquid)){
        dumpLiquid((Liquid) liquid);
      }
    }
  
    @Override
    public boolean dump(Item toDump){
      if(config == null || !block.hasItems || outItems.total() == 0 || (toDump != null && !outItems.has(toDump))) return false;
      if(proximity.size == 0) return false;
      
      Building other;
      
      if(toDump == null){
        for(int ii = 0; ii < content.items().size; ii++){
          Item item = content.item(ii);
        
          other = getNext("dumpItem",
              e -> e.interactable(team)
                  && outItems.has(item)
                  && e.acceptItem(this, item)
                  && canDump(e, item)
                  && config.directValid(GridChildType.output, item, getDirectBit(e)));
          if(other != null){
            other.handleItem(this, item);
            outItems.remove(item, 1);
            incrementDump(proximity.size);
            return true;
          }
        }
      }
      else{
        other = getNext("dumpItem",
            e -> e.interactable(team)
                && outItems.has(toDump)
                && e.acceptItem(this, toDump)
                && canDump(e, toDump)
                && config.directValid(GridChildType.output, toDump, getDirectBit(e)));
        if(other != null){
          other.handleItem(this, toDump);
          outItems.remove(toDump, 1);
          incrementDump(proximity.size);
          return true;
        }
      }
      return false;
    }
  
    @Override
    public void dumpLiquid(Liquid liquid, float scaling){
      int dump = this.cdump;
    
      if(config == null || outLiquid.get(liquid) <= 0.0001f) return;
      if(!net.client() && state.isCampaign() && team == state.rules.defaultTeam) liquid.unlock();
    
      for(int i = 0; i < proximity.size; i++){
        incrementDump(proximity.size);
        Building other = proximity.get((i + dump) % proximity.size);
        other = other.getLiquidDestination(this, liquid);
      
        if(other != null && other.interactable(team) && other.block.hasLiquids && canDumpLiquid(other, liquid) && other.liquids != null
            && config.directValid(GridChildType.output, liquid, getDirectBit(other))){
          float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
          float fract = outLiquid.get(liquid) / block.liquidCapacity;
        
          if(ofract < fract) outputLiquid(other, (fract - ofract) * block.liquidCapacity / scaling, liquid);
        }
      }
    }

    public float outputLiquid(Building next, float amount, Liquid liquid) {
      float flow = Math.min(next.block.liquidCapacity - next.liquids.get(liquid), amount);
      if (next.acceptLiquid(this, liquid)) {
        next.handleLiquid(this, liquid, flow);
        outLiquid.remove(liquid, flow);
      }

      return flow;
    }
  
    public void resourcesSiphon(){
      siphoning = true;
      if(config == null) return;
      for(UnlockableContent item : config.get(GridChildType.input, ContentType.item)){
        siphonItem((Item) item);
      }
      for(UnlockableContent liquid : config.get(GridChildType.input, ContentType.liquid)){
        siphonLiquid((Liquid) liquid);
      }
      siphoning = false;
    }
    
    public void transBack(){
      if(config == null) return;

      Building parentBuild = parent.getBuilding();
      ItemsBuffer itsB = parent.getBuffer(DistBuffers.itemBuffer);
      LiquidsBuffer lisB = parent.getBuffer(DistBuffers.liquidBuffer);

      items.each((item, amount) -> {
        int move = parentBuild.acceptItem(this, item)? Math.min(parentBuild.getMaximumAccepted(item) - parentBuild.items.get(item), amount): 0;
        if(move > 0){
          removeStack(item, move);
          parentBuild.handleStack(item, move, this);
          itsB.dePutFlow(item, move);
        }
      });
      
      liquids.each((liquid, amount) -> {
        lisB.dePutFlow(liquid, moveLiquid(parentBuild, liquid));
      });

      outItems.each((item, amount) -> {
        if(config.get(GridChildType.output, ContentType.item).contains(item)) return;
        int accept = parentBuild.acceptItem(this, item)? Math.min(parentBuild.getMaximumAccepted(item) - parentBuild.items.get(item), amount): 0;
        if(accept > 0){
          outItems.remove(item, accept);
          parentBuild.handleStack(item, accept, this);
          itsB.dePutFlow(item, accept);
        }
      });

      outLiquid.each((liquid, amount) -> {
        if(config.get(GridChildType.output, ContentType.liquid).contains(liquid)) return;
        lisB.dePutFlow(liquid, outputLiquid(parentBuild, amount, liquid));
      });
    }
  
    public void siphonItem(Item item){
      if(config == null) return;
      Building other;
      other = getNext("siphonItem",
          e -> e.block.hasItems
              && e.items.has(item)
              && config.directValid(GridChildType.input, item, getDirectBit(e)));
      if(other == null || !interactable(other.team) || !acceptItem(other, item)) return;
      other.removeStack(item, 1);
      handleItem(other, item);
    }
  
    public void siphonLiquid(Liquid liquid){
      if(config == null) return;
      Building other;
      other = getNext("siphonLiquid",
          e -> e.block.hasLiquids
              && e.liquids.get(liquid) > 0
              && config.directValid(GridChildType.input, liquid, getDirectBit(e)));
      if(other == null || !acceptLiquid(other, liquid)) return;
      other.moveLiquid(this, liquid);
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      if(siphoning) return super.acceptItem(source, item);
      return config != null
          && config.directValid(GridChildType.acceptor, item, getDirectBit(source))
          && config.get(GridChildType.acceptor, ContentType.item).contains(item)
          && super.acceptItem(source, item);
    }
    
    public final boolean acceptItemOut(Building source, Item item){
      return interactable(source.team) && outItems.get(item) < itemCapacity;
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      if(siphoning) return super.acceptLiquid(source, liquid);
      return config != null
          && config.directValid(GridChildType.acceptor, liquid, getDirectBit(source))
          && config.get(GridChildType.acceptor, ContentType.liquid).contains(liquid)
          && super.acceptLiquid(source, liquid);
    }
    
    public final boolean acceptLiquidOut(Building source, Liquid liquid){
      return interactable(source.team) && outLiquid.get(liquid) < liquidCapacity;
    }

    public GridChildType[] configTypes(){
      return configTypes;
    }

    public ContentType[] configContentTypes(){
      return supportContentType;
    }
  }
}
