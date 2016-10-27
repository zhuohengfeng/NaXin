package org.ryan.naxin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SendVideo extends Activity {
	
	private Person chatPerson = null;
	private Person myPerson  = null;
	
	private int startVideoCmdType = 0x0;
	
	private TextView mStateText = null;
	private Button mHangupBn = null;
	private SurfaceView mSurfaceViewBack = null;
	private SurfaceView mSurfaceViewFront = null;
	private SurfaceHolder mSurfaceHolderBack = null;
	private SurfaceHolder mSurfaceHolderFront = null;
	private LinearLayout mTitleLinearLayout = null;
	private LinearLayout mBottomLinearLayout = null;
	
	private Camera mCamera = null;
	private Camera.Size csize = null;  
	
	private int mSurfaceFrontWidth = 0;
	private int mSurfaceFrontHeight = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.sendvideo);
		
		startVideoCmdType = (int)(getIntent().getExtras().getInt(Constant.videoCmdStr));
		myPerson = (Person)(getIntent().getExtras().getSerializable(Constant.myInfoStr));
		chatPerson = (Person)(getIntent().getExtras().getSerializable(Constant.userNameStr));

		// ----------------获取控件--------------------------------
		mTitleLinearLayout = (LinearLayout)findViewById(R.id.video_title);
		mBottomLinearLayout = (LinearLayout)findViewById(R.id.video_bottom);
		mHangupBn = (Button)findViewById(R.id.video_handup);
		
		mSurfaceViewBack = (SurfaceView)findViewById(R.id.surfaceView_back);
		mSurfaceHolderBack = mSurfaceViewBack.getHolder();
		mSurfaceHolderBack.addCallback(mSurfaceCallback);
		mSurfaceHolderBack .setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	        
		mSurfaceViewFront = (SurfaceView)findViewById(R.id.surfaceView_front);
		mSurfaceHolderFront = mSurfaceViewFront.getHolder();
		mSurfaceHolderFront.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceViewFront.setZOrderOnTop(true);
		
		mStateText = (TextView)findViewById(R.id.video_state);
		if(startVideoCmdType == 0x01){
			mStateText.setText("请求与"+chatPerson.getName()+"连接中...");
			mHangupBn.setEnabled(false);
		}
		else{
			mStateText.setText("已与"+chatPerson.getName()+"连接进行视频通话");
			if(mMainService != null){
				mHangupBn.setEnabled(true);
			}
		}
		
		// ------------------------------------------------------
		mHangupBn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				stopSendVideo();
				stopRecvVideo();
				mMainService.disconnectVideo(chatPerson);
				SendVideo.this.finish();
			}
		});
		
		
		// --------------------绑定服务-----------------------------
		mServiceIntent = new Intent(SendVideo.this, MainService.class);
		this.bindService(mServiceIntent, mServiceConn, BIND_AUTO_CREATE);
		
		// ------------------接收广播-------------------------------
		IntentFilter mVideoFilter = new IntentFilter();
		mVideoFilter.addAction(Constant.BR_RejectVideoConnect);
		mVideoFilter.addAction(Constant.BR_AgreeVideoConnect);
		mVideoFilter.addAction(Constant.BR_DisconnectVideo);
		this.registerReceiver(mVideoReceiver, mVideoFilter);
		
		// ----------------------------------
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(mVideoReceiver);
		this.unbindService(mServiceConn);
		mServiceConn = null;
		if(mMainService != null){
			stopSendVideo();
			stopRecvVideo();
			mMainService = null;
		}
		      
	    if(null != mCamera)
	    {
			mCamera.setPreviewCallback(null); 
			mCamera.stopPreview(); 
			mCamera.release();
			mCamera = null;   
	    }
		
		super.onDestroy();
	}

	
	private boolean isAnimation = true;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if(isAnimation){
				isAnimation = false;
				startAnim(true);
			}
			else{
				isAnimation = true;
				startAnim(false);
			}
		}
		
		return super.onTouchEvent(event);
	}


	private void startAnim (boolean isStart) {  
		if(isStart){
			AnimationSet animup = new AnimationSet(true);
			AnimationSet animdn = new AnimationSet(true);
			TranslateAnimation mTranslateTitle = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,-1.0f);
			TranslateAnimation mTranslateBottom = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF, 1.0f);
			
			animup.setFillEnabled(true); //启动Fill保持   
			animup.setFillAfter(true);  //设置动画的最后一帧是保持在View上面   
			animdn.setFillEnabled(true); //启动Fill保持   
			animdn.setFillAfter(true);  //设置动画的最后一帧是保持在View上面   
			
			mTranslateTitle.setDuration(800);
			mTranslateBottom.setDuration(800);
			animup.addAnimation(mTranslateTitle);
			animdn.addAnimation(mTranslateBottom);
			mTitleLinearLayout.startAnimation(animup);
			mBottomLinearLayout.startAnimation(animdn);
			
		}
		else{
			AnimationSet animup = new AnimationSet(true);
			AnimationSet animdn = new AnimationSet(true);
			TranslateAnimation mTranslateTitle = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0f);
			TranslateAnimation mTranslateBottom = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,1.0f,Animation.RELATIVE_TO_SELF, 0f);
			
			animup.setFillEnabled(true); //启动Fill保持   
			animup.setFillAfter(true);  //设置动画的最后一帧是保持在View上面   
			animdn.setFillEnabled(true); //启动Fill保持   
			animdn.setFillAfter(true);  //设置动画的最后一帧是保持在View上面   
			
			mTranslateTitle.setDuration(800);
			mTranslateBottom.setDuration(800);
			animup.addAnimation(mTranslateTitle);
			animdn.addAnimation(mTranslateBottom);
			mTitleLinearLayout.startAnimation(animup);
			mBottomLinearLayout.startAnimation(animdn);
		}
	}

	//---------------------------------------------------------
	// 启动服务的Intent
	private Intent mServiceIntent = null;
	private MainService mMainService = null;
	
	private ServiceConnection mServiceConn = new ServiceConnection(){
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			// 这里的IBinder是本地的，所以可以直接使用
			if(Constant.DEBUG) Log.i(Constant.TAG,"SendVideo: the Service is connedted!");
			mMainService = ((MainService.MyBinder)service).getServiceInstance();
			
			// 发送语音通话的请求
			if(startVideoCmdType == 0x01){
				mMainService.sendVideoRequest(myPerson.getUID(), chatPerson.getPersonHost());
			}
			else{
				if(!mHangupBn.isEnabled()){
					mHangupBn.setEnabled(true);	
				}
			}
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			if(Constant.DEBUG) Log.i(Constant.TAG,"SendAudio: the Service is Disconnedted!");
			
		}
	};
	
	
	//-------------广播接收器-------------------
	private BroadcastReceiver mVideoReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(Constant.BR_RejectVideoConnect)){
				Toast.makeText(SendVideo.this, "对方拒绝进行视频通话！", Toast.LENGTH_LONG).show();
				stopSendVideo();
				stopRecvVideo();
				SendVideo.this.finish();
			}
			else if(action.equals(Constant.BR_AgreeVideoConnect)){
				Toast.makeText(SendVideo.this, "对方同意进行视频通话！", Toast.LENGTH_SHORT).show();
				mStateText.setText("已与"+chatPerson.getName()+"连接进行语音通话");
				if(mMainService != null){
					mHangupBn.setEnabled(true);	
				}
			}
			else if(action.equals(Constant.BR_DisconnectVideo)){
				Toast.makeText(SendVideo.this, "对方断开了视频通话！", Toast.LENGTH_SHORT).show();
				stopSendVideo();
				stopRecvVideo();
				SendVideo.this.finish();
			}
		}
	};
	
	
    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
		
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			
	        mCamera = Camera.open();
	        if(mCamera == null){
	        	if(Constant.DEBUG) Log.e(Constant.TAG, "open the camera failed!!");
	        }
	        else{
	        	try {
					mCamera.setPreviewDisplay(mSurfaceHolderBack);
					
				     /* Camera Service settings*/    
				     Camera.Parameters parameters = mCamera.getParameters();
				     // parameters.setFlashMode("off"); 
				     parameters.setPictureFormat(PixelFormat.JPEG);     
				     parameters.setPreviewFormat(/*PixelFormat.YCbCr_420_SP*/ImageFormat.NV21); 

				     parameters.setPictureSize(176, 144); 
				     parameters.setPreviewSize(176, 144); 

				     if (SendVideo.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) 
				     {
				    	 parameters.set("orientation", "portrait"); //
				      	 parameters.set("rotation", 90); 
				      	 mCamera.setDisplayOrientation(90); 
				     } 
				     else{
				    	 parameters.set("orientation", "landscape"); //
				    	 mCamera.setDisplayOrientation(0); 
				     } 
			
				     mCamera.setPreviewCallback(mPreviewCallback);
				     
				     mCamera.setParameters(parameters);    
				     mCamera.startPreview(); 
				     
				     
				     csize = mCamera.getParameters().getPreviewSize();
				     if(Constant.DEBUG) Log.d(Constant.TAG+"initCamera", "after setting, previewSize:width: " + csize.width + " height: " + csize.height);
				     if(Constant.DEBUG) Log.d(Constant.TAG+"initCamera", "after setting, previewformate is " + mCamera.getParameters().getPreviewFormat());
				     if(Constant.DEBUG) Log.d(Constant.TAG+"initCamera", "after setting, previewframetate is " + mCamera.getParameters().getPreviewFrameRate());
			    
					 // 本方同意开始接收视频，启动本地接受线程
					 startRecvVideo(chatPerson);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
				    if(null != mCamera)
				    {
				    	mCamera.setPreviewCallback(null); 
						mCamera.stopPreview(); 
				    	mCamera.release();
				    	mCamera = null;     
				    }
					e.printStackTrace();
				}
	        }
		}
		
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			if(Constant.DEBUG) Log.i(Constant.TAG, "SurfaceHolder.Callback￡oSurface Destroyed");
			if(null != mCamera)
			{
				mCamera.setPreviewCallback(null); 
				mCamera.stopPreview(); 
				mCamera.release();
				mCamera = null;     
			}
		}
		
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO Auto-generated method stub
			
		}
	};
    
    
	private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
		
		public void onPreviewFrame(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			try{
				YuvImage image = new YuvImage(data, ImageFormat.NV21, csize.width, csize.height, null);
				if(image != null){
					if(mSurfaceFrontWidth == 0 && mSurfaceFrontHeight == 0){
						mSurfaceFrontWidth = mSurfaceViewFront.getWidth();
						mSurfaceFrontHeight = mSurfaceViewFront.getHeight();
					}
					
				    ByteArrayOutputStream stream = new ByteArrayOutputStream();
				    image.compressToJpeg(new Rect(0, 0, csize.width, csize.height), 80, stream);
				    stream.flush();
				    //启用线程将图像数据发送出去
				    sendVideoImg(chatPerson, stream);

				}
			}catch(Exception ex){
			    if(Constant.DEBUG) Log.e(Constant.TAG,"Error:"+ex.getMessage());
			}

		}
	};
	
	
	
	//---------------------------------------------------------------------
	private boolean isStopSendVideoThread = false;
	private boolean isStopRecvVideoThread = false;
	private RecvVideoThread mRecvVideoThread = null;
	// 开始接收视频
	public void startRecvVideo(final Person chatPerson){
		isStopRecvVideoThread = false;
		mRecvVideoThread = new RecvVideoThread(chatPerson);
		mRecvVideoThread.start();
	}

	// 发送视频
	public void sendVideoImg(final Person chatPerson, final ByteArrayOutputStream outStream){
    	isStopSendVideoThread = false;
		new SendVideoThread(chatPerson, outStream).start();
	}
	
	// 停止发送视频
	public void stopSendVideo(){
		isStopSendVideoThread = true;
	}
	
	// 停止接收送视频
	public void stopRecvVideo(){
		isStopRecvVideoThread = true;
		if(mRecvVideoThread!=null){
			mRecvVideoThread.release();
			mRecvVideoThread = null;
		}
	}
	
	
	
	// ==========发送/接收视频采用TCP协议========================
	// 发送视频线程
	private class SendVideoThread extends Thread{
		private Person chatPerson = null;
		private ByteArrayOutputStream mOutStream = null;
		
		public SendVideoThread(Person mPerson, ByteArrayOutputStream outStream) {
			super();
			// TODO Auto-generated constructor stub
			this.chatPerson = mPerson;
			this.mOutStream = outStream;

			// 重点：先关闭流！！！！！
	        try {
	        	outStream.close();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
			// 创建客户端连接服务器的client
			InetAddress sendAddress = null;
			Socket sendSocket = null;
			OutputStream socketStream = null;
			ByteArrayInputStream inputstream = null; 
            byte[] readbuffer = new byte[Constant.bufferSize];  
            
			try {
				// 创建Socket
				sendAddress = InetAddress.getByName(chatPerson.getPersonHost());
				sendSocket = new Socket(sendAddress, Constant.PORT);
				
				if(sendSocket!= null && sendSocket.isConnected()){
					if(Constant.DEBUG) Log.d(Constant.TAG, "video服务端：已经连接上===========>");
					
					socketStream = sendSocket.getOutputStream(); // socket流
					inputstream = new ByteArrayInputStream(mOutStream.toByteArray()); // 要发送的数据流
					
					int hasRead = 0;
					while((hasRead=inputstream.read(readbuffer)) != -1){
						if(Constant.DEBUG) Log.e(Constant.TAG,"Video send the stream read="+hasRead);
						socketStream.write(readbuffer,0,hasRead);
					}
					socketStream.flush();
					
				}
				else{
					if(Constant.DEBUG) Log.e(Constant.TAG, "video客户端：没有连接上=====>");
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				// 无论结果如何都要关闭socket
				try {
					if(null!=inputstream)inputstream.close();
					if(null!= socketStream)socketStream.close();
					if(null!= sendSocket && !sendSocket.isClosed())sendSocket.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	
	
	// 接收语音的线程
	private class RecvVideoThread extends Thread{
		private Person chatPerson = null;
		private ServerSocket recvSocketServer = null;
		
		public RecvVideoThread(Person mPerson) {
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
			int hasReadSize = 0;
			int readbyte = 0;
			byte[] recvBuff = new byte[Constant.bufferSize];
			byte[] ImgBuff = new byte[Constant.bufferSize*8];
			Socket recvSocket = null;
			InputStream mSocketInputStream = null;
			
			if(Constant.DEBUG) Log.e(Constant.TAG,"Video RecvAudioThread ...");
            
			try{
				
				recvSocketServer = new ServerSocket(Constant.PORT);
				
				while(!isStopRecvVideoThread && !recvSocketServer.isClosed() && null!=recvSocketServer){
					recvSocket = recvSocketServer.accept();
					recvSocket.setSoTimeout(5000);
					
					if(Constant.DEBUG) Log.e(Constant.TAG,"the Recv Video start.....");
					
					mSocketInputStream = recvSocket.getInputStream();
					
					// 读取视频流，返回读取的字节数
					hasReadSize = 0;
					readbyte = 0;
					while((hasReadSize = mSocketInputStream.read(recvBuff)) != -1){
						System.arraycopy(recvBuff, 0, ImgBuff, readbyte, hasReadSize);
						readbyte += hasReadSize;
					}
					
					Bitmap bmprecv = BitmapFactory.decodeByteArray(ImgBuff, 0, readbyte);
					
					if(Constant.DEBUG) Log.d(Constant.TAG, "一共读到字节数：readbyte = "+readbyte);
						
					Canvas mCanvas = mSurfaceHolderFront.lockCanvas();
					if(mCanvas != null){
						if(bmprecv != null){
							mCanvas.drawBitmap(bmprecv, 0,0,null);
						}
						else{
							Paint mPaint = new Paint();
							mPaint.setColor(Color.RED);
							mCanvas.drawText("无图像！", 10, 20, 120, 120, mPaint);
						}
					}
					mSurfaceHolderFront.unlockCanvasAndPost(mCanvas);
					
					if (mSocketInputStream != null) {
						mSocketInputStream.close();
						mSocketInputStream = null;
					}
					
					if (recvSocket != null && !recvSocket.isClosed()) {
						recvSocket.close();
						recvSocket = null;
					}
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				// 无论结果如何都要关闭socket
				try {
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
