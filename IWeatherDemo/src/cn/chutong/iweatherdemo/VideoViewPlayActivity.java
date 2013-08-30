package cn.chutong.iweatherdemo;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.MediaController.OnHiddenListener;
import io.vov.vitamio.widget.MediaController.OnShownListener;
import io.vov.vitamio.widget.VideoView;
import io.vov.vitamio.widget.VideoView.OnMediaControlListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class VideoViewPlayActivity extends Activity implements OnClickListener{
	
	private VideoView mVideoView;
	private MediaController mediaController;
	private ProgressDialog progressDialog;
	private GestureDetector detector;
	private Handler widgetHandler;
	
	private final String URL = "http://112.124.20.62:8134/hls-vod/";
	private String url = "http://112.124.20.62:8134/hls-vod/sample.m3u8";
	private boolean hasLocked = false;
	
	private int windownWidth;
	private int windowHeight;
	private float mBrightness;
	private int mVolume;
	private int maxVolume;
	private AudioManager audioManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.videoview_activity);
		initView();
	}
	
	private void initView(){
		
		Intent intent = getIntent();
		if(intent != null){
			if(intent.getStringExtra("URL") != null && intent.getStringExtra("URL").length() != 0){
				StringBuffer sb = new StringBuffer();
				sb.append(URL);
				sb.append(intent.getStringExtra("URL"));
				sb.append(".m3u8");
				url = sb.toString().trim();
				Log.d("Tag", url);
			}
				
		}
		
		if (url == "") {
			Toast.makeText(VideoViewPlayActivity.this, "Please edit VideoViewDemo Activity, and set path" + " variable to your media file URL/path", Toast.LENGTH_LONG).show();
			return;
		} else {
			mVideoView = (VideoView) findViewById(R.id.surface_view);
			mVideoView.setVideoPath(url);
			mediaController = new MediaController(this);
			mVideoView.setMediaController(mediaController);
			mVideoView.requestFocus();
		}
		
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		windownWidth = outMetrics.widthPixels;
		windowHeight = outMetrics.heightPixels;
		
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		progressDialog = new ProgressDialog(this);
		progressDialog.setCanceledOnTouchOutside(false);		
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage("正在加载，请稍等");
		progressDialog.show();
		
		widgetHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				LinearLayout videoview_volume_container = (LinearLayout) findViewById(R.id.videoview_volume_container);
				videoview_volume_container.setVisibility(View.GONE);
			}
		};
		
		setAction();
	}
	
	@SuppressWarnings("deprecation")
	private void setAction(){
		mVideoView.setOnPreparedListener(new PrepareListener());
		mVideoView.setOnMediaControlListener(new MediaControlListener());
		mVideoView.setOnInfoListener(new MyInfoListener());
		mVideoView.setOnBufferingUpdateListener(new MediaBufferUpdateListener());
		mediaController.setOnShownListener(new MyOnshowListener());
		mediaController.setOnHiddenListener(new MyOnhideListener());
		progressDialog.setOnKeyListener(new DialogOnKeyListener());
		
		LinearLayout videoview_download_linear = (LinearLayout) findViewById(R.id.videoview_download_linear);
		videoview_download_linear.setOnClickListener(this);
		LinearLayout videoview_collect_linear = (LinearLayout) findViewById(R.id.videoview_collect_linear);
		videoview_collect_linear.setOnClickListener(this);
		LinearLayout videoview_share_linear = (LinearLayout) findViewById(R.id.videoview_share_linear);
		videoview_share_linear.setOnClickListener(this);
		ImageView videoview_actionbar_back_btn = (ImageView) findViewById(R.id.videoview_actionbar_back_btn);
		videoview_actionbar_back_btn.setOnClickListener(this);
		
		detector = new GestureDetector(new MyGestureDetector());
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.videoview_download_linear:
			Toast.makeText(this, "下载", Toast.LENGTH_SHORT).show();
			break;

		case R.id.videoview_collect_linear:
			Toast.makeText(this, "收藏", Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.videoview_share_linear:
			Toast.makeText(this, "分享", Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.videoview_actionbar_back_btn:
			finish();
			break;
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			resetTime();
			return super.onTouchEvent(event);
		}
		return detector.onTouchEvent(event);
	}
	
	private class PrepareListener implements MediaPlayer.OnPreparedListener{

		@Override
		public void onPrepared(MediaPlayer mp) {
			mp.setPlaybackSpeed(1.0f);
		}
		
	}
	
	private class MyInfoListener implements OnInfoListener{

		@Override
		public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
			switch (arg1) {
			case MediaPlayer.MEDIA_INFO_BUFFERING_START:
				progressDialog.setMessage("正在缓冲：0%");
				progressDialog.show();
				arg0.stop();
				break;

			case MediaPlayer.MEDIA_INFO_BUFFERING_END:
				arg0.start();
				progressDialog.cancel();
				break;
				
			case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
				break;
			}
			
			return true;
		}
	}
	
	private class MediaBufferUpdateListener implements OnBufferingUpdateListener{

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			progressDialog.setMessage("正在缓冲：" + percent + "%");
		}
		
	}
	
	private class DialogOnKeyListener implements OnKeyListener{

		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if(keyCode == KeyEvent.KEYCODE_BACK){
				finish();
				return true;
			}
			return false;
		}
		
	}
	
	private class MyOnshowListener implements OnShownListener{

		@Override
		public void onShown() {
			if(!hasLocked)
				open();
		}
		
	}
	
	private class MyOnhideListener implements OnHiddenListener{

		@Override
		public void onHidden() {
			if(!hasLocked)
				hide();
		}

	}	
	
	/**
	 * lockScreen是锁屏回调函数，videoQuality是切换视频质量的回调函数
	 * @author wangyang
	 *
	 */
	private class MediaControlListener implements OnMediaControlListener{

		@Override
		public void lockScreen(boolean islocked) {
			hasLocked = islocked;
			if(islocked){
				hide();
			}else{
				open();
			}
		}

		@Override
		public void videoQuality(TextView textView) {
			
		}
		
	}
	
	private class MyGestureDetector implements OnGestureListener{
		
		private boolean inLeft = false;
		@Override
		public boolean onDown(MotionEvent e) {
			
			int x = (int) e.getX();
			if(x <= windownWidth/2){
				inLeft = true;
			}else{
				inLeft = false;
			}
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			float mOldY = e1.getY();
			int y = (int) e2.getRawY();
            if (!inLeft)// 右边滑动
            	setStreamVolume((mOldY - y) / windowHeight);
            else
            	setBrightnessSlide((mOldY - y) / windowHeight);
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}
		
	}
	
	private void hide(){
		LinearLayout videoview_function_linear = (LinearLayout) findViewById(R.id.videoview_function_linear);
		videoview_function_linear.setVisibility(View.GONE);
		RelativeLayout videoview_actionbar_rel = (RelativeLayout) findViewById(R.id.videoview_actionbar_rel);
		videoview_actionbar_rel.setVisibility(View.GONE);
	}
	
	private void open(){
		LinearLayout videoview_function_linear = (LinearLayout) findViewById(R.id.videoview_function_linear);
		videoview_function_linear.setVisibility(View.VISIBLE);
		RelativeLayout videoview_actionbar_rel = (RelativeLayout) findViewById(R.id.videoview_actionbar_rel);
		videoview_actionbar_rel.setVisibility(View.VISIBLE);
	}
	
	private void setStreamVolume(float changePercent){
		mVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (mVolume < 0)
            mVolume = 0;
		
		int afterVolume = mVolume + (int)(changePercent*maxVolume);
		if(afterVolume > maxVolume){
			afterVolume = maxVolume;
		}
		if(afterVolume < 0){
			afterVolume = 0;
		}
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, afterVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
		LinearLayout videoview_volume_container = (LinearLayout) findViewById(R.id.videoview_volume_container);
		videoview_volume_container.setVisibility(View.VISIBLE);
		TextView videoview_volume_percent = (TextView) findViewById(R.id.videoview_volume_percent);
		videoview_volume_percent.setText((int)afterVolume*100/maxVolume + "%");
	}
	
	/**
     * 滑动改变亮度
     * 
     * @param percent
     */
    private void setBrightnessSlide(float percent) {
		mBrightness = getWindow().getAttributes().screenBrightness;
        if (mBrightness <= 0.00f)
            mBrightness = 0.50f;
        if (mBrightness < 0.01f)
            mBrightness = 0.01f;

        WindowManager.LayoutParams lpa = getWindow().getAttributes();
        lpa.screenBrightness = mBrightness + percent;
        if (lpa.screenBrightness > 1.0f)
            lpa.screenBrightness = 1.0f;
        else if (lpa.screenBrightness < 0.01f)
            lpa.screenBrightness = 0.01f;
        getWindow().setAttributes(lpa);

    }
    
    @SuppressLint("UseValueOf")
	private byte[] longTobyte(long lon){
    	byte buff[] = new byte[8];
    	for(int i = 0; i < buff.length; i++){
    		buff[i] = Long.valueOf(lon & 0xff).byteValue();
    		lon = lon >> 8;
    	}
    	return buff;
    }
    
	private void resetTime() {
		widgetHandler.removeMessages(1);
		Message msg = widgetHandler.obtainMessage(1);
		widgetHandler.sendMessageDelayed(msg, 2000);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(hasLocked){
				Toast.makeText(this, "屏幕已锁定", Toast.LENGTH_SHORT).show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
}
