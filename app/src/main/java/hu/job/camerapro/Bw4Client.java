package hu.job.camerapro;
import android.content.Context;
import android.net.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class Bw4Client {
 public static final class Result { public final String message,log; Result(String m,String l){message=m;log=l;} }
 public static Result probe(Context context,String host){
 StringBuilder log=new StringBuilder();
 ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
 Network wifi=null;
 for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=n;break;}}
 if(wifi==null)return new Result("Nincs Wi-Fi kapcsolat","Válaszd ki a kamera saját Wi-Fi hálózatát. Mobilinternetre nem küldünk kamerakérést.");
 log.append("Wi-Fi hálózat kiválasztva, cél: ").append(host).append(":81\n");
 try(Socket s=wifi.getSocketFactory().createSocket()){
 s.connect(new InetSocketAddress(host,81),3000);log.append("TCP kapcsolat sikeres.\n");s.setSoTimeout(3500);
 // Read-only documented status endpoint, ordinary HTTP is experimental.
 String request="GET /get_status.cgi? HTTP/1.1\r\nHost: "+host+":81\r\nConnection: close\r\n\r\n";
 s.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));s.getOutputStream().flush();log.append("Állapotlekérdezés elküldve (kísérleti HTTP).\n");
 byte[] b=new byte[256];try{int n=s.getInputStream().read(b);if(n>0){log.append("Válasz: ").append(n).append(" bájt. Tartalma adatvédelmi okból nincs naplózva.\n");return new Result("TCP elérhető – válasz érkezett, protokoll ellenőrizendő",log.toString());}log.append("A kamera lezárta a kapcsolatot.\n");}catch(IOException e){log.append("Nincs értelmezhető válasz: ").append(e.getClass().getSimpleName()).append("\n");}
 return new Result("TCP elérhető – gyári protokoll még nem működik",log.toString());
 }catch(IOException e){log.append("Kapcsolati hiba: ").append(e.getClass().getSimpleName());return new Result("A kamera nem érhető el",log.toString());}
 }
}