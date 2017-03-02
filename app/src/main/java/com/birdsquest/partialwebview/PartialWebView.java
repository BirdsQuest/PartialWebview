/** Requirements~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
AndroidManifest.xml~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
<uses-permission android:name="android.permission.INTERNET"></uses-permission>
To Call A Page~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
article.loadPage("http://www3.nhk.or.jp/news/easy/k10010888361000/k10010888361000.html","newsarticle");
Main.java~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
PartialWebView article;
@Override protected void onCreate(Bundle savedInstanceState){
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	article=(PartialWebView)findViewById(R.id.article);
}
 @Override protected void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
	Log.e("Main","SaveInstance");
	article.save(outState);
}

@Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
	super.onRestoreInstanceState(savedInstanceState);
	Log.e("Main","RestoreInstance");
	article.load(savedInstanceState);
}
*/

package com.birdsquest.partialwebview;

import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.app.Activity;
import android.webkit.WebView;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;

public class PartialWebView extends WebView{
	Context context;
	WebView preloader;
	boolean loading=false;
	boolean showFurigana=true;
	String tag, url, contents,
		selectElementScript="function selectElement(e){"+
			"var range=document.createRange();"+
			"range.selectNodeContents(e);"+
			"var selected=window.getSelection();"+
			"selected.removeAllRanges();"+
			"selected.addRange(range);"+
		"}",
		cleanArticleScript="var article=document.getElementById('%1$s').cloneNode(true);"+
			"var p=article.getElementsByTagName('p'), children, spanClass;"+
			"for(var index=0;index<p.length;index++){"+
				"children=p[index].childNodes;"+
				"for(var innerdex=0;innerdex<children.length;innerdex++){"+
					"if(children[innerdex].tagName=='A'){"+
						"spanClass=children[innerdex].firstChild.firstChild;"+
						"spanClass.outerHTML=spanClass.innerHTML;"+
						"children[innerdex].outerHTML=children[innerdex].innerHTML;"+
					"}else if(children[innerdex].tagName=='SPAN'){"+
						"children[innerdex].outerHTML=children[innerdex].innerHTML;"+
					"}else if(children[innerdex].tagName!='RUBY'){"+
						"spanClass=document.createElement('span');"+
						"spanClass.innerHTML = children[innerdex].textContent;"+
						"children[innerdex].nodeValue.replace(/children[innerdex].textContent/, \"<span>children[innerdex].textContent</span>\");;"+
			"}  }   }",
		isolateTextOnClickScript="document.getElementById('%1$s').onclick=function(e){"+
				"e=e.target;"+
				"console.log(e.nodeType);"+
				"if(e.tagName=='SPAN'){console.log(e.innerText);}"+//Android.selected(e.textContent);
				"if(e.tagName=='RT'){e=e.parentNode.childNodes[0];selectElement(e);Android.selected(e.textContent);}"+
				"else if(e.tagName=='RUBY'){e=e.childNodes[0];selectElement(e);Android.selected(e.textContent);}"+
			"};",
		loadingBarScript="<style>"+
				"#BarContainer{width:100%;height:100px;background:grey;position:relative;}"+
				"#PercentComplete{width:0%;height:100px;background:green;position:absolute;left:0px;top:0px;}"+
			"</style>"+
			"<div id='BarContainer'>"+
				"<div id='PercentComplete'></div>"+
			"</div>"+
			"<center>"+
				"<div id='LoadingText'>ニュースはロード中します。。。</div>"+
			"</center>"+
			"<script>"+
				"function setPercent(value){"+
					"document.getElementById('PercentComplete').style.width=value+'%';"+
					"if(value==100){document.getElementById('LoadingText').innerHTML='ニュースはロードしました！'}"+
				"}"+
			"</script>",
		articleStyle="<style>%2$s"+
			"#%1$s{color:DARKGREY;}"+
			"#%1$s ruby{color:BLACK;}"+
			"</style>",
		articleScript=
			"<script>"+
				selectElementScript+
				isolateTextOnClickScript+
			"</script>";

	public PartialWebView(Context context){super(context);init(context, null);}
	public PartialWebView(Context context, AttributeSet attrs){super(context, attrs);init(context, attrs);}
	public void init(Context context, AttributeSet attrs){
		this.context=context;
		getSettings().setJavaScriptEnabled(true);
		addJavascriptInterface(new JSInterface(), "Android");

		preloader=new WebView(context);
		preloader.getSettings().setJavaScriptEnabled(true);
		preloader.setWebViewClient(onPreloaded);
		preloader.addJavascriptInterface(new JSInterface(), "Android");
		setWebChromeClient(consoleWriter);
		preloader.setWebChromeClient(consoleWriter);

		getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		requestFocus(View.FOCUS_DOWN);
	}

	protected void save(Bundle outState) {
		outState.putString("tag",tag);
		outState.putString("url",url);
		outState.putString("contents",contents);
		outState.putBoolean("showFurigana",showFurigana);
	}

	protected void load(Bundle savedInstanceState) {
		url=savedInstanceState.getString("url",null);
		tag=savedInstanceState.getString("tag",null);
		contents=savedInstanceState.getString("contents",null);
		showFurigana=savedInstanceState.getBoolean("showFurigana",true);
		loadPage(url,tag);
	}

	//Page Loading--------------------------------------------------------------------------
	public void loadPage(String url, String tag){
		if(contents==null&&this.url!=url){
			this.tag=tag;
			this.url=url;
			setLoadingScreen();
			preloader.loadUrl(url);
		}else{
			loadContents(contents);}
	}

	private void setLoadingScreen(){
		loading=true;
		loadData(loadingBarScript, "text/html; charset=utf-8", null);
	}

	WebViewClient onPreloaded=new WebViewClient(){
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
			Toast.makeText(context, "Error! "+description, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onPageFinished(WebView view, String url){
			if(loading){
				loadUrl("about:blank");
				runJS("(function(){"+
						selectElementScript+
						String.format(cleanArticleScript,  tag)+
						"console.log(article.outerHTML);"+
						"Android.passOn(article.outerHTML);"+
						"})()",view);
				loading=false;
				view.setWebViewClient(new WebViewClient());
				view.loadUrl("about:blank");
			}else{view.setWebViewClient(onPreloaded);}
		}	};

	public void loadContents(String div){
		if(div!=null){
			Log.e("Oooga Booga",String.format(articleStyle+contents+articleScript, tag, (showFurigana?"":"rt{display:none;}")));
			contents=div;
			loadData(String.format(articleStyle+contents+articleScript, tag, (showFurigana?"":"rt{display:none;}")),
					"text/html; charset=utf-8", null);
	}   }

	//JS Functions-------------------------------------------------------------------------------------------
	public void runJS(String script){runJS(script,this);}
	public void runJS(String script, WebView view){
		if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.KITKAT){
			view.evaluateJavascript(script, null);
		}else{
			view.loadUrl("javascript:"+script);
	}	}

	public class JSInterface{
		JSInterface(){}

		@JavascriptInterface
		public void selected(String text){
			Toast.makeText(context, text, Toast.LENGTH_LONG).show();
		}

		@JavascriptInterface
		public void passOn(final String div){
			((Activity)context).runOnUiThread(new Runnable(){
				@Override
				public void run(){
					loadContents(div);
	}	});	}	}

	WebChromeClient consoleWriter=new WebChromeClient(){
		public void onProgressChanged(WebView view, int progress){
			if(loading){
				Log.e("This is","a test: "+progress);
				runJS("(function(){setPercent("+progress+")})();");
			}
		}

		public boolean onConsoleMessage(ConsoleMessage cm){
			Log.e("WebView", cm.message()+" (line:"+cm.lineNumber()+")");
			return true;
		}
	};
}