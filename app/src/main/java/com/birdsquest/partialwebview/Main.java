package com.birdsquest.partialwebview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class Main extends AppCompatActivity{
	PartialWebView article;
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		article=(PartialWebView)findViewById(R.id.article);

		(findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			article.loadPage("http://www3.nhk.or.jp/news/easy/k10010888361000/k10010888361000.html","newsarticle");
			}
		});
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.e("Main","SaveInstance");
		article.save(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.e("Main","RestoreInstance");
		article.load(savedInstanceState);
	}
}
