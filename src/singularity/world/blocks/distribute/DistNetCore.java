package singularity.world.blocks.distribute;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import singularity.Sgl;
import singularity.world.components.distnet.DistComponent;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.modules.DistCoreModule;

public class DistNetCore extends DistNetBlock{
  public int computingPower = 32;
  public int frequencyOffer = 8;

  public DistNetCore(String name){
    super(name);
    frequencyUse = 0;
    configurable = true;
    hasItems = true;
    hasLiquids = true;
    hasGases = true;
  }
  
  public class DistNetCoreBuild extends DistNetBuild implements DistNetworkCoreComp, DistComponent{
    DistCoreModule distCore;

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public int frequencyOffer(){
      return frequencyOffer;
    }

    @Override
    public Building create(Block block, Team team){
      distCore = new DistCoreModule(this);
      super.create(block, team);
      items = distCore.getBuffer(DistBuffers.itemBuffer).generateBindModule();
      return this;
    }
  
    @Override
    public DistCoreModule distCore(){
      return distCore;
    }
  
    @Override
    public void updateTile(){
      super.updateTile();
      updateDistNetwork();
    }

    @Override
    public void buildConfiguration(Table table){
      table.button(t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image().size(40).size(50));
        t.table(b -> {
          b.table(text -> {
            text.defaults().grow().left();
            text.add(Core.bundle.get("infos.statIO")).color(Pal.accent);
            text.row();
            text.add(Core.bundle.get("infos.statIOSubscript"));
          }).grow().right().padLeft(8);
        }).size(258, 50).padLeft(8);
      }, () -> Sgl.ui.bufferStat.show(distCore.buffers.values())).size(316, 50);
    }
  }
}
