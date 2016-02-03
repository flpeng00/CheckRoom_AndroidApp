package edu.skku.checkroom;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ClientTask extends AsyncTask<String, Void, Void>
{
	
	private static final String TAG = "ClientTask";
	private byte[] buffer = new byte[10];
	private byte[] temp = new byte[10];
	private Handler mHandler;
	
	ClientTask(Handler h)
	{
		mHandler = h;
	}

	@Override
	protected Void doInBackground(String... params) {
		int i = 0;
		String ip = params[0];
		int port = Integer.parseInt(params[1]);
		String message = params[2];
		
		try {
			Log.d(TAG, ip + ":" + port + " Connecting");
			Socket socket = new Socket(ip, port);
			OutputStream outputStream = socket.getOutputStream();
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
			temp = message.getBytes("US-ASCII");
			buffer[0] = 0x02;
			for(i=0;i<temp.length;i++){
				buffer[i+1] = temp[i];
			}
			buffer[i+1] = '\0';
			buffer[i+2] = 0x03;
			bufferedOutputStream.write(buffer, 0, i+3);
			Log.d(TAG, "Client Message Sent, Socket Closing");
			bufferedOutputStream.close();
			outputStream.close();
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			mHandler.sendMessage(Message.obtain(null, MainService.MSG_SERVER_CLOSED));
			Log.d(TAG, "CLOSED");
		} catch (IOException e) {
			e.printStackTrace();
			mHandler.sendMessage(Message.obtain(null, MainService.MSG_SERVER_CLOSED));
			Log.d(TAG, "CLOSED");
		}
		Log.d(TAG, "Client Socket Closed");
		return null;
	}
	
}