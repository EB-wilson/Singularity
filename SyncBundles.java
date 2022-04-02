import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SyncBundles{
  private static final String sourceLocale = "zh_CN";
  private static final String[] locales = {
      "zh_TW", "(待翻譯)",
      "", "(waitToTranslate)"
  };

  private static final String bundlesDir = File.separator + "assets" + File.separator + "bundles" + File.separator;

  public static void main(String[] args){
    String folder = args[0] + bundlesDir;
    File source = new File(folder, "bundle_" + sourceLocale + ".properties");
    Properties sourceBundle = new Properties();
    sourceBundle.read(source);

    for(int i = 0; i < locales.length; i += 2){
      String locale = locales[i], mark = locales[i + 1];
      File file = new File(folder, "bundle" + (locale.isBlank() ? "": "_" + locale) + ".properties");
      Properties bundle = new Properties(sourceBundle, mark);
      if(file.exists()) bundle.read(file);
      bundle.write(file);
    }
  }

  public static class Properties{
    ArrayList<Line> lines = new ArrayList<>();
    HashMap<String, Pair> map = new HashMap<>();

    public Properties(){}

    public Properties(Properties source, String mark){
      for(Line line: source.lines){
        Line l;
        if(line instanceof Pair){
          l = new Pair(line.string, ((Pair) line).key, mark + " " + ((Pair) line).value);
          map.put(((Pair) l).key, (Pair) l);
        }
        else l = new Line(line.string);
        lines.add(l);
      }
    }

    public void read(File file){
      try{
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        boolean init = lines.isEmpty();
        while((line = reader.readLine()) != null){
          String[] strs = line.trim().split("=", 2);

          if(init){
            if(!strs[0].startsWith("#") && strs.length == 2){
              Pair p;
              lines.add(p = new Pair(line, strs[0].trim(), strs[1].trim()));
              map.put(p.key, p);
            }
            else{
              lines.add(new Line(line));
            }
          }
          else {
            Pair pair = map.get(strs[0].trim());
            if(pair != null){
              pair.value = strs[1].trim();
            }
          }
        }
      }catch(IOException e){
        throw new RuntimeException(e);
      }
    }

    public void write(File file){
      try{
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
        for(Line line: lines){
          writer.write(line.toString());
          writer.newLine();
          writer.flush();
        }
      }catch(IOException e){
        throw new RuntimeException(e);
      }
    }
  }

  public static class Line{
    String string;

    public Line(String line){
      this.string = line;
    }

    @Override
    public String toString(){
      return string;
    }
  }

  public static class Pair extends Line{
    String key;
    String value;

    public Pair(String line, String key, String value){
      super(line);
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString(){
      return key + " = " + value;
    }
  }
}
