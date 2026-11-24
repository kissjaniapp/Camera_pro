package hu.job.camerapro;
import android.content.Context;
import android.net.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
public final class Bw4Status {
 public static final class Result {
  public final String message, detail;
  Result(String m,String d){message=m;detail=d;}
 }
 private Bw4Status(){}
 private static String field(String body,String name){
  Pattern p=Pattern.compile("(?m)\\bvar\\s+"+Pattern.quote(name)+"\\s*=\\s*[\"']?([A-Za-z0-9._-]+)");
  Matcher m=p.matcher(body);return m.find()?m.group(1):"nem ismert";
 }
 public static Result read(Context context,String host,String user,String password){
  ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
  if(cm==null)return new Result("Nincs hálózatkezelő","A telefon hálózati szolgáltatása nem érhető el.");
  Network wifi=null;
  for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=n;break;}}
  if(wifi==null)return new Result("Nincs Wi-Fi kapcsolat","Csatlakozz a kamera saját Wi-Fi-hálózatához.");
  HttpURLConnection http=null;
  try{
   String query="?loginuse="+URLEncoder.encode(user,"UTF-8")+"&loginpas="+URLEncoder.encode(password,"UTF-8");
   URL url=new URL("http://"+host+":81/get_status.cgi"+query);
   http=(HttpURLConnection)wifi.openConnection(url);
   http.setConnectTimeout(4000);http.setReadTimeout(5000);http.setInstanceFollowRedirects(false);
   http.setRequestProperty("Connection","close");
   int code=http.getResponseCode();
   if(code!=200)return new Result("HTTP "+code,"A kamera státuszlekérdezésre nem adott 200 OK választ.");
   StringBuilder body=new StringBuilder();
   try(BufferedReader reader=new BufferedReader(new InputStreamReader(http.getInputStream(),StandardCharsets.UTF_8))){
    String line;while((line=reader.readLine())!=null&&body.length()<32768){body.append(line).append('\n');}
   }
   String s=body.toString();
   if(!s.contains("var result=")||!s.contains("sys_ver"))return new Result("Szokatlan kameraválasz","A kamera válaszolt, de nem ismerjük a státuszformátumot.");
   String detail="Kamera: "+host+"\nFirmware: "+field(s,"sys_ver")+"\nMCU: "+field(s,"scm_version")+"\nAkkumulátor: "+field(s,"batteryRate")+"%\nSD státusz: "+field(s,"sdstatus")+"\nFehér LED: "+field(s,"manual_light_status")+"\nP2P állapot: "+field(s,"p2pstatus")+"\nInternet jelző: "+field(s,"internet")+"\n\nA státuszválasz NEM jelenti azt, hogy az élőkép is működik.\nA hitelesítési adatok és az eszközazonosítók nem kerülnek naplóba.";
   return new Result("BW4 státuszlekérdezés sikeres",detail);
  }catch(Exception e){return new Result("BW4 státuszlekérdezés sikertelen","Hibatípus: "+e.getClass().getSimpleName()+". Ellenőrizd a helyi Wi-Fi-kapcsolatot és a kamera hozzáférését.");}
  finally{if(http!=null)http.disconnect();}
 }
}
