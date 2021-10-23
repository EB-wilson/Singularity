package singularity.world.blocks.environment;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.g2d.PixmapRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.graphics.MultiPacker;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.OverlayFloor;
import singularity.Sgl;
import singularity.core.UpdateTiles;
import singularity.type.Gas;
import singularity.world.SglFx;

import static mindustry.Vars.tilesize;

public class SglOverlay extends OverlayFloor implements UpdateTiles.Updatable{
  public Gas gas = null;
  public float gasPressure = 1.0f;
  public boolean pumpable = false;
  
  public SglOverlay(String name){
    super(name);
    update = true;
  }
  
  public SglOverlay(String name, Gas gas, float pressure){
    this(name);
    this.gas = gas;
    this.gasPressure = pressure;
    this.mapColor = gas.color;
  }
  
  @Override
  public void createIcons(MultiPacker packer){
    super.createIcons(packer);
    for(int i = 0; i < variants; i++){
      PixmapRegion shadow = Core.atlas.getPixmap(name + (i + 1));
      Pixmap image = shadow.crop();
      
      int offset = image.width / tilesize - 1;
      int shadowColor = Color.rgba8888(0, 0, 0, 0.3f);
      
      for(int x = 0; x < image.width; x++){
        for(int y = offset; y < image.height; y++){
          if(shadow.getA(x, y) == 0 && shadow.getA(x, y - offset) != 0){
            image.setRaw(x, y, shadowColor);
          }
        }
      }
      
      packer.add(MultiPacker.PageType.environment, name + (i + 1), image);
      packer.add(MultiPacker.PageType.editor, "editor-" + name + (i + 1), image);
      
      if(i == 0){
        packer.add(MultiPacker.PageType.editor, "editor-block-" + name + "-full", image);
        packer.add(MultiPacker.PageType.main, "block-" + name + "-full", image);
      }
    }
  }
  
  @Override
  public void drawBase(Tile tile){
    super.drawBase(tile);
    Sgl.updateTiles.add(tile, this);
  }
  
  public void update(Tile tile){
    if(tile.build != null) return;
    
    float atmoPressure = Sgl.atmospheres.current.getCurrPressure();
    float diff = Mathf.maxZero(gasPressure - atmoPressure);
    float rate = Math.min(diff/Math.max(gasPressure, atmoPressure)/2, 0.35f)*Time.delta;
    
    if(Math.random() < rate){
      SglFx.gasLeak.at(tile.worldx(), tile.worldy(), 0, gas.color, diff);
    }
  }
}
