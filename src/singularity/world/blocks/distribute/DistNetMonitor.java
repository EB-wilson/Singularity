package singularity.world.blocks.distribute;

import arc.scene.ui.layout.Table;

public class DistNetMonitor extends DistNetBlock{
  public DistNetMonitor(String name){
    super(name);
    isNetLinker = false;
    configurable = true;
  }

  public class DistNetMonitorBuild extends DistNetBuild{
    @Override
    public void buildConfiguration(Table table){

    }
  }
}
