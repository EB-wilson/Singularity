package singularity.world.blocks.nuclear;

import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.actions.Actions;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;

public class EnergyBuffer extends NuclearNode {
  public float minPotential = 128;
  public float maxPotential = 1024;

  public EnergyBuffer(String name) {
    super(name);
  }

  @Override
  public void setStats() {
    super.setStats();

    stats.add(SglStat.minEnergyPotential, minPotential, SglStatUnit.neutronFlux);
    stats.add(SglStat.maxEnergyPotential, maxPotential, SglStatUnit.neutronFlux);
  }

  @Override
  public void appliedConfig() {
    super.appliedConfig();

    config(Object[].class, (EnergyBufferBuild b, Object[] arr) -> {
      if (arr[0] instanceof Boolean bool){
        if (bool) b.output = (float) arr[1];
        else b.input = (float) arr[1];
      }
    });
  }

  public class EnergyBufferBuild extends NuclearNodeBuild {
    public float input = minPotential, output = minPotential;

    boolean showing;
    Runnable show = () -> {}, close = () -> {};

    @Override
    public boolean onConfigureBuildTapped(Building other) {
      if (other == this){
        if (!showing){
          show.run();
          showing = true;
        }
        else {
          close.run();
          showing = false;
        }
        return false;
      }
      else return super.onConfigureBuildTapped(other);
    }

    @Override
    public void buildConfiguration(Table table) {
      showing = false;
      table.table(t -> {
        t.visible = false;
        t.setOrigin(Align.center);

        t.table(Tex.pane, ta -> {
          ta.left().defaults().left();
          ta.table(sli -> {
            sli.defaults().pad(0).margin(0);
            sli.table(Tex.buttonTrans, i -> i.image(Icon.download).size(40)).size(50);
            sli.slider(Mathf.log2(minPotential), Mathf.log2(maxPotential), 0.01f, Mathf.log2(input), f -> configure(new Object[]{false, Mathf.pow(2, f)})).size(200, 50).padLeft(8).padRight(8);
            sli.add("").update(lable -> lable.setText(Mathf.round(input) + "NF"));
          });
          ta.row();
          ta.table(sli -> {
            sli.defaults().pad(0).margin(0);
            sli.table(Tex.buttonTrans, i -> i.image(Icon.upload).size(40)).size(50);
            sli.slider(Mathf.log2(minPotential), Mathf.log2(maxPotential), 0.01f, Mathf.log2(output), f -> configure(new Object[]{true, Mathf.pow(2, f)})).size(200, 50).padLeft(8).padRight(8);
            sli.add("").update(lable -> lable.setText(Mathf.round(output) + "NF"));
          });
        });

        show = () -> {
          t.visible = true;
          t.pack();
          t.setTransform(true);
          t.actions(
              Actions.scaleTo(0f, 1f),
              Actions.visible(true),
              Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out)
          );
        };

        close = () -> {
          t.actions(
              Actions.scaleTo(1f, 1f),
              Actions.scaleTo(0f, 1f, 0.07f, Interp.pow3Out),
              Actions.visible(false)
          );
        };
      }).fillY();
    }

    @Override
    public void updateTile() {
      super.updateTile();

      input = Mathf.clamp(input, minPotential, maxPotential);
      output = Mathf.clamp(output, minPotential, maxPotential);
    }

    @Override
    public float getInputPotential() {
      return Math.min(input, getEnergy());
    }

    @Override
    public float getOutputPotential() {
      return Math.min(output, getEnergy());
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.f(input);
      write.f(output);
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      input = read.f();
      output = read.f();
    }
  }
}
