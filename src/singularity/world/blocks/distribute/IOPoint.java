package singularity.world.blocks.distribute;

import arc.func.Cons;
import arc.math.geom.Point2;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.world.Edges;
import singularity.contents.DistributeBlocks;
import singularity.world.blocks.SglBlock;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.components.distnet.IOPointBlockComp;
import singularity.world.components.distnet.IOPointComp;
import singularity.world.meta.SglStat;
import universecore.annotations.Annotations;
import universecore.util.DataPackable;

@Annotations.ImplEntries
public abstract class IOPoint extends SglBlock implements IOPointBlockComp{
  public static final byte[] NULL = new byte[0];

  public IOPoint(String name){
    super(name);
    update = true;
    buildCostMultiplier = 0;

    schematicPriority = -10;
  }

  @Override
  public void init(){
    super.init();

    setupRequestFact();
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.add(SglStat.componentBelongs, t -> {
      t.defaults().left();
      t.image(DistributeBlocks.matrix_controller.fullIcon).size(35).padRight(8);
      t.add(DistributeBlocks.matrix_controller.localizedName);
    });
  }

  @Override
  public void parseConfigObjects(SglBuilding e, Object obj) {
    if (obj instanceof TargetConfigure cfg && e instanceof IOPointBuild io){
      Building tile = e.nearby(-Point2.x(cfg.offsetPos), -Point2.y(cfg.offsetPos));
      if (!(tile instanceof DistMatrixUnitBuildComp mat)){
        io.parent(null);
        io.gridConfig(null);
      }
      else{
        //校准坐标...
        int offX = e.tileX() - tile.tileX();
        int offY = e.tileY() - tile.tileY();

        cfg.offsetPos = Point2.pack(offX, offY);

        io.parent(mat);
        io.gridConfig(cfg);
        io.parent().addIO(io);
      }
    }
  }

  @Override
  public Object pointConfig(Object config, Cons<Point2> transformer){
    if(config instanceof byte[] b && DataPackable.readObject(b) instanceof TargetConfigure cfg){
      cfg.configHandle(transformer);
      return cfg.pack();
    }
    return config;
  }

  @Override
  public boolean unlocked(){
    return DistributeBlocks.matrix_controller.unlocked();
  }

  @Override
  public boolean unlockedNow(){
    return DistributeBlocks.matrix_controller.unlockedNow();
  }

  @Override
  public boolean unlockedNowHost(){
    return DistributeBlocks.matrix_controller.unlockedNowHost();
  }

  abstract public void setupRequestFact();

  @Annotations.ImplEntries
  public abstract class IOPointBuild extends SglBuilding implements IOPointComp{
    public DistMatrixUnitBuildComp parentMat;
    public TargetConfigure config;

    @Override
    public void onProximityAdded() {
      super.onProximityAdded();
      if (config != null){
        Building tile = nearby(-Point2.x(config.offsetPos), -Point2.y(config.offsetPos));
        if (!(tile instanceof DistMatrixUnitBuildComp mat)){
          parentMat = null;
          config = null;
        }
        else{
          parentMat = mat;
          parentMat.addIO(this);
        }
      }
    }

    @Override
    public void updateTile(){
      if(parentMat == null || !parentMat.getBuilding().isAdded()){
        return;
      }
      if(parentMat.gridValid()){
        resourcesDump();
        resourcesSiphon();
        transBack();
      }
    }

    @Override
    public void onRemoved(){
      if(parentMat != null) parentMat.removeIO(this);
      super.onRemoved();
    }

    @Override
    public byte[] config() {
      return config == null? NULL : config.pack();
    }

    @Override
    public byte version(){
      return 3;
    }

    @Override
    public void write(Writes write){
      super.write(write);

      if (config == null){
        write.i(-1);
      }
      else {
        write.i(1);
        config.write(write);
      }
    }

    @Override
    public void read(Reads read, byte revision){
      super.read(read, revision);

      if(revision >= 3){
        if (read.i() > 0){
          config = new TargetConfigure();
          config.read(read);
        }
      }
    }

    public byte getDirectBit(Building e){
      byte dir = relativeTo(Edges.getFacingEdge(e, this));
      return (byte) (dir == 0? 1: dir == 1? 2: dir == 2? 4: dir == 3? 8: 0);
    }

    protected abstract void transBack();

    protected abstract void resourcesSiphon();

    protected abstract void resourcesDump();
  }
}
