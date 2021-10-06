package singularity.ui.dialogs;

import arc.Core;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.serialization.Jval;
import singularity.Sgl;
import universeCore.UncCore;
import universeCore.util.animLayout.CellAction;
import universeCore.util.animLayout.CellChangeColorAction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class PublicInfoDialog extends BaseListDialog{
  private static final String langRegex = "#locale#";
  private static final Pattern imagePattern = Pattern.compile("<image *=.*>");
  
  Cons<Integer> buildInfo;
  Seq<ItemEntry> itemEntrySeq;
  boolean connected = false;
  float timer = 0;
  
  public PublicInfoDialog(){
    super(Core.bundle.get("misc.publicInfo"));
    
    Prov<String> loadAnim = () -> {
      float time = Time.time % 120;
      
      return (time < 40? ".": time < 80? "..": "...") + "(" + Strings.autoFixed(timer*15, 0) + ")";
    };
    
    defaultInfo = info -> {
      info.add("").update(t -> {
        t.setText(Core.bundle.get("misc.loading") + loadAnim.get());
        
        if(!connected){
          timer += (1f/900)*Time.delta;
    
          if(timer >= 1){
            infoTable.clearChildren();
            infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
            infoTable.row();
            infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
      
            Log.err("connect failed, time out");
          }
        }
      });
    };
    
    shown(this::refresh);
  }
  
  public void refresh(){
    Log.info("loading message");
    
    connected = false;
    timer = 0;
    String directory = Sgl.publicInfo + "directory.ini";
    itemEntrySeq = new Seq<>();
  
    Http.get(directory, request -> {
      String direResult = request.getResultAsString();
      Log.info(direResult);
      
      Jval dire = Jval.read(direResult);
      dire.get("pages").asArray().forEach(this::buildChild);
      
      items = itemEntrySeq;
      rebuild();
      
      connected = true;
    }, e -> {
      connected = false;
      
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
    
    Table infoContainer = new Table();
    ObjectMap<String, TextureRegion> atlas = new ObjectMap<>();
    String[] title = {""};
    
    Jval.JsonMap assets = sect.get("assets").asObject();
    Log.info(assets.toString());
    assets.forEach(image -> {
      if(!atlas.containsKey(image.key)){
        Jval.JsonArray size = image.value.get("size").asArray();
        if(image.value.get("location").asString().equals("url")){
          
          atlas.put(image.key, new TextureRegion(Core.atlas.find("nomap").texture, size.get(0).asInt(), size.get(1).asInt()));
          Http.get(image.value.get("address").asString(), res -> {
            Pixmap pix = new Pixmap(res.getResult());
            Core.app.post(() -> {
              try{
                Texture tex = new Texture(pix);
                tex.setFilter(Texture.TextureFilter.linear);
                atlas.put(image.key, new TextureRegion(tex));
                pix.dispose();
              }catch(Exception e){
                Log.err(e);
              }
            });
          }, Log::err);
        }
        else atlas.put(image.key,  new TextureRegion(Core.atlas.find(image.value.get("address").asString()).texture, size.get(0).asInt(), size.get(1).asInt()));
      }
    });
    Log.info(2);
    Jval.JsonArray arr = sect.get("languages").asArray();
    ObjectSet<String> languages = new ObjectSet<>();
    
    for(Jval lang: arr){
      languages.add(lang.asString());
    }
    
    String currLang = Core.settings.getString("locale");
    
    String language = !currLang.equals("en") && languages.contains(currLang)? currLang: "";
    String url = sect.get("info").asString().replace(langRegex, language);
    
    Http.get(url, result -> {
      String[] strs = result.getResultAsString().split("\n");
      title[0] = strs[0];
      
      for(int i=1; i<strs.length; i++){
        if(imagePattern.matcher(strs[i]).matches()){
          String image = strs[i].replaceAll("<image *=", "").replace(">", "").replace(" ", "");
          TextureRegion region = atlas.get(image);
          
          float iWidth = Math.min(width - margin - itemBoardWidth, region.width);
          float scl = iWidth/region.width;
          
          infoContainer.image().size(region.width*scl, region.width*scl);
        }
        else{
          infoContainer.add(strs[i]).growX().fillY();
        }
      }
    }, e -> {
      infoTable.clearChildren();
      infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
      infoTable.row();
      infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
      
      Log.err(e);
    });
    Log.info(3);
    
    itemEntrySeq.add(new ItemEntry(table -> table.add(title[0]), table -> {
      AtomicInteger i = new AtomicInteger();
      
      Cell<Table> cell = table.add(infoContainer).width(width - itemBoardWidth - pad - margin*2).growY().padTop(pad + 40).color(infoContainer.color.cpy().a(0));
      UncCore.cellActions.add(new CellChangeColorAction(cell, table, infoContainer.color.cpy().a(1), 30));
      UncCore.cellActions.add(new CellAction(cell, table, 30){
        float p = pad + 40;
        float to = pad;
        float curr = pad + 40;

        @Override
        public void action(){
          curr = p + (to - p)*progress;
          cell.padTop(curr);
        }
      }.gradient(0.2f));
      
      buildInfo.get(i.getAndIncrement());
    }));
  }
}
