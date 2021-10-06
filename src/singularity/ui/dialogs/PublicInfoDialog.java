package singularity.ui.dialogs;

import arc.Core;
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

import java.util.regex.Pattern;

public class PublicInfoDialog extends BaseListDialog{
  private static final String langRegex = "#locale#";
  private static final Pattern imagePattern = Pattern.compile("<image *=.*>");
  
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
    String directory = Sgl.publicInfo + "directory.hjson";
    itemEntrySeq = new Seq<>();
  
    Http.get(directory, request -> {
      String direResult = request.getResultAsString();
      Log.info(direResult);
      
      Jval dire = Jval.read(direResult);
      for(Jval jval : dire.get("pages").asArray()){
        buildChild(jval);
      }
  
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
    infoContainer.defaults().grow().margin(margin).pad(pad);
    ObjectMap<String, TextureRegion> atlas = new ObjectMap<>();
    ObjectMap<String, float[]> atlasSize = new ObjectMap<>();
    String[] title = {""};
    
    Jval.JsonMap assets = sect.get("assets").asObject();
    Log.info(assets.toString());
    
    for(ObjectMap.Entry<String, Jval> asset : assets){
      if(!atlas.containsKey(asset.key)){
        Jval.JsonArray size = asset.value.get("size").asArray();
        atlasSize.put(asset.key, new float[]{size.get(0).asFloat(), size.get(1).asFloat()});
        
        if(asset.value.get("location").asString().equals("url")){
          atlas.put(asset.key, Core.atlas.find("nomap"));
          
          Runnable[] r = new Runnable[]{() -> {}};
          r[0] = () -> Http.get(asset.value.get("address").asString(), res -> {
            Pixmap pix = new Pixmap(res.getResult());
            Core.app.post(() -> {
              try{
                Texture tex = new Texture(pix);
                tex.setFilter(Texture.TextureFilter.linear);
                atlas.put(asset.key, new TextureRegion(tex));
                pix.dispose();
              }catch(Exception e){
                Log.err(e);
              }
            });
          }, e -> r[0].run());
          
          r[0].run();
        }
        else atlas.put(asset.key, Core.atlas.find(asset.value.get("address").asString()));
      }
    }
    
    Jval.JsonArray arr = sect.get("languages").asArray();
    ObjectSet<String> languages = new ObjectSet<>();
    
    for(Jval lang: arr){
      languages.add(lang.asString());
    }
    
    String currLang = Core.settings.getString("locale");
    
    String language = !currLang.equals("en") && languages.contains(currLang)? currLang: "";
    String url = sect.get("info").asString().replace(langRegex, language);
    
    Http.get(url).error(e -> {
      infoTable.clearChildren();
      infoTable.add(Core.bundle.get("warn.publicInfo.connectFailed"));
      infoTable.row();
      infoTable.button(Core.bundle.get("misc.refresh"), this::refresh).size(140, 60);
  
      Log.err(e);
    }).block(result -> {
      String[] strs = result.getResultAsString().split("\n");
      title[0] = strs[0];
      Log.info(title[0]);
      
      for(int i=1; i<strs.length; i++){
        if(imagePattern.matcher(strs[i]).matches()){
          String image = strs[i].replaceAll("<image *=", "").replace(">", "").replace(" ", "");
          TextureRegion region = atlas.get(image);
          float[] size = atlasSize.get(image);
          
          float iWidth = Math.min(width - margin*2 - pad - itemBoardWidth, size[0]);
          float scl = iWidth/size[0];
          
          infoContainer.image(region).size(size[0]*scl, size[1]*scl);
        }
        else{
          infoContainer.add(strs[i]).growX().fillY();
        }
        infoContainer.row();
      }
    });
    
    Log.info(3);
    
    itemEntrySeq.add(new ItemEntry(table -> {
      table.add(title[0]);
      Log.info(title[0]);
    }, table -> {
      Cell<Table> cell = table.add(infoContainer).width(width - itemBoardWidth - pad - margin*2).growY().padTop(pad + 40).padBottom(pad).color(infoContainer.color.cpy().a(0));
      UncCore.cellActions.add(new CellChangeColorAction(cell, table, infoContainer.color.cpy().a(1), 30));
      UncCore.cellActions.add(new CellAction(cell, table, 30){
        float p = pad + 40;
        float to = pad;
        float curr = pad + 40, currP = pad;

        @Override
        public void action(){
          curr = p + (to - p)*progress;
          currP = p + (to - p)*(1-progress);
          cell.padTop(curr);
          cell.padBottom(currP);
        }
      }.gradient(0.2f));
    }));
  }
}
