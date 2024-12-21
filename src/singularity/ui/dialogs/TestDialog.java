package singularity.ui.dialogs;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Gl;
import arc.graphics.Mesh;
import arc.graphics.Pixmap;
import arc.graphics.g3d.Camera3D;
import arc.graphics.gl.FrameBuffer;
import arc.input.KeyCode;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.util.Tmp;
import mindustry.content.Planets;
import mindustry.input.Binding;
import singularity.graphic.graphic3d.Draw3D;
import singularity.graphic.graphic3d.StandardBatch3D;
import singularity.graphic.graphic3d.StdShadowBatch3D;
import singularity.ui.SglStyles;
import universecore.util.handler.FieldHandler;

public class TestDialog extends Dialog {
  public TestDialog() {
    super("", SglStyles.transGrayBack);
    titleTable.clear();
    addCloseButton();

    Mesh grid = FieldHandler.getValueDefault(Planets.serpulo.mesh, "mesh");
    Mesh planet = new Mesh(true,
        grid.getMaxVertices(), 0,
        StandardBatch3D.buildVertexAttributes()
    );
    Draw3D.meshToStandardVertices(
        grid,
        planet,
        new int[]{0, 5, 4}// p, n, c
    );

    //StandardBatch3D batch3D = new StandardBatch3D(4000);
    StdShadowBatch3D batch3D = new StdShadowBatch3D(4000, 1, Gl.triangles, 1024);
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

    FrameBuffer targetBuffer = new FrameBuffer(Pixmap.Format.rgba8888, Core.graphics.getWidth(), Core.graphics.getHeight(), true);
    batch3D.setTarget(targetBuffer);

    Vec3 axis = new Vec3(1, 0, 0).nor();
    cont.fill((x, y, w, h) -> {
      camera.position.set(camPos);
      camera.up.set(0, 1, 0);
      camera.update();

      Draw3D.begin(batch3D, true);
      Draw3D.camera(camera);
      Draw3D.resetLights();
      Draw3D.nextLight(-5, 20, -0, Color.white);
      //Draw3D.nextLight(-20, 6, -0, Color.white).color.a(0.6f);
      //Draw3D.nextLight(0, 4, -0, Color.white).color.a(0.6f);

      Draw3D.dirLightColor(Color.white, 0.2f);
      Draw3D.lightDir(-1, -1, -1);

      Draw3D.setAmbientColor(1f, 1f, 1f, 0.1f);

      Draw3D.resetPreTransform();
      Draw3D.alpha(true);
      batch3D.draw(
          Core.atlas.white().texture, null, null, planet
      );
      Draw3D.alpha(false);
      Draw3D.rect(
          -50, -10, -50,
          -50, -10, 50,
          50, -10, 50,
          50, -10, -50,
          Color.white
      );

      for (int i = 0; i < 10; i++) {
        Draw3D.cube(
            5, i*4, 5, 2, Color.white
        );
      }

      Draw3D.end();
    }).touchable = Touchable.disabled;

    //fill(t -> {
    //  t.row();
    //  t.add("").update(l -> l.setText(Strings.fixed(60/Time.delta, 2) + "fps"));
    //  t.row();
    //  t.top().slider(0, 10, 0.0001f, batch3D.shadowBias, f -> {
    //    batch3D.shadowBias = f;
    //  }).width(200f);
    //  t.add("").update(l -> l.setText("Shadow bias: " + batch3D.shadowBias)).width(100f);
    //});
  }
}
