package singularity.world.blocks.drills;

import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import singularity.world.components.ChainsBlockComp;
import singularity.world.components.ChainsBuildComp;
import singularity.world.modules.ChainsModule;
import universecore.annotations.Annotations;

@Annotations.ImplEntries
public class ExtendableDrill extends BaseDrill implements ChainsBlockComp{
  public int maxChainsHeight = 30;
  public int maxChainsWidth = 30;

  public ObjectSet<ExtendMiner> validChildType = new ObjectSet<>();

  public ExtendableDrill(String name){
    super(name);
  }

  @Override
  public boolean chainable(ChainsBlockComp other){
    return other instanceof ExtendMiner m && validChildType.contains(m);
  }

  @Annotations.ImplEntries
  public class ExtendableDrillBuild extends BaseDrillBuild implements ChainsBuildComp{
    public ChainsModule chains;
    public boolean valid = true;

    public Seq<ItemStack> ores = new Seq<>();

    boolean updatedMark;

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains = new ChainsModule(this);
      chains.newContainer();
      return this;
    }

    @Override
    public boolean updateValid(){
      return super.updateValid() && valid;
    }

    @Override
    public void onProximityUpdate(){
      noSleep();

      getMines(tile, block, ores);
      updatedMark = true;
    }

    @Override
    public void updateTile(){
      super.updateTile();
      chains.container.update();

      if(updatedMark){
        updateOres();
      }
    }

    @Override
    public void onChainsUpdated(){
      updateOres();
    }

    @SuppressWarnings("DuplicatedCode")
    public void updateOres(){
      updatedMark = false;

      outputItems.clear();

      for(ItemStack ore: ores){
        outputItems.add(ore.copy());
      }

      for(ChainsBuildComp comp: chains.container.all){
        if(comp instanceof ExtendableDrillBuild oth && oth != this){
          valid = false;
          return;
        }
        if(comp instanceof ExtendMiner.ExtendMinerBuild ext){
          t: for(ItemStack mine: ext.mines){
            for(ItemStack ores: outputItems){
              if(ores.item == mine.item){
                ores.amount += mine.amount;
                continue t;
              }
            }
            outputItems.add(mine.copy());
          }
        }

        valid = true;
      }

      boolean reset = mineOreItems.size != outputItems.size;
      if(!reset){
        for(ItemStack stack: outputItems){
          if(!mineOreItems.contains(stack.item)){
            reset = true;
            break;
          }
        }
      }
      if(reset){
        mineOreItems.clear();
        for(ItemStack stack: outputItems){
          mineOreItems.add(stack.item);
        }

        currentMines = new boolean[outputItems.size];
        progress = new float[outputItems.size];
        lastDrillSpeed = new float[outputItems.size];
        if(outputItems.size == 1) currentMines[0] = true;
      }
    }
  }
}
