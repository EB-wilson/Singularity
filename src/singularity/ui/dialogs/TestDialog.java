package singularity.ui.dialogs;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g3d.Camera3D;
import arc.input.KeyCode;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec2;
import arc.math.geom.Vec3;
import arc.scene.Element;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Dialog;
import arc.util.Tmp;
import mindustry.input.Binding;
import singularity.Singularity;
import singularity.graphic.graphic3d.Draw3D;
import singularity.graphic.graphic3d.SortedBatch3D;
import singularity.graphic.graphic3d.StdShadowBatch3D;
import singularity.ui.SglStyles;

public class TestDialog extends Dialog {
  public TestDialog() {
    super("", SglStyles.transGrayBack);
    titleTable.clear();

    //SortedBatch3D batch3D = new SortedBatch3D(4000);
    StdShadowBatch3D batch3D = new StdShadowBatch3D(4000, 3, Gl.triangles, 1024);
    Camera3D camera = batch3D.getCamera();
    camera.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
    camera.up.set(0, 1, 0);
    Vec3 right = new Vec3(1, 0, 0);

    Vec3 camPos = new Vec3(4, 0, 10);
    Vec3 vec = new Vec3();
    update(() -> {
      Tmp.v31.set(
          Core.input.axis(Binding.move_x),
          0,
          Core.input.axis(Binding.move_y)
      );
      Mat3D.rot(Tmp.v31, camera.combined);
      Tmp.v31.y = 0;
      Tmp.v31.nor().scl(0.1f);
      if (Core.input.keyDown(KeyCode.space)) Tmp.v31.add(0, 0.1f, 0);
      if (Core.input.keyDown(KeyCode.shiftLeft)) Tmp.v31.add(0, -0.1f, 0);
      camPos.add(Tmp.v31);
    });

    cont.table().grow();

    addCaptureListener(new InputListener(){
      float lx, ly;

      @Override
      public boolean mouseMoved(InputEvent event, float x, float y) {
        if (lx != x){
          camera.direction.rotate(camera.up, (x - lx) * 0.1f);
          right.rotate(camera.up, (x - lx) * 0.1f);
          lx = x;
        }
        if (ly != y){
          camera.direction.rotate(right, - (y - ly) * 0.1f);
          ly = y;
        }

        return false;
      }
    });

    Vec3 axis = new Vec3(1, 0, 0).nor();
    fill((x, y, w, h) -> {
      camera.position.set(camPos);
      camera.up.set(0, 1, 0);
      camera.update();

      Draw3D.begin(batch3D, true);
      Draw3D.camera(camera);
      Draw3D.resetLights();
      Draw3D.nextLight(-0, 20, -0, Color.white);

      //Draw3D.dirLightColor(Color.red, 0.2f);
      //Draw3D.lightDir(-1, -1, -1);

      Draw3D.setAmbientColor(1f, 1f, 1f, 0.1f);

      Draw3D.alpha(false);
      Draw3D.resetPreTransform();
      for (int dy = 0; dy < 3; dy++) {
        for (int dx = 0; dx < 10; dx++) {
          for (int dz = 0; dz < 10; dz++) {
            Draw3D.cube(
                dx*2 - 10, dy*4, dz*2 - 10, 1, Color.gray
            );
          }
        }
      }

      Draw3D.rect(
          -100, -5, -100,
          -100, -5, 100,
          100, -5, 100,
          100, -5, -100,
          Color.white
      );

      Draw3D.end();
    });

    fill(t -> {
      t.top().slider(0, 10, 0.0001f, batch3D.shadowBias, f -> {
        batch3D.shadowBias = f;
      }).width(200f);
      t.add("").update(l -> l.setText("Shadow bias: " + batch3D.shadowBias)).width(100f);
    });
  }
}
