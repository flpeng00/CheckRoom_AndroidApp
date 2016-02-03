package edu.skku.checkroom;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
	
	public static final String TAG = "MainActivity";
	private EditText editServerIp;
	private EditText editServerPort;
	private TextView textTh;
	private Button btnConnect;
	private Button btnStream;
	private String serverIp = null;
	private ListView lvLog;
	private ArrayList<String[]> logItems;
	private LogAdapter logAdapter;
	private int serverPort = -1;
	private Messenger serviceMessenger = null;
	
	private ServiceConnection conn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger = null;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bindService(new Intent(this, MainService.class), conn, Context.BIND_AUTO_CREATE);
		btnConnect = (Button)findViewById(R.id.btn_connect);
		btnStream = (Button)findViewById(R.id.btn_stream);
		editServerIp = (EditText)findViewById(R.id.edit_serverip);
		editServerPort = (EditText)findViewById(R.id.edit_serverport);
		textTh = (TextView)findViewById(R.id.text_th);
		btnConnect.setOnClickListener(l);
		btnStream.setOnClickListener(l);
		btnStream.setEnabled(false);
		lvLog = (ListView)findViewById(R.id.listview_log);
		logItems = new ArrayList<String[]>();
		logAdapter = new LogAdapter(this, R.layout.row_log, logItems);
		lvLog.setAdapter(logAdapter);
		
		IntentFilter filter = new IntentFilter();
    	filter.addAction(MainServiceIntent.ACTION_CLIENT_CONNECTED);
    	filter.addAction(MainServiceIntent.ACTION_SET_DATA);
    	filter.addAction(MainServiceIntent.ACTION_SET_NOTIFICATION);
    	filter.addAction(MainServiceIntent.ACTION_CLIENT_DISCONNECTED);
	    registerReceiver(serviceConnReceiver, filter);
	}
	
	private OnClickListener l = new OnClickListener(){

		@SuppressLint("NewApi")
		@Override
		public void onClick(View v) {
			
			switch(v.getId()){
			
			case R.id.btn_connect:
				try {
					serverIp = editServerIp.getText().toString();
					serverPort = Integer.parseInt(editServerPort.getText().toString());
					serviceMessenger.send(Message.obtain(null, MainService.MSG_SERVER_CONNECT, serverPort, 0, serverIp));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case R.id.btn_stream:
				try {
					addLogItem("WebStreaming");
					serviceMessenger.send(Message.obtain(null, MainService.MSG_SERVER_STREAM, serverPort, 0, serverIp));
					Intent intent = new Intent(MainActivity.this, CameraActivity.class);
	        		intent.putExtra("SERVER_IP", serverIp);
	        		intent.putExtra("SERVER_PORT", serverPort);
	        		MainActivity.this.startActivity(intent);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(conn);
		unregisterReceiver(serviceConnReceiver);
	}
	
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
        	if(serviceMessenger != null && serverIp != null){
        		serviceMessenger.send(Message.obtain(null, MainService.MSG_SERVER_MONITOR, serverPort, 0, serverIp));
        		addLogItem("Monitoring");
        	}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	private BroadcastReceiver serviceConnReceiver = new BroadcastReceiver(){
		
		@SuppressLint("SimpleDateFormat")
		@Override
        public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(MainServiceIntent.ACTION_CLIENT_CONNECTED.equals(action)){
				btnConnect.setText("CONNECTED");
				btnConnect.setEnabled(false);
				btnStream.setEnabled(true);
				Log.d(TAG, "BR : ACTION_CLIENT_CONNECTED");
				addLogItem("CubieCam Connected");
				addLogItem("Monitoring");
			} else if(MainServiceIntent.ACTION_SET_DATA.equals(action)){
				String t = intent.getStringExtra(MainServiceIntent.VALUE_TEMPERATURE);
				String h = intent.getStringExtra(MainServiceIntent.VALUE_HUMIDITY);
				Log.d(TAG, "BR : ACTION_SET_DATA " + t + "/" + h);
				textTh.setText(t + "¨¬C / " + h + "%");
			} else if(MainServiceIntent.ACTION_SET_NOTIFICATION.equals(action)){
				addLogItem("Captured");			
			} else if(MainServiceIntent.ACTION_CLIENT_DISCONNECTED.equals(action)){
				btnConnect.setText("CONNECT");
				btnConnect.setEnabled(true);
				btnStream.setEnabled(false);
				textTh.setText("Disconnected");
				Log.d(TAG, "BR : ACTION_CLIENT_DISCONNECTED");
				addLogItem("CubieCam Disconnected");
			}
		}
	};
	
	@SuppressLint("SimpleDateFormat")
	private void addLogItem(String s){
		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = df.format(c.getTime());
		String[] l = new String[2];
		l[0] = formattedDate;
		l[1] = s;
		Log.d(TAG, "Add LogItem : " + l[0] + " / " + l[1]);
		logItems.add(l);
		logAdapter.notifyDataSetChanged();	
	}
	
	private class LogAdapter extends ArrayAdapter<String[]>	 {
		
		private ArrayList<String[]> items;

		public LogAdapter(Context context, int resource, ArrayList<String[]> items) {
			super(context, resource, items);
			this.items = items;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			 View v = convertView;
             if (v == null) {
                 LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                 v = vi.inflate(R.layout.row_log, null);
             }
             String[] logItems = items.get(position);
             if (logItems != null) {
                     TextView textDate = (TextView) v.findViewById(R.id.text_log_date);
                     TextView textContent = (TextView) v.findViewById(R.id.text_log_content);
                     if (textDate != null){
                         textDate.setText(logItems[0]);                            
                     }
                     if(textContent != null){
                    	 textContent.setText(logItems[1]);
                     }
             }
             return v;
		}
	}
}
