import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SyncBundles{
  public static void main(String[] args){
    Properties info = new Properties(new File("bundleInfo.properties"));
    String sourceLocale = info.get("source");
    String bundlesDir = info.get("bundlesDir").replace("/", File.separator);
    Properties loc = new Properties();
    loc.read(info.get("targetLocales").replace("[", "").replace("]", ""));
    String[] locales = new String[loc.map.size()*2];
    int index = 0;
    for(Pair entry: loc.map.values()){
      locales[index] = entry.key.replace("en_US", "");
      locales[index+1] = entry.value;
      index += 2;
    }

    File source = new File(bundlesDir, "bundle_" + sourceLocale + ".properties");
    Properties sourceBundle = new Properties();
    sourceBundle.read(source);

    for(int i = 0; i < locales.length; i += 2){
      String locale = locales[i], mark = locales[i + 1];
      File file = new File(bundlesDir, "bundle" + (locale.isBlank() ? "": "_" + locale) + ".properties");
      Properties bundle = new Properties(sourceBundle, mark);
      if(file.exists()) bundle.read(file);
      bundle.write(file);
    }
  }

  public static class Properties{
    ArrayList<Line> lines = new ArrayList<>();
    LinkedHashMap<String, Pair> map = new LinkedHashMap<>();

    public Properties(){}

    public Properties(File file){
      read(file);
    }

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

    public String get(String key){
      return map.containsKey(key)? map.get(key).value: null;
    }

    public void read(String content){
      read(new BufferedReader(new StringReader(content)));
    }

    public void read(File file){
      try{
        read(new BufferedReader(new FileReader(file)));
      }catch(FileNotFoundException e){
        e.printStackTrace();
      }
    }

    public void read(BufferedReader reader){
      try{
        String line;
        boolean init = lines.isEmpty();
        Pair last = null;

        while((line = reader.readLine()) != null){
          if(last != null){
            if(line.endsWith("\\")){
              last.value = last.value + line.substring(0, line.length() - 1) + System.lineSeparator();
              continue;
            }
            else last = null;
          }

          String[] strs = line.trim().split("=", 2);
          if(init){
            if(!strs[0].startsWith("#") && strs.length == 2){
              Pair p;
              lines.add(p = new Pair(line, strs[0].trim(), strs[1].trim()));
              map.put(p.key, p);
              if(strs[1].endsWith("\\")){
                last = p;
                p.value = p.value.substring(0, p.value.length()-1) + System.lineSeparator();
              }
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
