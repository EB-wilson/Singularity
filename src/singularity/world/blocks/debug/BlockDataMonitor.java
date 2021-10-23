package singularity.world.blocks.debug;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import singularity.Singularity;
import singularity.ui.dialogs.BlockDataDialog;
import singularity.world.blocks.debug.VarsContainer.VarsContainerBuild;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class BlockDataMonitor extends Block{
  public float range = 20;
  
  public BlockDataMonitor(String name){
    super(name);
    update = true;
    hasItems = false;
    hasLiquids = false;
    hasPower = false;
    configurable = true;
    
    config(Integer.class, (BlockDataMonitorBuild e, Integer p) -> {
      Tile tile = Vars.world.tile(p);
      if(tile == null || tile.build == null) return;
      Building other = tile.build;
      
      if(e.targets.contains(other)){
        if(other instanceof VarsContainerBuild){
          e.varsContainer = null;
          e.dataDialog.unloadVars();
        }
        else e.targets.remove(other);
      }
      else{
        if(other.block instanceof VarsContainer){
          e.varsContainer = (VarsContainerBuild)other;
          e.dataDialog.setVars(e.varsContainer.vars);
        }
        else e.targets.add(other);
      }
    });
  }
  
  public boolean inRange(Tile origin, Tile other, float range){
    return Intersector.overlaps(Tmp.cr1.set(origin.drawx(), origin.drawy(), range), other.getHitbox(Tmp.r1));
  }
  
  public boolean inRange(Building origin, Building other, float range){
    return inRange(origin.tile, other.tile, range);
  }
  
  public class BlockDataMonitorBuild extends Building{
    public VarsContainerBuild varsContainer;
    
    public final Seq<Building> targets = new Seq<>();
    public final BlockDataDialog dataDialog = new BlockDataDialog(targets);
    
    @Override
    public void buildConfiguration(Table table){
      ImageButton button = new ImageButton(Singularity.getModAtlas("data_monitor"));
      button.clicked(() -> {
        targets.removeAll(e -> world.tile(e.tile.pos()).build != e);
        dataDialog.build();
        dataDialog.show();
      });
      table.add(button).size(50, 50);
    }
  
    @Override
    public boolean onConfigureTileTapped(Building other){
      if(other == this){
        targets.clear();
        return false;
      }
      else if(other != null && inRange(this, other, range*tilesize)){
        configure(other.tile.pos());
        return false;
      }
      
      return true;
    }
  
    @Override
    public void drawConfigure(){
      Draw.color(Pal.accent);
      
      Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
      Drawf.circles(x, y, range * tilesize);
      
      Draw.color(Color.purple);
      
      for(int dx = (int)(x - range - 2); dx <= x + range + 2; dx++){
        for(int dy = (int)(y - range - 2); dy <= y + range + 2; dy++){
          Building link = world.build(dx, dy);
          if(link == null) continue;
      
          if(link != this && inRange(this, link, range*tilesize)){
            if(targets.contains(link)){
              Drawf.square(link.x, link.y, link.block.size * tilesize / 2f + 1f);
              Drawf.square(link.x, link.y, link.block.size * tilesize / 3f, Time.time/2);
            }
            else{
              float radius = tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f);
              Drawf.circles(link.x, link.y, radius);
              for(int i=0; i<4; i++){
                float rotation = Time.time/2 + i*90;
                Drawf.square((float)(link.x + radius*Math.cos(rotation)), (float)(link.y + radius*Math.sin(rotation)), 2);
              }
            }
          }
        }
      }
      
      Draw.reset();
    }
  
    @Override
    public void drawSelect(){
      super.drawSelect();
    
      Lines.stroke(1f);
    
      Draw.color(Pal.accent);
      Drawf.circles(x, y, range * tilesize);
      Draw.reset();
    }
  }
}
