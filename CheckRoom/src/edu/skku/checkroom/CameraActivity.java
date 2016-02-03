package edu.skku.checkroom;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

@SuppressLint("NewApi")
public class CameraActivity extends ActionBarActivity {
	
	public static final String TAG = "CameraActivity";
	public static final int MSG_ACTIVITY_READY = 0;
	private WebView webView;
	private CameraHandler mHandler = new CameraHandler();
	String addr = null;
	int port = -1;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		Bundle extras = getIntent().getExtras();
		addr = extras.getString("SERVER_IP");
		port = extras.getInt("SERVER_PORT");
		
		webView = (WebView)findViewById(R.id.wv_stream);
		webView.addJavascriptInterface(new WebAppInterface(this), "Android"); 
	    WebSettings webSettings = webView.getSettings();
	    webSettings.setJavaScriptEnabled(true);
	    webSettings.setSupportMultipleWindows(false);
	    webView.setWebChromeClient(new WebChromeClient() {});
	    
	    final ProgressDialog pDialog = new ProgressDialog(CameraActivity.this);
	    pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    pDialog.setTitle("Please wait");
	    pDialog.setMessage("Start streaming service...");
	    pDialog.show();
	    new AsyncTask<Void, Void, Void>(){
			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					Thread.sleep(1000);
					mHandler.sendMessage(Message.obtain(null, MSG_ACTIVITY_READY));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				pDialog.dismiss();
				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		final ProgressDialog pDialog = new ProgressDialog(CameraActivity.this);
	    pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    pDialog.setTitle("Please wait");
	    pDialog.setMessage("Exit streaming service...");
	    pDialog.show();
	    try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void getFromJS(String toast) { 
           if(toast.equals("close"))
           {
              finish();
           }
        }
    }
    
	@SuppressLint("HandlerLeak")
	private class CameraHandler extends Handler{
		
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
			case MSG_ACTIVITY_READY:
				webView.loadUrl("http://" + addr + ":8080/javascript_simple.html");
				break;
			}
		}
		
	};
}
