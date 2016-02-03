package edu.skku.checkroom;

import java.io.IOException;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MainService extends Service{
	
	private static String TAG = "MainService";
	public static final int MSG_SERVER_CONNECT = 1;
	public static final int MSG_SERVER_CLOSED = 2;
	public static final int MSG_CMD_RECEIVED = 3;
	public static final int MSG_SERVER_STREAM = 4;
	public static final int MSG_SERVER_MONITOR = 5;
	private static final int PORT = 14000;
	private ServerTask serverTask = null;
	private ServiceHandler mHandler = new ServiceHandler();
	private Messenger messenger = new Messenger(mHandler);
	private NotificationManager mNotificationManager;
	private boolean isConnected = false;
	private Vibrator vib;
	NotificationCompat.Builder mBuilder;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Binding Service");
		serverTask = new ServerTask(mHandler, PORT);
		serverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		Intent notiIntent = new Intent(getApplicationContext(),MainActivity.class);
		notiIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		notiIntent.setAction(Intent.ACTION_MAIN);
		notiIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent notiPandingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notiIntent, 0);
		mBuilder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("CheckRoom")
        .setContentText("실내 움직임이 감지되었습니다.");
		mBuilder.setContentIntent(notiPandingIntent);
		mBuilder.setAutoCancel(true);
		mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		return messenger.getBinder();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	
	
	@Override
	public void onDestroy() {
		try {
			serverTask.disconnect();
			mNotificationManager.cancel(8282);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.onDestroy();
	}



	@SuppressLint("HandlerLeak")
	private class ServiceHandler extends Handler{
		
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
			case MSG_CMD_RECEIVED:
				String m = (String) msg.obj;
				Log.d(TAG, "Message : " + m);
				StringTokenizer st = new StringTokenizer(m, "/");
				String cmd = st.nextToken();
				if(cmd.equals("Captured"))
				{
					if(st.hasMoreTokens())
						cmd = st.nextToken();
					mBuilder.setContentText(cmd);
					mNotificationManager.notify(8282, mBuilder.build());
					vib.vibrate(1000);
					sendBroadcast(new Intent(MainServiceIntent.ACTION_SET_NOTIFICATION));
				} else{
					Intent intent = new Intent(MainServiceIntent.ACTION_SET_DATA);
					if(cmd!=null)
						intent.putExtra(MainServiceIntent.VALUE_TEMPERATURE, cmd);
					if(st.hasMoreTokens())
						intent.putExtra(MainServiceIntent.VALUE_HUMIDITY, st.nextToken());
			    	sendBroadcast(intent);
				}
				if(!isConnected)
				{
					isConnected = true;
					sendBroadcast(new Intent(MainServiceIntent.ACTION_CLIENT_CONNECTED));
				}
				break;
			case MSG_SERVER_CONNECT:
				new ClientTask(mHandler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 
						(String)msg.obj, String.valueOf(msg.arg1), "14000");
				break;
			case MSG_SERVER_STREAM:
				new ClientTask(mHandler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 
						(String)msg.obj, String.valueOf(msg.arg1), "-2");
				break;
			case MSG_SERVER_MONITOR:
				new ClientTask(mHandler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 
						(String)msg.obj, String.valueOf(msg.arg1), "-1");
				break;
			case MSG_SERVER_CLOSED:
				if(isConnected)
				{
					isConnected = false;
					sendBroadcast(new Intent(MainServiceIntent.ACTION_CLIENT_DISCONNECTED));
				}
				break;
			}
		}
		
	};
}