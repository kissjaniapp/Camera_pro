package hu.job.camerapro;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import java.net.*;
import java.util.*;

/** Diagnostic only. No password, cloud authentication, pairing, or stream is attempted. */
public final class Bw4Discovery {
 private static final byte[] SEARCH={(byte)0xF5,(byte)0x3B,0x43,0x22};
 private static final int PORT=32108;
 public static final class Result {
  public final String message,log;
  Result(String m,String l){message=m;log=l;}
 }
 private Bw4Discovery(){}
 private static String broadcastFor(LinkAddress link){
  InetAddress addr=link.getAddress();
  if(!(addr instanceof Inet4Address))return null;
  byte[] octets=addr.getAddress();
  int bits=link.getPrefixLength();
  if(bits<1||bits>30)return null;
  int host=32-bits;int ip=((octets[0]&255)<<24)|((octets[1]&255)<<16)|((octets[2]&255)<<8)|(octets[3]&255);
  int mask=(int)(0xffffffffL << host);
  int bcast=(ip&mask)|~mask;
  return ((bcast>>>24)&255)+"."+((bcast>>>16)&255)+"."+((bcast>>>8)&255)+"."+(bcast&255);
 }
 public static Result probe(Context context,String host){
  ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
  if(cm==null)return new Result("Hálózati szolgáltatás nem elérhető","Nincs ConnectivityManager.");
  Network wifi=null;
  for(Network network:cm.getAllNetworks()){
   NetworkCapabilities caps=cm.getNetworkCapabilities(network);
   if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=network;break;}
  }
  if(wifi==null)return new Result("Nem található Wi-Fi hálózat","Csatlakozz a kamera AP-hálózatához.");
  StringBuilder log=new StringBuilder("BW4 keresési teszt (LAN_SEARCH)\n");
  log.append("Kamera IP: ").append(host).append("\n");
  LinkedHashSet<String> destinations=new LinkedHashSet<>();
  destinations.add(host);
  destinations.add("255.255.255.255");
  LinkProperties props=cm.getLinkProperties(wifi);
  if(props!=null)for(LinkAddress link:props.getLinkAddresses()){
   String broadcast=broadcastFor(link);if(broadcast!=null)destinations.add(broadcast);
  }
  log.append("Keresési célok: ").append(String.join(", ",destinations)).append("\n");
  int recognized=0,unknown=0,packets=0;
  LinkedHashSet<String> responders=new LinkedHashSet<>();
  try(DatagramSocket socket=new DatagramSocket(null)){
   socket.setReuseAddress(true);
   socket.bind(new InetSocketAddress(0));
   wifi.bindSocket(socket); // Prevent a mobile data/VPN route taking the packets.
   socket.setBroadcast(true);
   socket.setSoTimeout(250);
   byte[] buf=new byte[1500];
   for(int round=0;round<5;round++){
    for(String dest:destinations){
     try{
      InetAddress addr=InetAddress.getByName(dest);
      DatagramPacket request=new DatagramPacket(SEARCH,SEARCH.length,addr,PORT);
      socket.send(request);packets++;
     }catch(IOException e){log.append("Küldés sikertelen: ").append(dest).append(" (").append(e.getClass().getSimpleName()).append(")\n");}
    }
    long until=SystemClock.elapsedRealtime()+1000;
    while(SystemClock.elapsedRealtime()<until){
     try{
      DatagramPacket packet=new DatagramPacket(buf,buf.length);
      socket.receive(packet);
      if(packet.getLength()<4){unknown++;continue;}
      int magic=(packet.getData()[packet.getOffset()]&255)^0x15;
      int type=(packet.getData()[packet.getOffset()+1]&255)^0xdb;
      if(magic==0xf1&&type==0x91){
       recognized++;
       responders.add(packet.getAddress().getHostAddress()+":"+packet.getPort());
      }else{unknown++;}
     }catch(SocketTimeoutException ignored){} // Continue until round deadline.
    }
   }
   log.append("Elküldött UDP keresések: ").append(packets).append("\n");
   log.append("LAN_NOTIFY válaszok: ").append(recognized).append("\n");
   log.append("Más válaszok: ").append(unknown).append("\n");
   for(String responder:responders)log.append("Válaszadó: ").append(responder).append("\n");
   log.append("A BW4 gyári firmware ezt a felderítési módot nem feltétlenül támogatja.\n");
   log.append("A 81-es TCP-protokoll és a videófolyam nincs implementálva.\n");
   return new Result(recognized>0?"Helyi felderítés sikeres ("+recognized+" válasz)":"Nincs UDP-válasz broadcasttal sem",log.toString());
  }catch(Exception e){
   log.append("Keresés közbeni hiba: ").append(e.getClass().getSimpleName()).append("\n");
   return new Result("A BW4 keresése sikertelen",log.toString());
  }
 }
}
