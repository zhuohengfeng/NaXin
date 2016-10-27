package org.ryan.naxin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SendAudio extends Activity {
	private Button mSendButton = null;
	private TextView mStateText = null;
	
	private Person chatPerson = null;
	private Person myPerson  = null;
	
	private int startAudioCmdType = 0x0;
	private boolean isHandupAudio = false;

	private RecvAudioThread mRecvAudioThread = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.sendaudio);
		
		startAudioCmdType = (int)(getIntent().getExtras().getInt(Constant.audioCmdStr));
		myPerson = (Person)(getIntent().getExtras().getSerializable(Constant.myInfoStr));
		chatPerson = (Person)(getIntent().getExtras().getSerializable(Constant.userNameStr));
		mSendButton = (Button)findViewById(R.id.send_audio);
		mSendButton.setEnabled(false);
		mStateText = (TextView)findViewById(R.id.audio_connect);
		if(startAudioCmdType == 0x01){
			mStateText.setText("请求与"+chatPerson.getName()+"连接中...");
		}
		else{
			mStateText.setText("已与"+chatPerson.getName()+"连接进行语音通话");
			
			// 启动一个线程开始接收语音
			mRecvAudioThread = new RecvAudioThread(chatPerson);
			mRecvAudioThread.start();
			
			if(mMainService != null){
				mSendButton.setEnabled(true);
			}
		}
		
		
		mSendButton.setOnTouchListener(new View.OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
						if(Constant.DEBUG) Log.i(Constant.TAG, "start record the audio!");
						if(mMainService != null){
							startSendAudio(chatPerson);
						}
						break;
						
					case MotionEvent.ACTION_UP:
						if(Constant.DEBUG) Log.i(Constant.TAG, "end record the audio!");
						if(mMainService != null){
							stopSendAudio();
						}
						break;
				}
				return false;
			}
		});
		
		// --------------------绑定服务-----------------------------
		mServiceIntent = new Intent(SendAudio.this, MainService.class);
		this.bindService(mServiceIntent, mServiceConn, BIND_AUTO_CREATE);
		
		// ------------------接收广播-------------------------------
		IntentFilter mAudioFilter = new IntentFilter();
		mAudioFilter.addAction(Constant.BR_RejectAudioConnect);
		mAudioFilter.addAction(Constant.BR_AgreetAudioConnect);
		mAudioFilter.addAction(Constant.BR_DisconnectAudio);
		this.registerReceiver(mAudioReceiver, mAudioFilter);
	}
	
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		//if(Constant.DEBUG) Log.i(Constant.TAG,"SendAudio ====> onTouchEvent");
		// 点击屏幕，不销毁窗口
		return true;
		//return super.onTouchEvent(event);
	}




	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(mAudioReceiver);
		this.unbindService(mServiceConn);
		mServiceConn = null;
		if(mMainService != null){
			if(!isHandupAudio){
				mMainService.disconnectAudio(chatPerson);
			}
			stopSendAudio();
			stopRecvAudio();
			mMainService = null;
		}
		super.onDestroy();
	}
	
	
	//---------------------------------------------------------
	// 启动服务的Intent
	private Intent mServiceIntent = null;
	private MainService mMainService = null;
	
	private ServiceConnection mServiceConn = new ServiceConnection(){
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			// 这里的IBinder是本地的，所以可以直接使用
			if(Constant.DEBUG) Log.i(Constant.TAG,"SendAudio: the Service is connedted!");
			mMainService = ((MainService.MyBinder)service).getServiceInstance();
			
			
			if(startAudioCmdType == 0x01){
				// 发送语音通话的请求
				mMainService.sendAudioRequest(myPerson.getUID(), chatPerson.getPersonHost());
			}
			else{
				if(!mSendButton.isEnabled()){
					mSendButton.setEnabled(true);	
				}
			}
			
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			if(Constant.DEBUG) Log.i(Constant.TAG,"SendAudio: the Service is Disconnedted!");
			
		}
	};
	
	
	//-------------广播接收器-------------------
	private BroadcastReceiver mAudioReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(Constant.BR_RejectAudioConnect)){
				Toast.makeText(SendAudio.this, "对方拒绝进行语音通话！", Toast.LENGTH_LONG).show();
				SendAudio.this.finish();
			}
			else if(action.equals(Constant.BR_AgreetAudioConnect)){
				Toast.makeText(SendAudio.this, "对方同意进行语音通话！", Toast.LENGTH_SHORT).show();
				mStateText.setText("已与"+chatPerson.getName()+"连接进行语音通话");
				
				// 启动一个线程开始接收语音
				mRecvAudioThread = new RecvAudioThread(chatPerson);
				mRecvAudioThread.start();
				
				if(mMainService != null){
					mSendButton.setEnabled(true);	
				}
			}
			else if(action.equals(Constant.BR_DisconnectAudio)){
				isHandupAudio = true;
				Toast.makeText(SendAudio.this, "对话挂断了语音通话！", Toast.LENGTH_SHORT).show();
				stopSendAudio();
				stopRecvAudio();
				SendAudio.this.finish();
			}
			
			
		}
	};
	
	
	//---------------------------------------------------------------------
	private boolean isStopSendAudioThread = false;
	private boolean isStopRecvAudioThread = false;
	
	private AudioRecord mAudioRecord = null;
	private AudioTrack mAudioTrack = null;
	private int mRecordbufferSizeInBytes = 0;
	private int mTrackbufferSizeInBytes = 0;
	
    static final int frequency = 44100;  
    static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
	
	
	//-------初始化录音----------
	public boolean initSendAudio(){
		boolean ret = true;
        mRecordbufferSizeInBytes = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
    	mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
						    			frequency, 
						    			channelConfiguration, 
						    			audioEncoding, 
						    			mRecordbufferSizeInBytes);
    	return ret;
	}
	
	//-------初始化播放----------
	public boolean initRecvAudio(){
		boolean ret = true;
		mTrackbufferSizeInBytes = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
    	mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 
									frequency, 
									channelConfiguration, 
									audioEncoding, 
									mTrackbufferSizeInBytes, 
									AudioTrack.MODE_STREAM);

		//设置当前音量大小   
		mAudioTrack.setStereoVolume(1.0f, 1.0f);
    	return ret;
	}
	
	
	
	public void startSendAudio(final Person chatPerson){
		//-------------开始启动 录音----------------
    	isStopSendAudioThread = false;
		new SendAudioThread(chatPerson).start();
	}
	
	
	// 停止发送语音
	public void stopSendAudio(){
		if(Constant.DEBUG) Log.w(Constant.TAG, "stopSendAudio");
		isStopSendAudioThread = true;
	}
	
	// 停止接收送语音
	public void stopRecvAudio(){
		if(Constant.DEBUG) Log.w(Constant.TAG, "stopRecvAudio");
		isStopRecvAudioThread = true;
		
		if(mRecvAudioThread != null){
			mRecvAudioThread.release();
			mRecvAudioThread = null;
		}
		
	}
	
	
	
	// ==========发送/接收语音采用TCP协议========================
		// 发送语音线程
		private class SendAudioThread extends Thread{
			private Person chatPerson = null;
			
			public SendAudioThread(Person mPerson) {
				super();
				// TODO Auto-generated constructor stub
				this.chatPerson = mPerson;
			}

			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				
				// 创建客户端连接服务器的client
				InetAddress sendAddress = null;
				Socket sendSocket = null;
				OutputStream socketStream = null;
	            byte[] readbuffer = new byte[1024];  
	            
				try {
					// 创建Socket
					sendAddress = InetAddress.getByName(chatPerson.getPersonHost());
					sendSocket = new Socket(sendAddress, Constant.AudioPORT);
					socketStream = sendSocket.getOutputStream();
					
					initSendAudio();
					mAudioRecord.startRecording();//开始录制   
					while(!isStopSendAudioThread){// 发送线程也保持一致存在！！！！
						
						// 读取录音流，返回读取的字节数
						int bufferReadResult = mAudioRecord.read(readbuffer, 0, 640);
						if(Constant.DEBUG) Log.d(Constant.TAG, "发送语音数据长度="+bufferReadResult);
						if(bufferReadResult>0 && bufferReadResult%2==0){
							socketStream.write(readbuffer, 0, bufferReadResult);
							socketStream.flush();
						}
						
					}
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					// 无论结果如何都要关闭socket
					try {
						if(null!=mAudioRecord) {
							mAudioRecord.stop();
							mAudioRecord.release();
							mAudioRecord = null;
						}
						if(null!=socketStream)socketStream.close();
						if(!sendSocket.isClosed())sendSocket.close();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
				
			}
		}
		
		// 接收语音的线程
		private class RecvAudioThread extends Thread{
			private Person chatPerson = null;
			private ServerSocket recvSocketServer = null;
			
			public RecvAudioThread(Person mPerson) {
				super();
				// TODO Auto-generated constructor stub
				this.chatPerson = mPerson;
			}

			public void release(){
					try {
						if(!recvSocketServer.isClosed())
							recvSocketServer.close();
							recvSocketServer = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			@Override
			public void run() {
				
				Socket recvSocket = null;
				InputStream mSocketInputStream = null;
				
				if(Constant.DEBUG) Log.e(Constant.TAG,"Audio RecvAudioThread ...");
				
				try{
					recvSocketServer = new ServerSocket(Constant.AudioPORT);
					
					initRecvAudio();
					mAudioTrack.play();//开始播放   
					
					while(!isStopRecvAudioThread && !recvSocketServer.isClosed() && null!=recvSocketServer){
			            
						recvSocket = recvSocketServer.accept();
						recvSocket.setSoTimeout(5000);
						mSocketInputStream = recvSocket.getInputStream();
						
						// 读取录音流，返回读取的字节数
						int hasReadSize = 0;
						byte[] recvBuff = new byte[160];
						while((hasReadSize = mSocketInputStream.read(recvBuff))!= -1){
							if(hasReadSize>0 && hasReadSize%2==0){
								if(Constant.DEBUG) Log.d(Constant.TAG, "接收到的语音数据长度="+hasReadSize);
								mAudioTrack.write(recvBuff, 0, hasReadSize);
								mAudioTrack.flush();
							}
						}

						if(null!=mSocketInputStream){
							mSocketInputStream.close();
							mSocketInputStream = null;
						}
						if(!recvSocket.isClosed()){
							recvSocket.close();
							recvSocket = null;
						}

					}
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					// 无论结果如何都要关闭socket
					if(Constant.DEBUG) Log.e(Constant.TAG, "关闭Audio 接收线程========>") ;
					try {
						if(null != mAudioTrack) {
							mAudioTrack.stop();
							mAudioTrack.release();
							mAudioTrack = null;
						}
						
						if(null!=mSocketInputStream)mSocketInputStream.close();
						if(null!=recvSocket && !recvSocket.isClosed())recvSocket.close();
						if(null!=recvSocketServer && !recvSocketServer.isClosed())recvSocketServer.close();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
			}
		}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
