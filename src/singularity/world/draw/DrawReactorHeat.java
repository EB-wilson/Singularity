package singularity.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import singularity.graphic.SglDrawConst;
import singularity.world.blocks.nuclear.NuclearReactor;

import static mindustry.Vars.tilesize;

public class DrawReactorHeat extends DrawBlock{
  public Color lightColor = Color.valueOf("7f19ea");
  public Color coolColor = new Color(1, 1, 1, 0f);
  public Color hotColor = Color.valueOf("ff9575a3");
  public float flashThreshold = 46f;

  public TextureRegion lightsRegion;

  @Override
  public void load(Block block){
    lightsRegion = Core.atlas.find(block.name + "_light");
  }

  @Override
  public void draw(Building build){
    NuclearReactor.NuclearReactorBuild e = (NuclearReactor.NuclearReactorBuild) build;

    Draw.color(coolColor, hotColor, e.heat/e.block().maxHeat);
    Fill.rect(e.x, e.y, e.block.size * tilesize, e.block.size * tilesize);

    if(e.heat > flashThreshold){
      e.handleVar("flash", (Float f) -> f + (1f + ((e.heat - flashThreshold) / (e.block().maxHeat - flashThreshold)) * 5.4f) * Time.delta, 0f);
      Draw.color(Color.red, Color.yellow, Mathf.absin(e.getVar("flash"), 9f, 1f));
      Draw.alpha(0.3f);
      Draw.rect(lightsRegion, e.x, e.y);
    }

    Draw.reset();
  }

  @Override
  public void drawLight(Building build){
    NuclearReactor.NuclearReactorBuild e = (NuclearReactor.NuclearReactorBuild) build;
    float smoothLight = e.smoothEfficiency;
    Drawf.light(e.x, e.y, (90f + Mathf.absin(5, 5f)) * smoothLight,
        Tmp.c1.set(lightColor).lerp(Color.scarlet, e.heat), 0.6f * smoothLight);
  }

  @Override
  public TextureRegion[] icons(Block block){
    return SglDrawConst.EMP_REGIONS;
  }
}
