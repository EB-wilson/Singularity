package singularity.ui.tables;

import arc.scene.ui.layout.Table;
import singularity.world.distribution.DistributeNetwork;

public abstract class Monitor extends Table{
  public abstract void startMonit(DistributeNetwork distNetwork);

  public abstract void endMonit(DistributeNetwork distNetwork);
}
