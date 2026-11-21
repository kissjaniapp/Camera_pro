package hu.job.camerapro;
import android.content.*;
import androidx.work.*;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
public class DriveWorker extends Worker {
 public DriveWorker(Context c,WorkerParameters p){super(c,p);}
 private String token;
 private static final String BASE="https://www.googleapis.com/drive/v3/files";
 private HttpURLConnection conn(String url,String method)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+token);c.setConnectTimeout(20000);c.setReadTimeout(60000);return c;}
 private JSONObject response(HttpURLConnection c)throws Exception{try{int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("Drive HTTP "+code);try(InputStream in=c.getInputStream()){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return b.size()==0?new JSONObject():new JSONObject(b.toString("UTF-8"));}}finally{c.disconnect();}}
 private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 private JSONObject list(String q,String page)throws Exception{return response(conn(BASE+"?q="+enc(q)+"&fields="+enc("nextPageToken,files(id,name,appProperties,createdTime)")+"&pageSize=100"+(page==null?"":"&pageToken="+enc(page)),"GET"));}
 private void upload(File f)throws Exception{
 String id=f.getName();
 String q="trashed = false and appProperties has { key='cameraProId' and value='"+id+"' }";
 if(list(q,null).getJSONArray("files").length()>0){f.delete();return;}
 String boundary="cp"+UUID.randomUUID().toString().replace("-","");
 JSONObject meta=new JSONObject().put("name","CameraPro_"+f.getName()).put("appProperties",new JSONObject().put("cameraProId",id).put("cameraProArchive","v1").put("savedAt",""+f.lastModified()));
 HttpURLConnection c=conn("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart","POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","multipart/related; boundary="+boundary);
 byte[] head=("--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+meta+"\r\n--"+boundary+"\r\nContent-Type: video/mp4\r\n\r\n").getBytes("UTF-8");byte[] tail=("\r\n--"+boundary+"--\r\n").getBytes("UTF-8");c.setFixedLengthStreamingMode(head.length+f.length()+tail.length);
 try(OutputStream out=c.getOutputStream();InputStream in=new FileInputStream(f)){out.write(head);byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.write(tail);}
 response(c);f.delete();
 }
 private void cleanup()throws Exception{
 android.content.SharedPreferences p=getApplicationContext().getSharedPreferences("settings",0);Calendar cal=Calendar.getInstance();cal.add(p.getInt("unit",0)==1?Calendar.MONTH:Calendar.DAY_OF_YEAR,-p.getInt("retention",30));long cutoff=cal.getTimeInMillis();String page=null;ArrayList<String> expired=new ArrayList<>();
 do{JSONObject data=list("trashed = false and appProperties has { key='cameraProArchive' and value='v1' }",page);JSONArray a=data.getJSONArray("files");for(int i=0;i<a.length();i++){JSONObject f=a.getJSONObject(i);JSONObject props=f.optJSONObject("appProperties");if(props!=null&&Long.parseLong(props.optString("savedAt","9223372036854775807"))<cutoff)expired.add(f.getString("id"));}page=data.optString("nextPageToken",null);}while(page!=null&&!page.isEmpty());
 for(String id:expired){HttpURLConnection c=conn(BASE+"/"+enc(id),"PATCH");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write("{\"trashed\":true}".getBytes("UTF-8"));}response(c);}
 }
 @Override public Result doWork(){try{GoogleSignInAccount a=GoogleSignIn.getLastSignedInAccount(getApplicationContext());if(a==null||a.getAccount()==null)return Result.retry();token=GoogleAuthUtil.getToken(getApplicationContext(),a.getAccount(),"oauth2:https://www.googleapis.com/auth/drive.file");File[] files=new File(getApplicationContext().getFilesDir(),"queue").listFiles((d,n)->n.endsWith(".mp4"));if(files!=null)for(File f:files)upload(f);cleanup();return Result.success();}catch(Exception e){return Result.retry();}}
}
