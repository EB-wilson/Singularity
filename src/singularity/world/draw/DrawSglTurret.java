package singularity.world.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureRegion;
import arc.math.Rand;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.entities.part.DrawPart;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import singularity.Singularity;
import singularity.world.blocks.turrets.SglTurret;

public class DrawSglTurret extends DrawBlock{
  protected static final Rand rand = new Rand();

  public Seq<DrawPart> parts = new Seq<>();
  public String basePrefix = "";
  public @Nullable Liquid liquidDraw;
  public TextureRegion base, liquid, top, heat, preview, outline;

  public DrawSglTurret(DrawPart... parts){
    this.parts.addAll(parts);
  }

  public DrawSglTurret(String basePrefix, DrawPart... parts){
    this.basePrefix = basePrefix;
    this.parts.addAll(parts);
  }

  public DrawSglTurret(String basePrefix){
    this.basePrefix = basePrefix;
  }

  public DrawSglTurret(){
  }

  @Override
  public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
    Draw.rect(base, plan.drawx(), plan.drawy());
    Draw.rect(preview, plan.drawx(), plan.drawy());
  }

  @Override
  public void getRegionsToOutline(Block block, Seq<TextureRegion> out){
    for(var part : parts){
      part.getOutlines(out);
    }

    if(block.region.found() && !(block.outlinedIcon > 0 && block.getGeneratedIcons()[block.outlinedIcon].equals(block.region))){
      out.add(block.region);
    }
  }

  @Override
  public void draw(Building build){
    SglTurret turret = (SglTurret) build.block;
    SglTurret.SglTurretBuild tb = (SglTurret.SglTurretBuild)build;

    Draw.rect(base, build.x, build.y);
    Draw.color();

    Draw.z(Layer.turret - 0.5f);

    Drawf.shadow(preview, build.x + tb.recoilOffset.x - turret.elevation, build.y + tb.recoilOffset.y - turret.elevation, tb.drawrot());

    Draw.z(Layer.turret);

    drawTurret(turret, tb);
    drawHeat(turret, tb);

    if(parts.size > 0){
      if(outline.found()){
        //draw outline under everything when parts are involved
        Draw.z(Layer.turret - 0.01f);
        Draw.rect(outline, build.x + tb.recoilOffset.x, build.y + tb.recoilOffset.y, tb.drawrot());
        Draw.z(Layer.turret);
      }

      float progress = tb.progress();

      var params = DrawPart.params.set(build.warmup(), 1f - progress, 1f - progress, tb.heat, tb.curRecoil, tb.charge, tb.x + tb.recoilOffset.x, tb.y + tb.recoilOffset.y, tb.rotation);

      for(var part : parts){
        part.draw(params);
      }
    }
  }

  public void drawTurret(SglTurret block, SglTurret.SglTurretBuild build){
    if(block.region.found()){
      Draw.rect(block.region, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());
    }

    if(liquid.found()){
      Liquid toDraw = liquidDraw == null ? build.liquids.current() : liquidDraw;
      Drawf.liquid(liquid, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.liquids.get(toDraw) / block.liquidCapacity, toDraw.color.write(Tmp.c1).a(1f), build.drawrot());
    }

    if(top.found()){
      Draw.rect(top, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());
    }
  }

  public void drawHeat(SglTurret block, SglTurret.SglTurretBuild build){
    if(build.heat <= 0.00001f || !heat.found()) return;

    Drawf.additive(heat, block.heatColor.write(Tmp.c1).a(build.heat), build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot(), Layer.turretHeat);
  }

  /** Load any relevant texture regions. */
  @Override
  public void load(Block block){
    if(!(block instanceof SglTurret)) throw new ClassCastException("This drawer can only be used on turret(Sgl)s.");

    preview = Core.atlas.find(block.name + "_preview", block.region);
    outline = Core.atlas.find(block.name + "_outline");
    liquid = Core.atlas.find(block.name + "_liquid");
    top = Core.atlas.find(block.name + "_top");
    heat = Core.atlas.find(block.name + "_heat");
    base = Core.atlas.find(block.name + "_base");

    PixmapRegion image = Core.atlas.getPixmap(base);
    block.squareSprite = image.getA(0, 0) > 0.5f;

    for(var part : parts){
      part.turretShading = true;
      part.load(block.name);
    }

    if(!base.found() && block.minfo.mod != null) base = Core.atlas.find(block.minfo.mod.name + "-" + basePrefix + "block_" + block.size);
    if(!base.found()) base = Core.atlas.find(basePrefix + "block-" + block.size, Singularity.getModAtlas(basePrefix + "block-" + block.size));
  }

  /** @return the generated icons to be used for this block. */
  @Override
  public TextureRegion[] icons(Block block){
    return top.found() ? new TextureRegion[]{base, preview, top} : new TextureRegion[]{base, preview};
  }
}
