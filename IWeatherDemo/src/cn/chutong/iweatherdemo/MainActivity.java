package cn.chutong.iweatherdemo;

import io.vov.vitamio.LibsChecker;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!LibsChecker.checkVitamioLibs(this))
			return;
		
		setContentView(R.layout.activity_main);
		setAction();
	}

	private void setAction(){
		Button button = (Button) findViewById(R.id.main_btn_play);
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				EditText editText = (EditText) findViewById(R.id.main_ed_url);
				Editable editable = editText.getText();
				
				String url = editable.toString();
				Intent intent = new Intent();
				intent.putExtra("URL", url);
				intent.setClass(MainActivity.this, VideoViewPlayActivity.class);
				startActivity(intent);
			}
		});
	}
}
