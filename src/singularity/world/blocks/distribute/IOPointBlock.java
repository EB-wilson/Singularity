package singularity.world.blocks.distribute;

import arc.util.Nullable;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import singularity.type.Gas;
import singularity.type.SglContents;
import singularity.ui.tables.DistTargetConfigTable;
import singularity.world.components.GasBuildComp;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.blocks.SglBlock;
import singularity.world.distribution.GridChildType;

import static mindustry.Vars.*;

/**非content类，方块标记，不进入contents，用于创建矩阵网络IO接口点的标记类型*/
public class IOPointBlock extends SglBlock{
  @Nullable protected DistMatrixUnitBuildComp currPlacement;
  
  public IOPointBlock(String name){
    super(name);
    size = 1;
    update = true;
    hasItems = hasLiquids = hasGases = true;
    displayFlow = false;
    showGasFlow = false;
    
    outputItems = outputsLiquid = outputGases = true;
    
    allowConfigInventory = false;
    
    itemCapacity = 16;
    liquidCapacity = 16;
    gasCapacity = 8;
    maxGasPressure = 8;

    buildCostMultiplier = 0;
  }
  
  @Override
  public boolean canPlaceOn(Tile tile, Team team){
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
  
  public class IOPoint extends SglBuilding{
    public DistMatrixUnitBuildComp parent;
    public DistTargetConfigTable.TargetConfigure config;
    
    protected boolean siphoning;
    
    public IOPoint(){
      this.parent = currPlacement;
    }
  
    @Override
    public void onProximityAdded(){
      super.onProximityAdded();
      if(parent != null) parent.ioPoints().put(pos(), this);
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
  
    public void resourcesDump(){
      for(UnlockableContent item : config.get(GridChildType.output, ContentType.item)){
        dump((Item) item);
      }
      for(UnlockableContent liquid : config.get(GridChildType.output, ContentType.liquid)){
        dumpLiquid((Liquid) liquid);
      }
      for(UnlockableContent gas : config.get(GridChildType.output, SglContents.gas)){
        dumpGas((Gas) gas);
      }
    }
  
    @Override
    public boolean dump(Item toDump){
      if(!block.hasItems || items.total() == 0 || (toDump != null && !items.has(toDump))) return false;
      if(proximity.size == 0) return false;
      
      Building other;
      
      if(toDump == null){
        for(int ii = 0; ii < content.items().size; ii++){
          Item item = content.item(ii);
        
          other = getNext("dumpItem",
              e -> e.interactable(team)
                  && items.has(item)
                  && e.acceptItem(this, item)
                  && canDump(e, item)
                  && config.directValid(GridChildType.output, item, getDirectBit(e)));
          if(other != null){
            other.handleItem(this, item);
            items.remove(item, 1);
            incrementDump(proximity.size);
            return true;
          }
        }
      }
      else{
        other = getNext("dumpItem",
            e -> e.interactable(team) && items.has(toDump)
                && e.acceptItem(this, toDump)
                && canDump(e, toDump)
                && config.directValid(GridChildType.output, toDump, getDirectBit(e)));
        if(other != null){
          other.handleItem(this, toDump);
          items.remove(toDump, 1);
          incrementDump(proximity.size);
          return true;
        }
      }
      return false;
    }
  
    @Override
    public void dumpLiquid(Liquid liquid, float scaling){
      int dump = this.cdump;
    
      if(liquids.get(liquid) <= 0.0001f) return;
      if(!net.client() && state.isCampaign() && team == state.rules.defaultTeam) liquid.unlock();
    
      for(int i = 0; i < proximity.size; i++){
        incrementDump(proximity.size);
        Building other = proximity.get((i + dump) % proximity.size);
        other = other.getLiquidDestination(this, liquid);
      
        if(other != null && other.interactable(team) && other.block.hasLiquids && canDumpLiquid(other, liquid) && other.liquids != null
            && config.directValid(GridChildType.output, liquid, getDirectBit(other))){
          float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
          float fract = liquids.get(liquid) / block.liquidCapacity;
        
          if(ofract < fract) transferLiquid(other, (fract - ofract) * block.liquidCapacity / scaling, liquid);
        }
      }
    }
    
    @Override
    public void dumpGas(Gas gas){
      GasBuildComp other = (GasBuildComp) this.getNext("gases",
          e -> e instanceof GasBuildComp
              && e.interactable(team) && gases.get(gas) > 0
              && ((GasBuildComp) e).acceptGas(this, gas)
              && config.directValid(GridChildType.output, gas, getDirectBit(e)));
      
      if(other != null) moveGas(other, gas);
    }
  
    public void resourcesSiphon(){
      siphoning = true;
      for(UnlockableContent item : config.get(GridChildType.input, ContentType.item)){
        siphonItem((Item) item);
      }
      for(UnlockableContent liquid : config.get(GridChildType.input, ContentType.liquid)){
        siphonLiquid((Liquid) liquid);
      }
      for(UnlockableContent gas : config.get(GridChildType.input, SglContents.gas)){
        siphonGas((Gas) gas);
      }
      siphoning = false;
    }
    
    public void transBack(){
      Building parentBuild = parent.getBuilding();
      items.each((item, amount) -> {
        int accept = parentBuild.acceptItem(this, item)? Math.min(parentBuild.getMaximumAccepted(item) - parentBuild.items.get(item), amount): 0;
        if(accept > 0){
          removeStack(item, accept);
          parentBuild.handleStack(item, accept, this);
        }
      });
      
      liquids.each((liquid, amount) -> {
        moveLiquid(parentBuild, liquid);
      });
      
      if(parent instanceof GasBuildComp) moveGas(parent.getBuilding(GasBuildComp.class));
    }
  
    public void siphonItem(Item item){
      Building next;
      next = getNext("siphonItem",
          e -> e.block.hasItems
              && e.items.has(item)
              && config.directValid(GridChildType.input, item, getDirectBit(e)));
      if(next == null || next.team != team || !acceptItem(next, item)) return;
      next.removeStack(item, 1);
      handleItem(next, item);
    }
  
    public void siphonLiquid(Liquid liquid){
      Building next;
      next = getNext("siphonLiquid",
          e -> e.block.hasLiquids
              && e.liquids.get(liquid) > 0
              && config.directValid(GridChildType.input, liquid, getDirectBit(e)));
      if(next == null) return;
      next.moveLiquid(this, liquid);
    }
  
    public void siphonGas(Gas gas){
      GasBuildComp next;
      next = (GasBuildComp) getNext("siphonGas",
          e -> e instanceof GasBuildComp
              && ((GasBuildComp) e).getGasBlock().hasGases()
              && ((GasBuildComp) e).gases().get(gas) > 0
              && config.directValid(GridChildType.input, gas, getDirectBit(e)));
      if(next == null) return;
      next.moveGas(this, gas);
    }
  
    @Override
    public boolean acceptItem(Building source, Item item){
      if(siphoning) return super.acceptItem(source, item);
      return config != null
          && config.directValid(GridChildType.acceptor, item, getDirectBit(source))
          && config.get(GridChildType.acceptor, ContentType.item).contains(item)
          && super.acceptItem(source, item);
    }
    
    public final boolean acceptItemSuper(Building source, Item item){
      return super.acceptItem(source, item);
    }
  
    @Override
    public boolean acceptLiquid(Building source, Liquid liquid){
      if(siphoning) return super.acceptLiquid(source, liquid);
      return config != null
          && config.directValid(GridChildType.acceptor, liquid, getDirectBit(source))
          && config.get(GridChildType.acceptor, ContentType.liquid).contains(liquid)
          && super.acceptLiquid(source, liquid);
    }
    
    public final boolean acceptLiquidSuper(Building source, Liquid liquid){
      return super.acceptLiquid(source, liquid);
    }
    
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      if(siphoning) return super.acceptGas(source, gas);
      return config != null
          && config.directValid(GridChildType.acceptor, gas, getDirectBit(source.getBuilding()))
          && config.get(GridChildType.acceptor, SglContents.gas).contains(gas)
          && super.acceptGas(source, gas);
    }
  
    public final boolean acceptGasSuper(GasBuildComp source, Gas gas){
      return super.acceptGas(source, gas);
    }
  }
}
