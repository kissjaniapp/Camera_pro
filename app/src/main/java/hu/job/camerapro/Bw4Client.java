package hu.job.camerapro;
import android.content.Context;
import android.net.*;
import java.net.*;
import java.io.*;

/**
 * Non-invasive camera/network diagnosis. No CGI request is sent because stock
 * BW4 port 81 does not respond to ordinary HTTP on current firmware.
 */
public final class Bw4Client {
 public static final class Result {
  public final String message,log;
  Result(String m,String l){message=m;log=l;}
 }
 public static Result probe(Context context,String host){
  StringBuilder log=new StringBuilder("BW4 helyi TCP 81 diagnosztika\n");
  ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
  if(cm==null)return new Result("Nincs hálózatkezelő","Nincs ConnectivityManager.");
  Network wifi=null; boolean vpn=false;
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities cap=cm.getNetworkCapabilities(n);
   if(cap==null)continue;
   if(cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN))vpn=true;
   if(cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))wifi=n;
  }
  if(vpn)log.append("Figyelem: VPN aktív. A kamerateszthez ideiglenesen kapcsold ki.\n");
  if(wifi==null)return new Result("Nincs Wi-Fi kapcsolat",log+"Csatlakozz a kamera saját Wi-Fi-jéhez.");
  LinkProperties props=cm.getLinkProperties(wifi);
  if(props!=null){
   log.append("Wi-Fi hálózat interfésze: ").append(props.getInterfaceName()).append("\n");
   for(LinkAddress ip:props.getLinkAddresses()){
    if(ip.getAddress() instanceof Inet4Address)log.append("Telefon helyi IP-je: ").append(ip.getAddress().getHostAddress()).append("/").append(ip.getPrefixLength()).append("\n");
   }
  }
  log.append("Cél: ").append(host).append(":81\n");
  try(Socket socket=wifi.getSocketFactory().createSocket()){
   socket.connect(new InetSocketAddress(host,81),3500);
   log.append("TCP 81 kapcsolat sikeres.\n");
   log.append("A kapcsolat létrejött; ez önmagában nem élőkép vagy bejelentkezés.\n");
   log.append("A gyári BW4 a :81 porton saját protokollt használ. Nem küldünk sima HTTP GET kérést.\n");
   log.append("Következő lépés: az O-KAM Pro AP-módú forgalmának ellenőrzése.\n");
   return new Result("TCP 81 elérhető – helyi videóprotokoll szükséges",log.toString());
  }catch(IOException e){
   log.append("TCP hiba: ").append(e.getClass().getSimpleName()).append("\n");
   return new Result("A BW4 81-es portja most nem érhető el",log.toString());
  }
 }
}
