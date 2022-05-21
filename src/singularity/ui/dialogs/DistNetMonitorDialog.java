package singularity.ui.dialogs;

import arc.Core;
import mindustry.ui.dialogs.BaseDialog;
import singularity.ui.tables.DistNetMonitor;
import singularity.world.distribution.buffers.BaseBuffer;

public class DistNetMonitorDialog extends BaseDialog{
  DistNetMonitor monitor;

  public int maxCount = 20;

  public DistNetMonitorDialog(){
    super(Core.bundle.get("dialog.distNetMonitor.title"));

    addCloseButton();
  }

  public void build(){
    shown(() -> monitor.startStat());

    hidden(() -> monitor.endStat());

    cont.table(t -> t.add(monitor = new DistNetMonitor(maxCount)).grow()).grow().margin(140);
  }

  public void show(Iterable<BaseBuffer<?, ?, ?>> buffers){
    monitor.setBuffers(buffers);

    show();
  }
}
