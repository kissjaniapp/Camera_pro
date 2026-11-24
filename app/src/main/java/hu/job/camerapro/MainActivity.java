package hu.job.camerapro;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import android.view.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.work.*;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.Scope;
import java.util.concurrent.TimeUnit;
import java.io.*;
public class MainActivity extends Activity {
 private ExoPlayer player;
 private PlayerView view;
 private EditText url, days;
 private Spinner units, quality;
 private TextView status;
 private android.content.SharedPreferences prefs;
 private static final int AUTH=12, IMPORT=13;
 private static final String SCOPE="https://www.googleapis.com/auth/drive.file";
 private LinearLayout content,nav;
 private String page="home", apStatus="Nincs ellenőrizve";
 private final StringBuilder diag=new StringBuilder();
 private EditText apIp;
 private volatile int attempt=0;
 private static final int BG=0xff09121e, PANEL=0xff202d3b, ACCENT=0xff45c6dd;
 @Override public void onCreate(Bundle b){
 super.onCreate(b);prefs=getSharedPreferences("settings",0);
 getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 showHome();
 }
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private android.graphics.drawable.GradientDrawable bg(int c,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(c);d.setCornerRadius(dp(radius));return d;}
 private TextView text(String t,int size,int color){TextView x=new TextView(this);x.setText(t);x.setTextSize(size);x.setTextColor(color);return x;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 private void gap(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
 private void shell(String name,String key){
 attempt++;if(player!=null){player.release();player=null;}page=key;
 LinearLayout root=column();root.setBackgroundColor(BG);setContentView(root);
 TextView title=text(name,25,-1);title.setPadding(dp(18),dp(18),dp(16),dp(12));root.addView(title);
 ScrollView sc=new ScrollView(this);content=column();content.setPadding(dp(14),dp(4),dp(14),dp(16));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
 nav=new LinearLayout(this);nav.setBackgroundColor(PANEL);root.addView(nav,new LinearLayout.LayoutParams(-1,dp(64)));
 tab("Kamerák", "home",()->showHome());tab("Felvételek","files",()->showFiles());tab("Beállítások","settings",()->showSettings());
 status=text("",14,ACCENT);
 }
 private void tab(String label,String key,Runnable fn){TextView t=text(label,14,page.equals(key)?ACCENT:0xffaab6c6);t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,-1,1));t.setOnClickListener(v->fn.run());}
 private void showHome(){shell("Camera Pro    +","home");
 LinearLayout card=column();card.setBackground(bg(PANEL,16));card.setPadding(dp(16),dp(14),dp(16),dp(14));content.addView(card);
 TextView title=text("HEIYOUCAM BW4",20,-1);card.addView(title);status.setText(apStatus);card.addView(status);gap(card,12);
 TextView preview=text("▷\nÉlőkép megnyitása",22,0xffd0deec);preview.setGravity(Gravity.CENTER);preview.setBackground(bg(0xff050a10,10));card.addView(preview,new LinearLayout.LayoutParams(-1,dp(190)));preview.setOnClickListener(v->showLive());
 button(card,"Kamera megnyitása",()->showLive());button(content,"+ Kamera hozzáadása / AP beállítás",()->showLive());}
 private void showLive(){shell("‹    BW4  •  Élőkép","live");content.addView(status);status.setText(apStatus);
 view=new PlayerView(this);view.setBackgroundColor(0xff000000);content.addView(view,new LinearLayout.LayoutParams(-1,dp(205)));
 gap(content,8);TextView hint=text("AP mód – csatlakozz a kamera saját Wi-Fi-jéhez",15,0xffbdc9d9);content.addView(hint);
 apIp=new EditText(this);apIp.setSingleLine(true);apIp.setInputType(android.text.InputType.TYPE_CLASS_TEXT);apIp.setText(prefs.getString("apHost","192.168.168.1"));apIp.setHint("Kamera IP-cím");content.addView(apIp);
 button(content,"Wi-Fi hálózat kiválasztása",()->startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)));
 button(content,"AP csatlakozás / diagnosztika",()->probeAp());
 button(content,"BW4 valódi állapotlekérdezés (HTTP)",()->statusDialog());
 button(content,"BW4 helyi protokoll keresése (UDP)",()->probeLan());
 button(content,"Kapcsolatnapló megnyitása",()->new AlertDialog.Builder(this).setTitle("AP kapcsolatnapló").setMessage(diag.length()==0?"Még nincs vizsgálat.":diag.toString()).setPositiveButton("Bezárás",null).show());
 String[] names={"Mozgásérzékelés","Intercom","Sziréna","Fehér fény","Képernyőkép","Felvétel","Éjszakai látás","Érzékelési terület","Emberérzékelés"};
 String[] icons={"◉","♩","⚠","☼","▣","●","☾","⊞","♙"};
 for(int i=0;i<3;i++){LinearLayout row=new LinearLayout(this);content.addView(row);for(int j=0;j<3;j++){int k=i*3+j;LinearLayout tile=column();tile.setGravity(Gravity.CENTER);TextView circle=text(icons[k],28,0xff9aaabb);circle.setGravity(Gravity.CENTER);circle.setBackground(bg(PANEL,40));tile.addView(circle,new LinearLayout.LayoutParams(dp(62),dp(62)));TextView label=text(names[k],12,0xffbbcad9);label.setGravity(Gravity.CENTER);tile.addView(label);row.addView(tile,new LinearLayout.LayoutParams(0,dp(106),1));tile.setOnClickListener(v->Toast.makeText(this,"A BW4 vezérlőprotokoll még nincs igazolva.",Toast.LENGTH_SHORT).show());}}
 gap(content,8);url=new EditText(this);url.setSingleLine(true);url.setHint("Szabványos RTSP/HTTP videó URL (opcionális)");url.setText(prefs.getString("url",""));content.addView(url);button(content,"Szabványos élőkép indítása",()->play());button(content,"Leállítás",()->{if(player!=null)player.stop();});}
 private void probeAp(){
 String host=apIp.getText().toString().trim();if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")){apIp.setError("IPv4 cím szükséges");return;}
 for(String part:host.split("\\."))if(Integer.parseInt(part)>255){apIp.setError("Érvénytelen IP");return;}
 prefs.edit().putString("apHost",host).apply();int gen=++attempt;status.setText("Kapcsolat ellenőrzése…");diag.setLength(0);
 new Thread(()->{Bw4Client.Result result=Bw4Client.probe(this,host);runOnUiThread(()->{if(gen!=attempt||isFinishing())return;diag.append(result.log);apStatus=result.message;status.setText(apStatus);});}).start();
 }
 private void probeLan(){
 String host=apIp.getText().toString().trim();
 if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")){apIp.setError("Érvényes IP-cím szükséges");return;}
 for(String part:host.split("\\."))if(Integer.parseInt(part)>255){apIp.setError("Érvénytelen IP-cím");return;}
 prefs.edit().putString("apHost",host).apply();int gen=++attempt;status.setText("BW4 UDP-kamerakeresés folyamatban…");diag.setLength(0);
 new Thread(()->{Bw4Discovery.Result result=Bw4Discovery.probe(this,host);runOnUiThread(()->{if(gen!=attempt||isFinishing())return;diag.append(result.log);apStatus=result.message;status.setText(apStatus);});}).start();
 }
 private void statusDialog(){
  String host=apIp.getText().toString().trim();
  if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")){apIp.setError("IPv4-cím szükséges");return;}
  LinearLayout fields=column();fields.setPadding(dp(16),dp(8),dp(16),dp(4));
  EditText username=new EditText(this);username.setSingleLine(true);username.setHint("Felhasználónév");username.setText("admin");fields.addView(username);
  EditText password=new EditText(this);password.setSingleLine(true);password.setHint("Kamera jelszava");password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);fields.addView(password);
  new AlertDialog.Builder(this).setTitle("BW4 helyi hozzáférés").setMessage("A hitelesítési adatok csak ehhez a lekérdezéshez kellenek, nem mentjük őket.").setView(fields)
  .setNegativeButton("Mégse",null)
  .setPositiveButton("Státusz lekérése",(dialog,which)->{
    String u=username.getText().toString(),pw=password.getText().toString();int gen=++attempt;
    status.setText("BW4 státusz lekérdezése…");
    new Thread(()->{
      Bw4Status.Result result=Bw4Status.read(this,host,u,pw);
      runOnUiThread(()->{if(gen!=attempt||isFinishing())return;apStatus=result.message;status.setText(result.message);diag.setLength(0);diag.append(result.detail);new AlertDialog.Builder(this).setTitle(result.message).setMessage(result.detail).setPositiveButton("Rendben",null).show();});
    }).start();
  }).show();
 }
 private void showFiles(){shell("Felvételek","files");File[] f=new File(getFilesDir(),"queue").listFiles((d,n)->n.endsWith(".mp4"));content.addView(text("Helyi feltöltési sor: "+(f==null?0:f.length)+" felvétel",18,-1));content.addView(status);button(content,"MP4 felvétel importálása",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/mp4");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);});button(content,"Google Drive beállítások",()->showSettings());}
 private void showSettings(){shell("Beállítások","settings");LinearLayout root=content;root.addView(status);
 TextView label=new TextView(this);label.setText("Google Drive archiválás");label.setTextSize(20);root.addView(label);
 button(root,"Google-fiók csatlakoztatása",()->{
 GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new Scope(SCOPE)).build();
 startActivityForResult(GoogleSignIn.getClient(this,options).getSignInIntent(),AUTH);
 });
 days=new EditText(this);days.setInputType(2);days.setText(""+prefs.getInt("retention",30));root.addView(days);
 units=new Spinner(this);units.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"nap","hónap"}));units.setSelection(prefs.getInt("unit",0));root.addView(units);
 quality=new Spinner(this);quality.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Eredeti (legjobb)","1080p","720p","480p"}));quality.setSelection(prefs.getInt("quality",0));root.addView(quality);
 TextView note=new TextView(this);note.setText("A minőségválasztás egyelőre mentett beállítás, az importált MP4 eredeti minőségű. Automatikus kamerarögzítés még nincs. A lejárt, alkalmazással feltöltött videók a Drive kukájába kerülnek.");root.addView(note);
 button(root,"Archiválási beállítások mentése",()->save());
 button(root,"MP4 felvétel importálása és feltöltése",()->{if(!save())return; Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/mp4");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);});
 button(root,"Feltöltési sor ellenőrzése / újrapróbálása",()->{enqueue();status.setText("Szinkronizálás ütemezve");});
 }
 private void button(LinearLayout l,String t,Runnable r){Button b=new Button(this);b.setText(t);b.setOnClickListener(v->r.run());l.addView(b);}
 private boolean save(){try{int n=Integer.parseInt(days.getText().toString());if(n<1||n>3650)throw new Exception();prefs.edit().putInt("retention",n).putInt("unit",units.getSelectedItemPosition()).putInt("quality",quality.getSelectedItemPosition()).apply();enqueue();return true;}catch(Exception e){days.setError("Adj meg 1–3650 közötti értéket");return false;}}
 private void play(){String u=url.getText().toString().trim();if(!(u.startsWith("rtsp://")||u.startsWith("http://")||u.startsWith("https://"))){status.setText("Érvényes videó URL szükséges");return;}prefs.edit().putString("url",u).apply();if(player!=null)player.release();player=new ExoPlayer.Builder(this).build();view.setPlayer(player);player.addListener(new Player.Listener(){public void onPlayerError(PlaybackException e){status.setText("Videókapcsolat sikertelen: "+e.getErrorCodeName());} public void onPlaybackStateChanged(int s){if(s==Player.STATE_READY)status.setText("Videóforrás elérhető");}});player.setMediaItem(MediaItem.fromUri(u));player.prepare();player.play();}
 private void enqueue(){Constraints c=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();WorkManager w=WorkManager.getInstance(this);w.enqueueUniqueWork("drive-now",ExistingWorkPolicy.APPEND_OR_REPLACE,new OneTimeWorkRequest.Builder(DriveWorker.class).setConstraints(c).build());w.enqueueUniquePeriodicWork("drive-retention",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(DriveWorker.class,1,TimeUnit.DAYS).setConstraints(c).build());}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==AUTH){try{GoogleSignInAccount a=GoogleSignIn.getSignedInAccountFromIntent(data).getResult();status.setText("Google-fiók csatlakoztatva");enqueue();}catch(Exception e){status.setText("Google engedélyezés sikertelen. OAuth konfiguráció szükséges lehet.");}}
 if(req==IMPORT&&result==RESULT_OK&&data!=null){Uri u=data.getData();new Thread(()->{File dir=new File(getFilesDir(),"queue");dir.mkdirs();File f=new File(dir,java.util.UUID.randomUUID()+".mp4");File tmp=new File(dir,f.getName()+".part");try(InputStream in=getContentResolver().openInputStream(u);OutputStream out=new FileOutputStream(tmp)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);if(!tmp.renameTo(f))throw new IOException();enqueue();runOnUiThread(()->status.setText("Felvétel a feltöltési sorban"));}catch(Exception e){tmp.delete();runOnUiThread(()->status.setText("Importálási hiba"));}}).start();}}
 @Override protected void onStop(){if(player!=null){player.release();player=null;}super.onStop();}
}
