package singularity.world.blocks.gas;

import arc.Core;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.consumers.ConsumePower;
import singularity.Sgl;
import singularity.Singularity;
import singularity.type.Gas;
import singularity.type.SglContents;
import singularity.ui.SglStyles;
import singularity.world.blockComp.GasBuildComp;
import singularity.world.blocks.environment.SglOverlay;

import java.util.Arrays;

public class GasCompressor extends GasBlock{
  public float pumpGasSpeed = 0.2f;
  public float pressurePowerScl = 0.4f;
  public float pumpingPowerScl = 1.8f;
  
  public boolean hasPump = true;
  public boolean pumpOnly = false;
  
  public float minPressure = 0.22f;
  public Func<GasCompressorBuild, Float> acceptPressure = entity -> Mathf.lerp(entity.gases.getPressure(), minPressure, entity.power.status);
  
  public Func<GasCompressorBuild, Float> compressPowerCons = entity -> entity.currentPressure*pressurePowerScl*(entity.gasPumping? pumpingPowerScl: 1);
  
  public final Seq<SglOverlay> floors = new Seq<>();
  
  public GasCompressor(String name){
    super(name);
    consumesPower = hasPower = hasItems = hasLiquids = true;
    outputsLiquid = outputGases = true;
    configurable = true;
    sync = true;
  }
  
  @Override
  public void appliedConfig(){
    config(Float.class, (GasCompressorBuild e, Float f) -> {
      e.currentPressure = f;
    });
    config(Boolean.class, (GasCompressorBuild e, Boolean b) -> {
      e.gasPumping = b;
    });
    
    config(IntSeq.class, (GasCompressorBuild e, IntSeq f) -> {
      e.currentPressure = ((float)f.get(0))/100000;
      e.gasPumping = f.get(1) > 0;
    });
    configClear((GasCompressorBuild e) -> {
      e.currentPressure = 0;
      e.gasPumping = false;
    });
  }
  
  @Override
  public void init(){
    super.init();
    hasPump |= pumpOnly;
  }
  
  @Override
  public void initPower(float powerCapacity){
    consumes.add(new ConsumePower(1, powerCapacity, false){
      @Override
      public float requestedPower(Building e){
        GasCompressorBuild entity = (GasCompressorBuild) e;
        return compressPowerCons.get(entity);
      }
    });
  }
  
  @Override
  public void setBars(){
    super.setBars();
  
    if(hasGases) bars.add("gasPressure", e -> {
      GasBuildComp entity = (GasBuildComp) e;
      return new Bar(
          () -> Core.bundle.get("fragment.bars.gasPressure") + ":" + Strings.autoFixed(entity.gases().getPressure()*100, 2) + "kPa",
          () -> Pal.accent,
          () -> Math.min(entity.gases().getPressure() / maxGasPressure, 1));
    });
  }
  
  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    if(!hasPump) return;
    Tile tile = Vars.world.tile(x, y);
    if(tile == null) return;
    
    getGasFloor(tile, false);
    ObjectSet<SglOverlay> showTiles = new ObjectSet<>();
    floors.each(showTiles::add);
    if(floors.size > 0){
      int line = 0;
      for(SglOverlay floor: showTiles){
        float width = floor.gasPressure > minPressure?
            //可挖掘的矿物显示
            drawPlaceText(floor.gas.localizedName, x, y - line, true):
            //不可挖掘的矿物显示
            drawPlaceText(Core.bundle.get("bar.lowerPressure"), x, y - line, false);
        float dx = x * Vars.tilesize + offset - width/2f - 4f, dy = y * Vars.tilesize + offset + size * Vars.tilesize / 2f + 5 - line*9f;
        Draw.mixcol(Color.darkGray, 1f);
        Draw.rect(floor.gas.uiIcon, dx, dy - 1);
        Draw.reset();
        Draw.rect(floor.gas.uiIcon, dx, dy);
        line++;
      }
    }
  }
  
  public Seq<SglOverlay> getGasFloor(Tile tile, boolean filter){
    floors.clear();
    if(isMultiblock()){
      for(Tile other: tile.getLinkedTilesAs(this, tempTiles)){
        if(filter? canPump(other): hasGas(other)){
          floors.add((SglOverlay)other.overlay());
        }
      }
    }
    else{
      if(filter? canPump(tile): hasGas(tile)) floors.add((SglOverlay)tile.overlay());
    }
    return floors;
  }
  
  public boolean hasGas(Tile tile){
    if(!(tile.overlay() instanceof SglOverlay)) return false;
    SglOverlay gFloor = (SglOverlay) tile.overlay();
    return gFloor.gas != null && gFloor.pumpable;
  }
  
  public boolean canPump(Tile tile){
    return hasGas(tile) && ((SglOverlay)tile.overlay()).gasPressure > minPressure;
  }
  
  public class GasCompressorBuild extends SglBuilding{
    public float currentPressure = minPressure;
    public boolean gasPumping, pumpingAtmo = true;
    
    public Seq<SglOverlay> pumpingFloors;
    public float[] pumpRate = new float[SglContents.gases().size];
    public float[] smoothRate = new float[SglContents.gases().size];
  
    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      gasPumping = pumpOnly;
      
      return this;
    }
  
    @Override
    public void onProximityUpdate(){
      super.onProximityUpdate();
      pumpingFloors = getGasFloor(tile, true).copy();
      pumpingAtmo = pumpingFloors.size == 0;
    }
  
    @Override
    public boolean acceptGas(GasBuildComp source, Gas gas){
      return source.getBuilding().team == getBuilding().team && source.getGasBlock().hasGases() && !gasPumping &&
          source.outputPressure() > acceptPressure.get(this) && gases.getPressure() < currentPressure;
    }
  
    @Override
    public float pressure(){
      return acceptPressure.get(this);
    }
    
    @Override
    public void updateTile(){
      if(gasPumping && gases.getPressure() < currentPressure){
        if(pumpingAtmo){
          float atmoPressure = Sgl.atmospheres.current.getCurrPressure();
          if(gases.getPressure() < currentPressure){
            float dumping = pumpGasSpeed*Mathf.maxZero((currentPressure - atmoPressure)/atmoPressure)/10*edelta();
            gases.distributeAtmo(dumping);
          }
        }
        else{
          Arrays.fill(pumpRate, 0);
          if(gases.getPressure() < currentPressure){
            for(SglOverlay floor : pumpingFloors){
              float diff = Mathf.maxZero(floor.gasPressure - acceptPressure.get(this));
              float delta = diff*pumpGasSpeed;
    
              handleGas(this, floor.gas, delta*edelta());
              pumpRate[floor.gas.id] += delta;
            }
            for(int i = 0; i < pumpRate.length; i++){
              smoothRate[i] = Mathf.lerpDelta(smoothRate[i], pumpRate[i], 0.15f);
            }
          }
        }
      }
      
      dumpGas();
    }
  
    @Override
    public void setBars(Table table){
      super.setBars(table);
      for(int id=0; id<pumpRate.length; id++){
        float rate = pumpRate[id];
        if(rate <= 0.0001) continue;
        
        Gas gas = SglContents.gas(id);
        int finalId = id;
        Func<GasCompressorBuild, Bar> bar = (e -> new Bar(
            () -> gas.localizedName + " : " + Core.bundle.format("bar.pumpSpeed", Strings.fixed(e.smoothRate[finalId]*60, 1)),
            () -> Pal.ammo,
            () -> e.power.status
        ));
        table.add(bar.get(this)).growX();
        table.row();
      }
    }
  
    @Override
    public float outputPressure(){
      return gases.getPressure();
    }
  
    @Override
    public void buildConfiguration(Table table){
      TextureRegion pumpIcon = Singularity.getModAtlas("icon_gas_pump");
      TextureRegion compressorIcon = Singularity.getModAtlas("icon_gas_compress");
      
      if(!pumpOnly && hasPump){
        table.table(Styles.black6, t -> {
          t.defaults().pad(0).margin(0);
          t.table(Tex.buttonTrans, i -> i.image().size(40).update(image -> image.setDrawable(gasPumping ? pumpIcon : compressorIcon))).size(50);
          t.table(b -> {
            b.check("", gasPumping, this::configure).left();
            b.table(text -> {
              text.defaults().grow().left();
              text.add(Core.bundle.get("infos.pumpMode")).color(Pal.accent);
              text.row();
              text.add("").update(l -> {
                l.setText(gasPumping ? pumpingAtmo? Core.bundle.get("infos.pumpAtomGassing"): Core.bundle.get("infos.pumpGassing"): Core.bundle.get("infos.compressing"));
              });
            }).grow().right().padLeft(8);
          }).size(258, 50).padLeft(8);
        }).size(316, 50);
        table.row();
      }
      
      table.table(Styles.black6, t -> {
        t.defaults().pad(0).margin(0);
        t.table(Tex.buttonTrans, i -> i.image(Singularity.getModAtlas("icon_pressure")).size(40)).size(50);
        t.slider(minPressure, maxGasPressure, 0.01f, currentPressure, this::configure).size(200, 50).padLeft(8).padRight(8).get().setStyle(SglStyles.sliderLine);
        t.add("0").size(50).update(lable -> lable.setText(Strings.autoFixed(currentPressure*100, 2) + "kPa"));
      });
    }
  
    @Override
    public Object config(){
      IntSeq data = new IntSeq();
      data.add((int)Math.floor(currentPressure*100000));
      data.add(gasPumping? 1: -1);
      return data;
    }
  
    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);
      gasPumping = read.bool();
      currentPressure = read.f();
    }
  
    @Override
    public void write(Writes write){
      super.write(write);
      write.bool(gasPumping);
      write.f(currentPressure);
    }
  }
}
