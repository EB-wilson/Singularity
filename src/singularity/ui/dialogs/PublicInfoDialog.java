package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Http;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.serialization.Jval;
import singularity.Sgl;
import universeCore.UncCore;
import universeCore.util.animLayout.CellAction;
import universeCore.util.animLayout.CellChangeColorAction;
import universeCore.util.handler.FieldHandler;

import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class PublicInfoDialog extends BaseListDialog{
  private static final String titlesUrl = "https://raw.githubusercontent.com/EB-wilson/Singularity/master/publicInfo/titles.hjson";
  private static final String langRegex = "#locale#";
  private static final Pattern imagePattern = Pattern.compile("<image *=.*>");
  
  volatile boolean initialized, titleLoaded;
  Throwable titleStatus;
  Jval titles;
  
  ObjectMap<String, Table> pages = new ObjectMap<>();
  
  Runnable loadPage;
  LoadTable itemsLoading;
  
  public PublicInfoDialog(){
    super(Core.bundle.get("misc.publicInfo"));
    
    defaultInfo = table -> {
      table.clearChildren();
      if(!initialized){
        itemsLoading = table.add(new LoadTable(this::refresh)).get();
      }
      else{
        table.add(Core.bundle.get("dialog.PublicInfo.empty"));
      }
    };
  
    build();
    
    shown(this::refresh);
  }
  
  @SuppressWarnings("StatementWithEmptyBody")
  public void refresh(){
    Log.info("loading message");
  
    ExecutorService exec = FieldHandler.getValue(Http.class, "exec", null);
    exec.shutdown();
    Http.setMaxConcurrent(8);
    
    titleLoaded = false;
    Http.get(titlesUrl, req -> {
      titleLoaded = true;
      titleStatus = null;
      titles = Jval.read(req.getResultAsString());
    }, e -> {
      titleLoaded = true;
      titleStatus = e;
  
      Log.err(e);
    });
    
    String directory = Sgl.publicInfo + "directory.hjson";
    items.clear();
    pages.clear();
    
    initialized = false;
  
    Http.get(directory, request -> {
      while(!titleLoaded){
      }
      if(titleStatus != null){
        infoTable.clearChildren();
        infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
        infoTable.row();
        infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
        
        Log.err(titleStatus);
        return;
      }
      
      itemsLoading.finish();
      initialized = true;
      
      String direResult = request.getResultAsString();
      Log.info(direResult);
      
      Jval dire = Jval.read(direResult);
      for(Jval jval : dire.get("pages").asArray()){
        buildChild(jval);
      }
  
      rebuild();
    }, e -> {
      infoTable.clearChildren();
      infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
      infoTable.row();
      infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
      
      Log.err(directory);
      Log.err(e);
    });
    
    rebuild();
  }
  
  void buildChild(Jval sect){
    Log.info(name + ", " + sect.toString());
    
    ObjectMap<String, TextureRegion> atlas = new ObjectMap<>();
    ObjectMap<String, float[]> atlasSize = new ObjectMap<>();
    
    if(sect.has("assets")){
      Jval.JsonMap assets = sect.get("assets").asObject();
      Log.info(assets.toString());
  
      for(ObjectMap.Entry<String, Jval> asset : assets){
        if(! atlas.containsKey(asset.key)){
          Jval.JsonArray size = asset.value.get("size").asArray();
          atlasSize.put(asset.key, new float[]{size.get(0).asFloat(), size.get(1).asFloat()});
      
          if(asset.value.get("location").asString().equals("url")){
            TextureRegion region;
            atlas.put(asset.key, region = Core.atlas.find("nomap"));
        
            Runnable[] r = new Runnable[]{() -> {
            }};
            r[0] = () -> Http.get(asset.value.get("address").asString(), res -> {
              Pixmap pix = new Pixmap(res.getResult());
              Core.app.post(() -> {
                try{
                  Texture tex = new Texture(pix);
                  tex.setFilter(Texture.TextureFilter.linear);
                  region.set(tex);
                  pix.dispose();
                }catch(Exception e){
                  Log.err(e);
                }
              });
            }, e -> r[0].run());
        
            r[0].run();
          }else atlas.put(asset.key, Core.atlas.find(asset.value.get("address").asString()));
        }
      }
    }
    
    Jval.JsonArray arr = sect.get("languages").asArray();
    ObjectSet<String> languages = new ObjectSet<>();
    
    for(Jval lang: arr){
      languages.add(lang.asString());
    }
    
    String currLang = Core.settings.getString("locale");
    
    String url = sect.get("info").asString().replace(langRegex, currLang);
    
    String title = titles.get(sect.get("title").asString()).get(currLang).asString();
    
    loadPage = () -> {
      infoTable.clear();
      
      if(pages.containsKey(url)){
        Cell<Table> cell = infoTable.add(pages.get(url)).left().grow().padLeft(pad).padTop(pad + 60).padBottom(pad);
        cell.color(cell.get().color.cpy().a(0));
        
        UncCore.cellActions.clear();
        UncCore.cellActions.add(new CellChangeColorAction(cell, infoTable, cell.get().color.cpy().a(1), 30));
        UncCore.cellActions.add(new CellAction(cell, infoTable, 30){
          final float p = pad + 60;
          final float to = pad;
          float curr = pad + 60;
          float currP = pad;
    
          @Override
          public void action(){
            curr = p + (to - p)*progress;
            currP = p + (to - p)*(1 - progress);
            cell.padTop(curr);
            cell.padBottom(currP);
          }
        }.gradient(0.2f));
      }
      else{
        LoadTable loading = infoTable.add(new LoadTable(loadPage)).get();
        
        Http.get(url, result -> {
          loading.finish();
          
          Table infoContainer = new Table();
          infoContainer.defaults().grow().margin(margin).padTop(pad);
          String[] strs = result.getResultAsString().split("\n");
  
          for(String str : strs){
            if(imagePattern.matcher(str).matches()){
              String image = str.replaceAll("<image *=", "").replace(">", "").replace(" ", "");
              TextureRegion region = atlas.get(image);
              float[] size = atlasSize.get(image);
      
              float iWidth = Math.min(width - margin*2 - pad*2 - itemBoardWidth - 55, size[0]);
              float scl = iWidth/size[0];
      
              infoContainer.image(region).size(size[0]*scl, size[1]*scl);
            }else{
              infoContainer.add(str).fillY().left().padLeft(4).width(width - itemBoardWidth - margin*2 - pad*2 - 55).get().setWrap(true);
            }
            infoContainer.row();
          }
    
          pages.put(url, infoContainer);
          loadPage.run();
        }, e -> {
          infoTable.clearChildren();
          infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
          infoTable.row();
          infoTable.button(Core.bundle.get("misc.refresh"), loadPage).size(140, 60);
    
          Log.err(e);
        });
      }
    };
    
    items.add(new ItemEntry(table -> {
      table.add(title);
    }, table -> {
      loadPage.run();
    }));
  }
  
  public static class LoadTable extends Table{
    float waitTime;
    
    float time;
    boolean timeout, lastTest = false, connectSucceed;
  
    public Cons<Label> buildLoading = t -> t.setText(Core.bundle.get("misc.loading") + Strings.autoFixed(time/60, 0) + getLoading(3, 30));
    Runnable refresh;
  
    Cell<Table> cell;
    
    public static String getLoading(int length, float interval){
      float time = Time.time % interval;
      int amount = (int)Math.ceil((time/interval)*length);
      
      String result = ".".repeat(amount);
      return result + " ".repeat(length - amount);
    }
    
    public LoadTable(Runnable refresh){
      this(9000, refresh);
    }
    
    public LoadTable(float wait, Runnable refresh){
      this.refresh = refresh;
      waitTime = wait;
  
      add("").update(buildLoading);
      row();
      cell = table(t -> {
        t.add(Core.bundle.get("dialog.publicInfo.loadTimeout"));
        t.button(Core.bundle.get("misc.refresh"), () -> {
          lastTest = timeout = connectSucceed = false;
          time = 0;
          UncCore.cellActions.add(new CellChangeColorAction(cell, this, t.color.cpy().a(0), 60));
  
          ExecutorService exec = FieldHandler.getValue(Http.class, "exec", null);
          exec.shutdown();
          Http.setMaxConcurrent(8);
          
          refresh.run();
        }).get().touchable(() -> timeout? Touchable.enabled: Touchable.disabled);
      });
      cell.color(cell.get().color.cpy().a(0));
      
      cell.update(t -> {
        if(!connectSucceed) time += Time.delta;
        if(time > waitTime) timeout = true;
        if(!lastTest && timeout){
          lastTest = true;
  
          UncCore.cellActions.add(new CellChangeColorAction(cell, this, t.color.cpy().a(1), 60));
        }
      });
    }
    
    public void finish(){
      connectSucceed = true;
    }
  }
}
