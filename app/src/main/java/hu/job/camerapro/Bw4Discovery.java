package hu.job.camerapro;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Read-only BW4 discovery diagnostic for a user-owned device.
 * VStarcam LAN_SEARCH wire bytes from public protocol research:
 * decoded F1 E0 00 00, XOR key 15 DB 43 22, wire F5 3B 43 22.
 * Does NOT echo discovery reply or start an authenticated session.
 */
public final class Bw4Discovery {
 private static final byte[] SEARCH={(byte)0xF5,(byte)0x3B,0x43,0x22};
 private static final int PORT=32108;
 public static final class Result {
  public final String message,log;
  Result(String message,String log){this.message=message;this.log=log;}
 }
 private Bw4Discovery(){}
 public static Result probe(Context context,String target){
  ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
  if(cm==null)return new Result("Nincs hálózatkezelő","A hálózati szolgáltatás nem elérhető.");
  Network wifi=null;
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities c=cm.getNetworkCapabilities(n);
   if(c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=n;break;}
  }
  if(wifi==null)return new Result("Nincs Wi-Fi","Csatlakozz a BW4 saját Wi-Fi-jéhez vagy az otthoni hálózathoz.");
  StringBuilder log=new StringBuilder("VStarcam LAN_SEARCH – kizárólag helyi teszt\\n");
  log.append("Cél: ").append(target).append(":").append(PORT).append("\\n");
  int found=0, unknown=0;
  try(DatagramSocket sock=new DatagramSocket(null)){
   sock.setReuseAddress(true);
   sock.bind(new InetSocketAddress(0));
   wifi.bindSocket(sock);
   sock.setSoTimeout(850);
   InetAddress ip=InetAddress.getByName(target);
   DatagramPacket ask=new DatagramPacket(SEARCH,SEARCH.length,ip,PORT);
   byte[] buffer=new byte[1400];
   for(int attempt=0;attempt<3;attempt++){
    sock.send(ask);
    long stop=android.os.SystemClock.elapsedRealtime()+900;
    while(android.os.SystemClock.elapsedRealtime()<stop){
     try{
      DatagramPacket answer=new DatagramPacket(buffer,buffer.length);
      sock.receive(answer);
      if(answer.getLength()<4){unknown++;continue;}
      int magic=(buffer[0]&255)^0x15;
      int type=(buffer[1]&255)^0xdb;
      if(magic==0xf1&&type==0x91){
       found++;
       log.append("LAN_NOTIFY válasz érkezett: ").append(answer.getAddress().getHostAddress()).append(":").append(answer.getPort()).append("\\n");
      }else{
       unknown++;
       log.append("Más UDP-válasz, ").append(answer.getLength()).append(" bájt; típusa nem azonosított.\\n");
      }
     }catch(SocketTimeoutException e){break;}
    }
   }
   log.append("Felismert LAN_NOTIFY: ").append(found).append("; más válasz: ").append(unknown).append("\\n");
   log.append("A videó és a munkamenet még nincs megvalósítva. A kamera UID-jét/jelszavát nem gyűjtjük.\\n");
   if(found>0)return new Result("BW4 helyi felderítés működik ("+found+" válasz)",log.toString());
   return new Result("Nincs UDP-felderítési válasz – TCP 81 még külön tesztelhető",log.toString());
  }catch(Exception e){
   log.append("Hálózati hiba: ").append(e.getClass().getSimpleName());
   return new Result("UDP-kamerakeresés sikertelen",log.toString());
  }
 }
}
