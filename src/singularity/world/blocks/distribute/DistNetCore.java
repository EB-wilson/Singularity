package singularity.world.blocks.distribute;

import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.modules.PowerModule;
import singularity.world.blocks.distribute.netcomponents.CoreNeighbourComponent;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.components.distnet.DistNetworkCoreComp;
import singularity.world.consumers.SglConsumeEnergy;
import singularity.world.consumers.SglConsumeType;
import singularity.world.consumers.SglConsumers;
import singularity.world.distribution.DistBuffers;
import singularity.world.modules.DistCoreModule;
import singularity.world.modules.NuclearEnergyModule;
import singularity.world.modules.SglConsumeModule;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.UncConsumePower;
import universecore.world.consumers.UncConsumeType;

import static mindustry.Vars.tilesize;

public class DistNetCore extends DistNetBlock{
  public int computingPower = 32;
  public int frequencyOffer = 8;
  public ObjectMap<DistBuffers<?>, Integer> bufferSize = ObjectMap.of(
      DistBuffers.itemBuffer, 256,
      DistBuffers.liquidBuffer, 256
  );

  public DistNetCore(String name){
    super(name);
    frequencyUse = 0;
    hasItems = true;
    hasLiquids = true;
  }

  @Override
  public void init(){
    super.init();
    hasEnergy = hasPower = false;
    consumeEnergy = consumesPower = true;

    initPower();
  }

  @Annotations.ImplEntries
  public class DistNetCoreBuild extends DistNetBuild implements DistNetworkCoreComp{
    SglConsumers dynamicCons = new SglConsumers(false);

    DistCoreModule distCore;
    boolean inited;

    Seq<CoreNeighbourComponent.CoreNeighbourComponentBuild> proximityComps = new Seq<>();

    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();

      netLinked.removeAll(proximityComps);

      proximityComps.clear();
      for(Building building: proximity){
        if(building instanceof CoreNeighbourComponent.CoreNeighbourComponentBuild comp) proximityComps.add(comp);
      }

      netLinked.addAll(proximityComps);
    }

    @Override
    public ObjectMap<DistBuffers<?>, Integer> bufferSize(){
      return bufferSize;
    }

    @Override
    public int computingPower(){
      return computingPower;
    }

    @Override
    public int frequencyOffer(){
      return frequencyOffer;
    }

    @Override
    public boolean componentValid(){
      return true;
    }

    @Override
    public void networkValided(){
      super.networkValided();

      dynamicCons = new SglConsumers(false);
      for(DistElementBuildComp element: distributor.network.elements){
        if(element instanceof ConsumerBuildComp consumer && consumer.consumer() != null){
          BaseConsumers cons = consumer.consumer().current;
          if(cons != null) for(BaseConsume<? extends ConsumerBuildComp> consume: cons.all()){
            dynamicCons.add(consume);
          }
        }
      }
    }

    @Override
    public Building create(Block block, Team team){
      distCore = new DistCoreModule(this);
      super.create(block, team);
      items = distCore.getBuffer(DistBuffers.itemBuffer).generateBindModule();
      liquids = distCore.getBuffer(DistBuffers.liquidBuffer).generateBindModule();

      power = new PowerModule();
      energy = new NuclearEnergyModule(this, 0, false);
      energy.setNet();

      consumer = new SglConsumeModule(this){
        @Override
        public void setCurrent(){
          current = dynamicCons;
        }

        @Override
        public boolean hasConsume(){
          return true;
        }

        @Override
        public boolean hasOptional(){
          return false;
        }
      };

      inited = true;
      recipeCurrent = 0;
      recipeSelected = true;
      onUpdateCurrent();

      return this;
    }

    @Override
    public float powerEfficiency(){
      if(consumer.current == null) return 0;
      UncConsumePower<?> cp = consumer.current.get(UncConsumeType.power);
      return cp == null? 1f: cp.buffered? 1f: power.status;
    }

    @Override
    public float energyEfficiency(){
      if(consumer.current == null) return 0f;
      SglConsumeEnergy<?> ce = consumer.current.get(SglConsumeType.energy);
      return ce == null? 1f: ce.buffer? 1f: Mathf.clamp(energy.getEnergy() / (ce.usage*60));
    }

    @Override
    public int consumeCurrent(){
      return inited ? 0: -1;
    }

    @Override
    public void drawSelect(){
      super.drawSelect();
      Lines.stroke(1f, Pal.accent);
      Cons<Building> outline = b -> {
        for(int i = 0; i < 4; i++){
          Point2 p = Geometry.d8edge[i];
          float offset = -Math.max(b.block.size - 1, 0) / 2f * tilesize;
          Draw.rect("block-select", b.x + offset * p.x, b.y + offset * p.y, i * 90);
        }
      };
      outline.get(this);
      proximityComps.each(outline);
    }

    @Override
    public float netEff(){
      return consEfficiency();
    }

    @Override
    public boolean shouldConsume(){
      return distributor.network.netStructValid() && super.shouldConsume();
    }
  }
}
