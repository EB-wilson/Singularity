package singularity.core;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.OrderedSet;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.mod.Mods;
import singularity.Sgl;

public class ModsInteropAPI {
  private final ObjectMap<Mods.LoadedMod, Jval> declares = new ObjectMap<>();
  private final OrderedSet<ConfigModel> models = new OrderedSet<>();

  public void addModel(ConfigModel model, boolean init){
    if (models.add(model) && init){
      for (ObjectMap.Entry<Mods.LoadedMod, Jval> entry : declares) {
        try {
          if (entry.value.has("disable_api") && entry.value.get("disable_api").asBool()) {
            model.disable(entry.key);
          } else model.parse(entry.key, entry.value);
        } catch (Throwable e){
          Log.err("[Singularity API] some error happened in interop declaring file, mod: " + entry.key.name + ";" + entry.key.meta.version + ", details: ");
          Log.err(e);
        }
      }
    }
  }

  public void init(){
    if (Sgl.config.loadInfo) Log.info("[Singularity API] loading mod interop api");
    for (Mods.LoadedMod mod : Vars.mods.list()) {
      if (mod.name.equals(Sgl.modName)) continue;

      Fi api = mod.root.child("singularity_api.json");
      api = api.exists()? api: mod.root.child("singularity_api.hjson");

      if (!api.exists()){
        if (Sgl.config.loadInfo && Sgl.config.debugMode) Log.info("[Debug] no interop declared, skip: " + mod.name + ":" + mod.meta.version);

        continue;
      }

      if (Sgl.config.loadInfo) Log.info("[Singularity API] interoping mod: " + mod.name + ":" + mod.meta.version);

      try {
        declares.put(mod, Jval.read(api.reader()));
      }catch (Throwable e){
        Log.err("[Singularity API] reading interop declaring file error, mod: " + mod.name + ";" + mod.meta.version + ", details: ");
        Log.err(e);
      }
    }
  }

  public void updateModels(){
    for (ObjectMap.Entry<Mods.LoadedMod, Jval> entry : declares) {
      boolean dis = entry.value.has("disable_api") && entry.value.get("disable_api").asBool();
      for (ConfigModel model : models) {
        try {
          Jval cfg = entry.value.get(model.modelName);

          if (dis || cfg.isString() && cfg.asString().equals("disabled")){
            model.disable(entry.key);
          }
          else model.parse(entry.key, cfg);
        }catch (Throwable e){
          Log.err("[Singularity API] some error happened in interop declaring file, mod: " + entry.key.name + ";" + entry.key.meta.version + ", details: ");
          Log.err(e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends Content> T selectContent(ContentType type, String name, Mods.LoadedMod mod) {
    Content content = Vars.content.getByName(type, name);
    if (content == null) content = Vars.content.getByName(type, mod.name + "-" + name);

    if (content == null)
      throw new RuntimeException("no such " + type.name() + " named '" + name + "'");

    if (content.minfo.mod != mod){
      Log.warn("[Singularity API][Warn] mod " + mod.name + ": " + mod.meta.version + " operate other mod content");
    }

    return (T) content;
  }

  public static abstract class ConfigModel{
    public final String modelName;

    protected ConfigModel(String modelName) {
      this.modelName = modelName;
    }

    public abstract void parse(Mods.LoadedMod mod, Jval declaring);
    public abstract void disable(Mods.LoadedMod mod);
  }
}
