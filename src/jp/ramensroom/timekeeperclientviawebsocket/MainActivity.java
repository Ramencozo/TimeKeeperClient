package jp.ramensroom.timekeeperclientviawebsocket;

import de.roderick.weberknecht.WebSocketEventHandler;
import de.roderick.weberknecht.WebSocketMessage;
import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final int MAX_VALUE_WEBSOCKET_LIMIT_DATA_SIZE = 32099;

	private static final String TAG = "MainActivity";

	// SharedPreferencesの名前
	private static final String KEY_PREFERENCES = "jp.ramensroom.timekeeperclientviawebsocket.KEY_PREFERENCES";
	// SharedPreferencesに鯖URLを保存/取出しするキー
	private static final String KEY_SERVER_URL = "jp.ramensroom.timekeeperclientviawebsocket.KEY_SERVER_URL";

	// SharedPreferencesに設定した「時」を保存/取出しするキー
	private static final String KEY_HOUR = "jp.ramensroom.timekeeperclientviawebsocket.KEY_HOUR";
	// SharedPreferencesに設定した「分」を保存/取出しするキー
	private static final String KEY_MINUTE = "jp.ramensroom.timekeeperclientviawebsocket.KEY_MINUTE";
	// SharedPreferencesに設定した「秒」を保存/取出しするキー
	private static final String KEY_SECOND = "jp.ramensroom.timekeeperclientviawebsocket.LEY_SECOND";

	// 鯖の接続状態 TextView
	private TextView connectStateTextView;
	
	// 現在 時分秒 表示 TextView
	private TextView timeTextView;

	// 時 設定 Spinner
	private Spinner hourSpinner;
	// 分 設定 Spinner
	private Spinner minuteSpinner;
	// 秒 設定 Spinner
	private Spinner secondSpinner;
	
	// 鯖へ設定した時分秒を送信するボタン
	private Button setTimeButton;
	// 鯖へカウントダウンの開始/一時停止の設定を送信するボタン
	private Button startPauseButton;
	// 鯖へ時分秒のリセット設定を送信するボタン
	private Button resetButton;

	// WebSocket接続マネージャ
	private WebSocketManager manager;

	// GUIパーツ操作用 Handler
	private Handler handler;
	
	// 各種設定保存用 SharedPreferences
	private SharedPreferences preferences;

	// 時 データ保存用 変数
	private String currentHour;
	// 分 データ保存用 変数
	private String currentMinute;
	// 秒 データ保存用 変数
	private String currentSecond;

	// カウントダウン状態フラグ
	private boolean isCounting;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
				
		// SharedPreferences読み込み
		preferences = getSharedPreferences(KEY_PREFERENCES, MODE_APPEND | MODE_PRIVATE);
		
		// 時分秒データ読み出し、初期化
		currentHour = preferences.getString(KEY_HOUR, "00");
		currentMinute = preferences.getString(KEY_MINUTE, "00");
		currentSecond = preferences.getString(KEY_SECOND, "00");
				
		handler = new Handler();
		
		manager = new WebSocketManager();		

		connectStateTextView = (TextView)findViewById(R.id.connect_state_textview);
		
		timeTextView = (TextView)findViewById(R.id.time_textview);
		timeTextView.setText(currentHour + ":" + currentMinute + ":" + currentSecond);
		
		// 時 設定用 Spinnerの中のデータ(00～59 のStringデータ)を生成
		ArrayAdapter<String> hourAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for(int i=0; i<60; i++){
			hourAdapter.add((i > 9 ? "" + i : "0" + i));
		}
		
		// SharedPreferencesに保存されてる「前回設定した『時』の、Spinner内でのインデックス」を探す
		int hourPosition = 0;
		for(int i=0; i<hourAdapter.getCount(); i++){
			if(currentHour.equals(hourAdapter.getItem(i))){
				hourPosition = i;
				break;
			}
		}
		
		// Spinner初期化
		hourSpinner = (Spinner)findViewById(R.id.hour_spinner);
		hourSpinner.setAdapter(hourAdapter);	// ArrayAdapterセット
		hourSpinner.setSelection(hourPosition); // 上記で探したインデックスの位置にSpinnerを設定
		hourSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// Spinnerでアイテム(00～59のデータ)を選択した時に呼ばれる処理
				// そのアイテムを『時』データとして持ってくる
				Spinner spinner = (Spinner)parent;
				currentHour = (String)spinner.getSelectedItem();
				timeTextView.setText(currentHour + ":" + currentMinute + ":" + currentSecond);
				
				// SharedPrefencesに保存　(アプリの次回起動時に読み込まれる)
				Editor editor = preferences.edit();
				editor.putString(KEY_HOUR, currentHour);
				editor.commit();
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// 分のSpinner用のArrayAdapter、 時 のSpinnerと同じ処理
		ArrayAdapter<String> minuteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for(int i=0; i<60; i++){
			minuteAdapter.add((i > 9 ? "" + i : "0" + i));
		}

		int minutePosition = 0;
		for(int i=0; i<minuteAdapter.getCount(); i++){
			if(currentMinute.equals(minuteAdapter.getItem(i))){
				minutePosition = i;
				break;
			}
		}

		minuteSpinner = (Spinner)findViewById(R.id.minute_spinner);
		minuteSpinner.setAdapter(minuteAdapter);
		minuteSpinner.setSelection(minutePosition);
		minuteSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Spinner spinner = (Spinner)parent;
				currentMinute = (String)spinner.getSelectedItem();
				timeTextView.setText(currentHour + ":" + currentMinute + ":" + currentSecond);

				Editor editor = preferences.edit();
				editor.putString(KEY_MINUTE, currentMinute);
				editor.commit();
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		// 秒のSpinner用のArrayAdapter、 時、分 のSpinnerと同じ処理
		ArrayAdapter<String> secondAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		secondAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for(int i=0; i<60; i++){
			secondAdapter.add((i > 9 ? "" + i : "0" + i));
		}

		int secondPosition = 0;
		for(int i=0; i<secondAdapter.getCount(); i++){
			if(currentSecond.equals(secondAdapter.getItem(i))){
				secondPosition = i;
				break;
			}
		}

		secondSpinner = (Spinner)findViewById(R.id.second_spinner);
		secondSpinner.setAdapter(secondAdapter);
		secondSpinner.setSelection(secondPosition);
		secondSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Spinner spinner = (Spinner)parent;
				currentSecond = (String)spinner.getSelectedItem();
				timeTextView.setText(currentHour + ":" + currentMinute + ":" + currentSecond);

				Editor editor = preferences.edit();
				editor.putString(KEY_SECOND, currentSecond);
				editor.commit();
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});	
		
		// 設定した時分秒を鯖へ送る処理用のButton
		setTimeButton = (Button)findViewById(R.id.set_time_button);
		setTimeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// WebSocketManager使える状態？
				if(manager != null){
					// WebSocketManager、鯖と接続されてる？
					if(manager.isConnected()){
						// 「set,HH:MM:SS」の形式で送信
						manager.send("set," + currentHour + ":" + currentMinute + ":" + currentSecond);
						Toast.makeText(getApplicationContext(), "Set Time: " + currentHour + ":" + currentMinute + ":" + currentSecond, Toast.LENGTH_SHORT).show();
					}else {
						Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
					}
				}else {
					Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		// カウントダウンの開始/一時停止用Button
		startPauseButton = (Button)findViewById(R.id.start_pause_button);
		startPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// カウントダウン中？
				if (isCounting) {
					// WebSocketManager生きてる？
					if(manager != null){
						// WebSocketManager接続されてる？
						if(manager.isConnected()){
							// カウントダウン一時停止命令を投げる
							manager.send("pause,null");
							Toast.makeText(getApplicationContext(), "Count Down Paused.", Toast.LENGTH_SHORT).show();
							isCounting = false;
							startPauseButton.setText("Start");
						}else{
							Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
						}
					}else {
						Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
					}
				}else{
					if(manager != null){
						if(manager.isConnected()){
							// カウントダウン開始命令を投げる
							manager.send("start,null");
							Toast.makeText(getApplicationContext(), "Count Down Started.", Toast.LENGTH_SHORT).show();
							isCounting = true;
							startPauseButton.setText("Pause");
						}else{
							Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
						}
					}else {
						Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		
		// リセットボタン
		resetButton = (Button)findViewById(R.id.reset_button);
		resetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(manager != null){
					if(manager.isConnected()){
						// リセット命令を投げる
						manager.send("reset,null");
						isCounting = false;
						startPauseButton.setText("Start");
					}else{
						Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
					}
				}else {
					Toast.makeText(getApplicationContext(), "Not Connected to TimeKeeper Server.", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		// onPauseでWebSocketManagerをつぶす
		if(manager.isConnected()){
			manager.setKeepAlive(false);
			manager.close();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// メニュー生成
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		// 「Connect/Disconnect」メニュータップ時の処理
		case R.id.menu_connect:
			LayoutInflater inflater = LayoutInflater.from(this);

			// ダイアログ用のViewを生成
			final View dialogView = inflater.inflate(R.layout.menu_connect, null);	
			
			// ↑で生成したView内のEditTextを初期化
			final EditText serverURLEditText = (EditText)dialogView.findViewById(R.id.server_url_edittext);
			// SharedPreferencesに保存されてる鯖URL読み込み(なければループバックアドレス)
			serverURLEditText.setText(preferences.getString(KEY_SERVER_URL, "127.0.0.1:8000"));

			// ダイアログのセッティング
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
										.setTitle("Connect to Server")
										.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												// WebSocketManagerが接続されていない状態ならば
												if(!manager.isConnected()){
													// 接続開始。鯖URLをEditTextから拾う
													final String serverURL = serverURLEditText.getText().toString();													
													
													// SharedPreferencesに保存する
													Editor editor = preferences.edit();
													editor.putString(KEY_SERVER_URL, serverURL);
													editor.commit();
													
													// 接続処理 ( ws://鯖アドレス:ポート番号/echo )
													manager.connect("ws://" + serverURL + "/echo", new WebSocketEventHandler() {
														@Override
														public void onOpen() {
															// 接続完了直後の処理
															handler.post(new Runnable() {
																@Override
																public void run() {
																	Toast.makeText(getApplicationContext(), "WebSocket Connected.", Toast.LENGTH_SHORT).show();
																	
																	// 接続状態のTextViewを書き換え
																	connectStateTextView.setText("Connected: " + "ws://" + serverURL + "/echo");
																	manager.setKeepAlive(true);
																}
															});
														}
														
														@Override
														public void onMessage(WebSocketMessage message) {
															// メッセージ受信時の処理
															final String msg = message.getText();
															handler.post(new Runnable() {
																@Override
																public void run() {
																	// 各種命令「ではないもの」(== 鯖からの「現在時間データ」)が飛んできたときに処理
																	if(	!msg.equals("keep_alive") &&
																		!msg.equals("start,null") &&
																		!msg.equals("pause,null") &&
																		!msg.equals("reset,null")){
																		timeTextView.setText(msg);
																	}
																}
															});
															Log.d(TAG, "---onMessage()---");
															Log.d(TAG, " - Message: " + msg);
														}
														
														@Override
														public void onClose() {
															handler.post(new Runnable() {
																@Override
																public void run() {
																	Toast.makeText(getApplicationContext(), "WebSocket Disconnected.", Toast.LENGTH_SHORT).show();
																}
															});
														}
													});
												}else{
													Toast.makeText(getApplicationContext(), "Already connected.", Toast.LENGTH_SHORT).show();
												}
											}
										})
										.setNegativeButton("Disconnect", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												if(manager.isConnected()){
													manager.close();
												}
											}
										});
			builder.setView(dialogView);
			AlertDialog dialog = builder.create();
			dialog.show();	// ダイアログを表示
			break;
		}
		
		return false;
	}
}