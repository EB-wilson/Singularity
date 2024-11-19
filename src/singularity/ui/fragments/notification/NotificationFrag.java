package singularity.ui.fragments.notification;

import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import singularity.graphic.SglDrawConst;

public class NotificationFrag {
  public final Seq<Notification> notifyQueue = new Seq<>();
  public final Seq<Notification> history = new Seq<>();

  Table notifies;
  Table window;

  public void build(Group parent) {
    parent.fill(Tex.buttonSideRight, main -> {
      main.left().table(tab -> {
        tab.button(Icon.rightOpen, () -> {
          //TODO
        }).fillX().growY();

        tab.table(notifies -> {
          this.notifies = notifies;
        }).size(300f, 500f);
      }).fill();
    });

    parent.fill(window -> this.window = window);
  }

  protected void buildNotification(Table table, Notification notification) {
    table.table(SglDrawConst.grayUIAlpha, pane -> {
      pane.image(notification.icon).size(32f).scaling(Scaling.fit).padRight(10f);
      pane.table(inf -> {
        inf.add(notification.title).growX().labelAlign(Align.left).fontScale(1.1f).color(Pal.accent);
        inf.row();
        inf.add(notification.information).growX().wrap().labelAlign(Align.left);
      });
    });
  }
}
