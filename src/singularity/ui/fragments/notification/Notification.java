package singularity.ui.fragments.notification;

import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;

public abstract class Notification {
  public String title;
  public TextureRegion icon;
  public String information;
  public boolean activeWindow;

  public boolean notified;
  public boolean read;

  public abstract void buildWindow(Table table);
}
