package singularity.graphic.graphic3d;

import arc.graphics.g3d.Camera3D;
import arc.struct.OrderedSet;
import arc.struct.Seq;
import mindustry.gen.Groups;

import java.util.Comparator;

public class Stage3D {
  private static final OrderedSet<Material> tmp = new OrderedSet<>();

  public final OrderedSet<RendererObject> objects = new OrderedSet<>();
  public final OrderedSet<RendererObject> sortedObjects = new OrderedSet<>();
  public final Camera3D camera3D = new Camera3D();

  public Comparator<RendererObject> comparator = (a, b) -> Float.compare(
      camera3D.position.dst2(a.getX(), a.getY(), a.getZ()),
      camera3D.position.dst2(b.getX(), b.getY(), b.getZ())
  );

  public void renderer(){
    OrderedSet<Material> set = tmp;
    set.clear();
    for (RendererObject object : objects) {
      object.parTransformed(false);
      set.add(object.material());
    }
    set.orderedItems().sort();

    for (Material material : set) {
      material.reset();
    }

    for (RendererObject object : objects) { object.renderer(); }
    for (RendererObject object : sortedObjects.orderedItems().sort(comparator)) { object.renderer(); }

    for (Material material : set) {
      material.passing();
    }
  }

  public void add(RendererObject object){
    (object.isSort()? sortedObjects: objects).add(object);
  }

  public void remove(RendererObject object){
    (object.isSort()? sortedObjects: objects).remove(object);
  }
}
