package singularity.world.blocks.distribute.netcomponents;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Edges;
import mindustry.world.Tile;
import singularity.world.blocks.distribute.DistNetBlock;
import singularity.world.components.distnet.DistElementBuildComp;
import singularity.world.distribution.DistributeNetwork;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ChainsBlockComp;
import universecore.components.blockcomp.ChainsBuildComp;
import universecore.components.blockcomp.SpliceBlockComp;
import universecore.components.blockcomp.SpliceBuildComp;
import universecore.world.DirEdges;
import universecore.world.blocks.modules.ChainsModule;

import java.util.Arrays;

@Annotations.ImplEntries
public class ComponentInterface extends DistNetBlock implements SpliceBlockComp {
  TextureRegion interfaceLinker, linker;

  public int maxChainsWidth = 40, maxChainsHeight = 40;

  public ComponentInterface(String name){
    super(name);

    isNetLinker = true;

  }

  @Override
  public void load(){
    super.load();

    interfaceLinker = Core.atlas.find(name + "_linker");
    linker = Core.atlas.find(name + "_comp_linker");
  }

  @Override
  public boolean chainable(ChainsBlockComp other) {
    return other == this;
  }

  @Annotations.ImplEntries
  public class ComponentInterfaceBuild extends DistNetBuild implements SpliceBuildComp {
    public ChainsModule chains;

    public Seq<ComponentInterfaceBuild> links = new Seq<>();
    public Seq<DistElementBuildComp> connects = new Seq<>();

    public byte interSplice = 0;
    public byte[] connectSplice = new byte[4];

    boolean mark;

    @Override
    public Building init(Tile tile, Team team, boolean shouldAdd, int rotation){
      super.init(tile, team, shouldAdd, rotation);
      chains = new ChainsModule(this);
      chains.newContainer();
      return this;
    }

    @Override
    public void updateTile() {
      super.updateTile();

      if (mark){
        updateNetLinked();
        new DistributeNetwork().flow(this);
        mark = false;
      }
    }

    @Override
    public void updateNetLinked() {
      super.updateNetLinked();

      links.clear();
      connects.clear();

      Arrays.fill(connectSplice, (byte) 0);

      for (Building building : proximity) {
        if (building instanceof ComponentInterfaceBuild inter && canChain(inter)){
          links.add(inter);
        }
        else if (building instanceof DistNetBuild device && linkable(device) && device.linkable(this) && connectable(device)){
          connects.add(device);

          int dir = relativeTo(device);
          Point2[] arr = DirEdges.get(size, dir);

          for (int i = 0; i < arr.length; i++) {
            Tile t = tile.nearby(arr[i]);
            if (t != null && t.build == device) connectSplice[dir] |= (byte) (1 << i);
          }
        }
      }

      netLinked().addAll(links).addAll(connects);
    }

    @Override
    public void onProximityAdded() {
      super.onProximityAdded();

      mark = true;
    }

    @Override
    public void onProximityUpdate() {
      super.onProximityUpdate();

      updateNetLinked();
      mark = true;
    }

    @Override
    public void networkRemoved(DistElementBuildComp remove) {
      super.networkRemoved(remove);

      connects.remove(remove);
      if (remove instanceof ComponentInterfaceBuild inter) links.remove(inter);

      mark = true;
    }

    @Override
    public void updateRegionBit() {
      SpliceBuildComp.super.updateRegionBit();

      interSplice = 0;
      for (int i = 0; i < 4; i++) {
        if ((splice() & (1 << i*2)) != 0) interSplice |= (byte) (1 << i);
      }
    }

    public boolean connectable(DistNetBuild other){
      int dir = other.relativeTo(this);
      Tile t = other.tile;
      for (Point2 point2 : DirEdges.get(other.block.size, dir)) {
        Tile ot = t.nearby(point2);
        if (ot == null || !(ot.build instanceof ComponentInterfaceBuild inter)) return false;

        if (inter != this && inter.distributor.network != distributor.network) return false;
      }

      return true;
    }

    @Override
    public boolean canChain(ChainsBuildComp other) {
      return SpliceBuildComp.super.canChain(other) && (other.tileX() == tileX() || other.tileY() == tileY());
    }
  }
}
