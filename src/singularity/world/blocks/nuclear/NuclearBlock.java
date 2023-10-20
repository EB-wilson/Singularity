package singularity.world.blocks.nuclear;

import arc.func.Floatc;
import arc.func.Floatp;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import singularity.world.blocks.SglBlock;
import singularity.world.meta.SglBlockGroup;

public class NuclearBlock extends SglBlock{
  public NuclearBlock(String name){
    super(name);
    hasEnergy = true;
    solid = true;
    update = true;
    group = SglBlockGroup.nuclear;
  }

  protected void buildEnergySlider(Table sli, float min, float max, Floatp yet, Floatc slid){
    sli.button(Icon.leftOpen, Styles.clearNonei, () -> slid.get(Mathf.clamp(Mathf.pow(2, (int) Mathf.log2(yet.get()) - 1), min, max))).size(32);
    sli.slider(Mathf.log2(min), Mathf.log2(max), 0.01f, Mathf.log2(yet.get()), f -> slid.get(Mathf.pow(2, f))).size(200, 40).padLeft(8).padRight(8).update(s -> s.setValue(Mathf.log2(yet.get())));
    sli.button(Icon.rightOpen, Styles.clearNonei, () -> slid.get(Mathf.clamp(Mathf.pow(2, (int) Mathf.log2(yet.get()) + 1), min, max))).size(32);
    sli.add("").update(lable -> lable.setText(Mathf.round(yet.get()) + "NF"));
  }
}
