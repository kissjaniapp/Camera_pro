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
 @Override public void onCreate(Bundle b){
 super.onCreate(b); prefs=getSharedPreferences("settings",0);
 ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);scroll.addView(root);setContentView(scroll);
 TextView title=new TextView(this);title.setText("Camera Pro • 0.1");title.setTextSize(26);root.addView(title);
 status=new TextView(this);status.setText("Nincs kapcsolat");root.addView(status);
 view=new PlayerView(this);root.addView(view,new LinearLayout.LayoutParams(-1, (int)(getResources().getDisplayMetrics().widthPixels*0.56)));
 url=new EditText(this);url.setSingleLine(true);url.setHint("RTSP vagy HTTP videó URL");url.setText(prefs.getString("url",""));root.addView(url);
 button(root,"Élőkép indítása (eredeti minőség)",()->play());
 button(root,"Élőkép leállítása",()->{if(player!=null)player.stop();});
 button(root,"BW4 helyi kapcsolat ellenőrzése",()->new Thread(()->{
 StringBuilder s=new StringBuilder();
 for(String ip:new String[]{"192.168.168.1","192.168.1.218"}){try(java.net.Socket socket=new java.net.Socket()){socket.connect(new java.net.InetSocketAddress(ip,81),2000);s.append(ip).append(":81 elérhető; a gyári protokoll még nincs támogatva.\n");}catch(Exception e){s.append(ip).append(":81 nem elérhető.\n");}}
 runOnUiThread(()->status.setText(s.toString()));
 }).start());
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
