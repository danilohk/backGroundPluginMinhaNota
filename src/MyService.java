package com.escola.EducaOnline;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

public class MyService extends BackgroundService {
	
	private final static String TAG = MyService.class.getSimpleName();
	
	private static final String MY_DB_NAME = "risfNotificacao.db";
	
	private String mHelloTo = "World";

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected JSONObject doWork(){
		JSONObject result = new JSONObject();
		if(haveNetworkConnection()){
			//Log.d("LOG-->", "TA COM INTERNET");
			
			//BANCO SQLITE
			
			File dbFile = getDatabasePath(MY_DB_NAME);
			String path = dbFile.getAbsolutePath();
			SQLiteDatabase sampleDB =  null;
			String banco = "";
			
			File file = new File("/data/data/com.escola.EducaOnline/app_database/file__0/0000000000000001.db");
			
			if(file.exists()) {		
				sampleDB =  SQLiteDatabase.openDatabase("/data/data/com.escola.EducaOnline/app_database/file__0/0000000000000001.db" ,null, SQLiteDatabase.OPEN_READWRITE);
				banco = "/data/data/com.escola.EducaOnline/app_database/file__0/0000000000000001.db";
			}
			else{
				sampleDB =  SQLiteDatabase.openDatabase("/data/data/com.escola.EducaOnline/app_webview/databases/file__0/1" ,null, SQLiteDatabase.OPEN_READWRITE);
				banco = "/data/data/com.escola.EducaOnline/app_webview/databases/file__0/1";
			}
			
			//VARIAVEIS DE CONFIGURAÇÃO
	    	String sql = "SELECT pathFile, mat  FROM config";
			Cursor myResposta = sampleDB.rawQuery(sql, null);
			String pathFile = "";
			String mat = "";
				
			try {
				if (myResposta.getCount() > 0) {
	
					int size = myResposta.getCount();
					
					myResposta.moveToNext();
											
					pathFile = myResposta.getString(myResposta.getColumnIndex("pathFile"));
					mat = myResposta.getString(myResposta.getColumnIndex("mat"));
					mat = mat.replaceAll("[|]", "'");
														
				}
			}finally {
				
			}
			
			
			//CHECAGEM DE CONTROLE
	    	String ids = "";
	    	sql = "SELECT idRemoto FROM controle";
			myResposta = sampleDB.rawQuery(sql, null);
					    			
			try {
				if (myResposta.getCount() > 0) {
	
					int size = myResposta.getCount();
					for (int i = 0; i < size; i++){
					
					    myResposta.moveToNext();
					    ids += myResposta.getString(myResposta.getColumnIndex("idRemoto")) + ", ";
						
					}
				}
			}finally {
				
			}
			
			/////////////////////////////////////////
			//                                     //
			//      WEB SERVICE - REQUEST          //
			//                                     //
			/////////////////////////////////////////
			
			/* Create the POST request */
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("http://www.enderecoprotegido.com.br/_escola/_WSMinhaNota/servidor.php");
			// Request parameters and other properties.
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			
			params.add(new BasicNameValuePair("metodo", "retornarDataSet"));
			params.add(new BasicNameValuePair("idAutorizado", "douglas15369"));
			params.add(new BasicNameValuePair("pathFile", pathFile));
			params.add(new BasicNameValuePair("sql","SELECT envio_sms.*, solicitante.statusPagamento FROM envio_sms LEFT JOIN solicitante ON (envio_sms.idSolicitante = solicitante.idSolicitante)  WHERE solicitante.matricula IN("+mat+") AND idSms NOT IN("+ids+"0)"));
			
			try {
			    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			    // writing error to Log
			  //  e.printStackTrace();
			}
			/*
			 * Execute the HTTP Request
			 */
			try {
			    HttpResponse response = httpClient.execute(httpPost);
			    HttpEntity respEntity = response.getEntity();
	
			    if (respEntity != null) {
			        // EntityUtils to get the response content
			        String content =  EntityUtils.toString(respEntity);
			        //Log.d(TAG, content);
			        
			        //AQUI ACONTECE A MÁGICA
			        
			        //parsing JSON
			        JSONArray jArray = new JSONArray(content);
			        	
			        if(jArray.length() > 0){
			            //MANIPULAR JSON
			        	int idRemoto = 0;
			        	String texto = "";
			        	String statusPago = "";
			        	
			        	for(int i=0;i<jArray.length();i++){
			        		JSONObject json_data = jArray.getJSONObject(i);
			        		idRemoto = json_data.getInt("idSms");
			        		texto = json_data.getString("texto");
			        		statusPago = json_data.getString("statusPagamento");
			        					        		
			        		sampleDB.execSQL("INSERT INTO mensagens (texto, cor, idSolicitante, statusPago, dataHoraMsg) VALUES('" + texto + "', " +  json_data.getString("idTipoCor") + ", " +  json_data.getString("idSolicitante") + ", '" + statusPago + "', '"+json_data.getString("dataHoraEnvio")+"')");
			        		sampleDB.execSQL("INSERT INTO controle (idRemoto) VALUES(" + idRemoto + ")");
			        		
			        		if(!statusPago.equals("on")){	   
			        			texto = "Mensagens importantes para seu filho.";
			        		}
			        		else{
			        			sampleDB.execSQL("UPDATE mensagens SET statusPago = 'on' WHERE idSolicitante = " + json_data.getString("idSolicitante"));
			        		}
			        					        		
			        	}
			        	
			        	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			        	Intent notificationIntent = new Intent(this, com.escola.EducaOnline.CordovaApp.class);
					    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
					    NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
				         
					    if (currentapiVersion < android.os.Build.VERSION_CODES.HONEYCOMB){
			        		Notification notification;
				        	notification = new Notification(R.drawable.icon, texto, 0);
			        		notification.setLatestEventInfo(this, "MINHA NOTA", texto, contentIntent);
			        		notification.flags = Notification.FLAG_AUTO_CANCEL;
			        		mNotificationManager.notify(0, notification);

			        	} else {
					       //NOTIFICACAO
							Builder notification = new Notification.Builder(this)
						    .setContentTitle("MINHA NOTA")
						    .setContentText(texto)
						    .setSmallIcon(R.drawable.icon)
						    // .setStyle(new Notification.BigTextStyle()
						    // .bigText(texto))
						    .setDefaults(-1)
							.setAutoCancel (true);
						    notification.setContentIntent(contentIntent);
						   
						    mNotificationManager.notify(1, notification.build());
			        	}
				        
			        }
			    }
			} catch (ClientProtocolException e) {
			    // writing exception to log
			  //  e.printStackTrace();
			} catch (IOException e) {
			    // writing exception to log
			  //  e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sampleDB.close();
		}
		
		return result;	
	}

	@Override
	protected JSONObject getConfig() {
		JSONObject result = new JSONObject();
		
		try {
			result.put("HelloTo", this.mHelloTo);
		} catch (JSONException e) {
		}
		
		return result;
	}

	@Override
	protected void setConfig(JSONObject config) {
		/*try {
			if (config.has("pathFile"))
				this.pathFile = config.getString("pathFile");
			if (config.has("mat"))
				this.telefone = config.getString("mat");
		} catch (JSONException e) {
		}*/
		
	}     

	@Override
	protected JSONObject initialiseLatestResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void onTimerEnabled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onTimerDisabled() {
		// TODO Auto-generated method stub
		
	}
	
	private boolean haveNetworkConnection() {
	    boolean haveConnectedWifi = false;
	    boolean haveConnectedMobile = false;

	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
	    for (NetworkInfo ni : netInfo) {
	        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
	            if (ni.isConnected())
	                haveConnectedWifi = true;
	        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
	            if (ni.isConnected())
	                haveConnectedMobile = true;
	    }
	    return haveConnectedWifi || haveConnectedMobile;
	}
}
