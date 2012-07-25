package jp.ramensroom.timekeeperclientviawebsocket;

import java.net.URI;
import java.net.URISyntaxException;


import android.util.Log;

import de.roderick.weberknecht.WebSocket;
import de.roderick.weberknecht.WebSocketConnection;
import de.roderick.weberknecht.WebSocketEventHandler;
import de.roderick.weberknecht.WebSocketException;


public class WebSocketManager{
	private static final String TAG = "WebSocketManager";

	private static final String CMD_KEEP_ALIVE = "keep_alive";	// 鯖に対してコネクションが途切れないように定期的に送るメッセージ
	
	private static final int TIME_KEEP_ALIVE = 10000;	// 10 second == 10000 millsecond　で定期的にkeepALiveメッセージ送る

	private WebSocket webSocket;
	private boolean connectState;

	private Thread keepAliveThread;
	private boolean isKeepAlive;
	
	public WebSocketManager() {
		connectState = false;
		
		isKeepAlive = false;
	}
	
	// 接続メソッド
	public void connect(String url, WebSocketEventHandler handler){
		try {
			Log.d(TAG, "---connect()---");
			Log.d(TAG, "Server URL:" + url);
			URI uri = new URI(url);
			webSocket = new WebSocketConnection(uri);
			webSocket.setEventHandler(handler);
			webSocket.connect();
			
			connectState = true;

			Log.d(TAG, "Connect OK to " + url);
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException:" + e.getMessage());
			e.printStackTrace();
		} catch (WebSocketException e) {
			Log.e(TAG, "WebSocketException:" + e.getMessage());
			e.printStackTrace();
		}
	}

	// メッセージ送信メソッド
	public void send(String message){
		try {
			webSocket.send(message);
			Log.d(TAG, "Sent:" + message);
		} catch (WebSocketException e) {
			Log.e(TAG, "WebSocketException:" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// 接続を切るメソッド
	public void close() {
		try {
			isKeepAlive = false;
			if(keepAliveThread != null){
				keepAliveThread.stop();
				keepAliveThread = null;
			}

			webSocket.close();

			connectState = false;
			Log.d(TAG, "Connect closed.");
		} catch (WebSocketException e) {
			Log.e(TAG, "WebSocketException:" + e.getMessage());
			e.printStackTrace();
			isKeepAlive = false;
			if(keepAliveThread != null){
				keepAliveThread.stop();
			}
			keepAliveThread = null;
		}
	}
	
	// 接続されてる？
	public boolean isConnected(){
		return connectState;
	}
		
	// 生きてるよーメッセージを投げるか投げないかの設定メソッド
	public void setKeepAlive(boolean state){
		// 今メッセージ投げる状態じゃなくて、かつメソッドで投げてほしい(state == true)と来たならkeepAliveにする
		if(!isKeepAlive && state){
			isKeepAlive = true;
			
			// 定期的に処理させるためにThread処理
			keepAliveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while(isKeepAlive){
							webSocket.send(CMD_KEEP_ALIVE);
							Thread.sleep(TIME_KEEP_ALIVE); // TIME_KEEP_ALIVE ミリ秒間隔でメッセージ送信 (上の方で10000ミリ秒 == 10秒と設定)
						}
					} catch (WebSocketException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			keepAliveThread.start();

		// keepAliveしないなら処理ストップ
		}else if(!state){
			isKeepAlive = false;
			keepAliveThread.stop();
			keepAliveThread = null;
		}
	}
}