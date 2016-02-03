package edu.skku.checkroom;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ServerTask extends AsyncTask<Void, Void, Void>{
	
	private final String TAG = "ServerTask";
	private int port;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private Handler mHandler;
	private byte[] buffer = new byte[30];
	private byte[] cmd = new byte[30];
	
	ServerTask(Handler h, int p)
	{
		mHandler = h;
		port = p;
	}
	
	
	@SuppressLint("NewApi")
	@Override
	protected Void doInBackground(Void... params) {
		try {
			serverSocket = new ServerSocket(port);
			int i = 0;
			String s;
			
			while(true){
				Log.d(TAG, "Server Waiting");
				socket = serverSocket.accept();
				InputStream inputStream = socket.getInputStream();
				BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
				bufferedInputStream.read(buffer);
				i = 0;
				if(buffer[0] == 0x02){
					while(buffer[i] != 0x03)
					{
						i++;
						if(i == buffer.length)
							break;
					}
				}
				cmd = Arrays.copyOfRange(buffer, 1, i);
				s = new String(cmd);
				mHandler.sendMessage(Message.obtain(null, MainService.MSG_CMD_RECEIVED, s));
				if(socket != null)
					socket.close();
				socket = null;
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
	
	public void disconnect() throws IOException
	{
		Message msg;
		
		if(socket != null)
			socket.close();
		socket = null;
		
		if(serverSocket != null)
			serverSocket.close();
		serverSocket = null;
		
		msg = Message.obtain(mHandler, MainService.MSG_SERVER_CLOSED);
		mHandler.sendMessage(msg);
		
	}
	
}