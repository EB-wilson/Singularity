package singularity.world.consumers;

import arc.math.Mathf;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.core.UI;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.ui.Styles;
import mindustry.world.meta.Stats;
import singularity.graphic.SglDrawConst;
import singularity.world.components.NuclearEnergyBuildComp;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsume;
import universecore.world.consumers.ConsumeType;

public class SglConsumeEnergy<T extends Building & NuclearEnergyBuildComp & ConsumerBuildComp> extends BaseConsume<T>{
  public boolean buffer = false;
  public float usage;

  public SglConsumeEnergy(float usage){
    this.usage = usage;
  }
  
  public void buffer(){
    this.buffer = true;
  }
  
  @Override
  public ConsumeType<SglConsumeEnergy<?>> type(){
    return SglConsumeType.energy;
  }

  @Override
  public void buildIcons(Table table) {
    buildNuclearIcon(table, usage);
  }

  public static void buildNuclearIcon(Table table, float amount) {
    table.stack(
        new Table(o -> {
          o.left();
          o.add(new Image(SglDrawConst.nuclearIcon)).size(32f).scaling(Scaling.fit);
        }),
        new Table(t -> {
          t.left().bottom();
          t.add(amount*60 >= 1000 ? UI.formatAmount((long) (amount*60))+ "NF/s" : amount*60 + "NF/s").style(Styles.outlineLabel);
          t.pack();
        })
    );
  }

  @Override
  public void merge(BaseConsume<T> baseConsume){
    if(baseConsume instanceof SglConsumeEnergy cons){
      buffer |= cons.buffer;
      usage += cons.usage;

      return;
    }
    throw new IllegalArgumentException("only merge consume with same type");
  }

  @Override
  public void consume(T entity){
    if(buffer) entity.handleEnergy(-usage*60*multiple(entity));
  }

  @Override
  public void update(T entity) {
    if(!buffer){
      entity.handleEnergy(-usage*parent.delta(entity));
    }
  }

  @Override
  public void display(Stats stats) {
    stats.add(SglStat.consumeEnergy, usage*60, SglStatUnit.neutronFluxSecond);
  }

  @Override
  public void build(T entity, Table table) {
    table.row();
  }

  @Override
  public float efficiency(T entity){
    if(entity.energy() == null) return 0;
    if(buffer){
      return entity.energy().getEnergy() >= usage*60*multiple(entity)? 1: 0;
    }
    return Mathf.clamp(entity.energy().getEnergy()/(usage*12.5f*multiple(entity)));
  }

  @Override
  public Seq<Content> filter(){
    return null;
  }
}
