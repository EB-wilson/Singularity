package singularity.graphic.graphic3d;

import arc.graphics.Gl;
import arc.graphics.Mesh;
import arc.math.geom.Mat3D;
import arc.struct.Seq;
import singularity.world.Transform;
import universecore.annotations.Annotations;

public interface RendererObject extends Transform {
  Seq<RendererObject> tmpStack = new Seq<>();

  boolean isSort();
  Material material();
  Mesh mesh();
  RendererObject parent();

  @Annotations.BindField(value = "tempTrans", initialize = "new Mat3D()")
  default Mat3D parentTrans(){ return null; }
  @Annotations.BindField("parTransformed")
  default boolean parTransformed(){ return false; }
  @Annotations.BindField("parTransformed")
  default void parTransformed(boolean transformed){}

  default void renderer() {
    Seq<RendererObject> stack = tmpStack.clear();

    if (!parTransformed()) {
      RendererObject curr = this;
      while (curr != null) {
        stack.add(curr);

        curr = curr.parent();
      }

      for (int i = stack.size - 1; i >= 0; i--) {
        RendererObject obj = stack.get(i);
        if (obj.parent() == null) obj.parentTrans().idt();
        else {
          obj.parent().getTransform(obj.parentTrans()).mulLeft(obj.parent().parentTrans());
        }
        obj.parTransformed(true);
      }
    }

    material().begin();
    if (parent() != null) material().drawObject(this, parentTrans());
    else material().drawObject(this);
    material().end();
  }

  default int meshOffset() {
    return 0;
  }
  default int meshCount() {
    Mesh mesh = mesh();
    return mesh.indices.max() > 0 ? mesh.getNumIndices() : mesh.getNumVertices();
  }
  default int verticesPrimitive(){
    return Gl.triangles;
  }
}
