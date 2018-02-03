package com.prog.browserparser;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private Button mBtnGo,mBtnBack,mBtnForward;
    private EditText mTextUrl;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==1)
        {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                webView.loadUrl("https://www.google.com.ua");
            } else {
                finish();
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(PackageManager.PERMISSION_GRANTED== ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){

        }else{
            ActivityCompat.requestPermissions((Activity)this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
        mBtnGo=(Button)findViewById(R.id.btnGo);
        mBtnBack=(Button)findViewById(R.id.btnBack);
        mBtnForward=(Button)findViewById(R.id.btnForw);
        mTextUrl=(EditText)findViewById(R.id.goUrl);
        hideFocus(mTextUrl);
        mTextUrl.setSelectAllOnFocus(true);

        webView = (WebView) findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);


        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressBar.setMax(100);
        progressBar.setProgress(1);
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
            }
        });
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, final String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);

                mTextUrl.setText(url);
                final Handler handler = new Handler();
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        writeFile(url);
                        handler.post(new Runnable()
                        {
                            public void run()
                            {
                            }
                        });
                    }
                };
                Thread t = new Thread(r);
                t.start();
                Log.e("URL",url);
            }



            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, final String url2) {
                final Handler handler = new Handler();
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        getLinks(url2);
                        handler.post(new Runnable()
                        {
                            public void run()
                            {
                            }
                        });
                    }
                };
                Thread t = new Thread(r);
                t.start();
                progressBar.setVisibility(View.GONE);
            }
        });

        mBtnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideFocus(mTextUrl);
                final String[] validUrl = new String[1];
                final Handler handler = new Handler();
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        validUrl[0] =getValidateUrl(mTextUrl.getText().toString());
                        handler.post(new Runnable()
                        {
                            public void run()
                            {
                                String url=validUrl[0];
                                webView.loadUrl(url);
                            }
                        });
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (webView.canGoBack())
                    //webView.goBack();
            }
        });
        mBtnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (webView.canGoForward())
                    webView.goForward();
            }
        });
        mTextUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideFocus(mTextUrl);
                    final String[] validUrl = new String[1];
                    final Handler handler = new Handler();
                    Runnable r = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            validUrl[0] =getValidateUrl(mTextUrl.getText().toString());
                            handler.post(new Runnable()
                            {
                                public void run()
                                {
                                    String url=validUrl[0];
                                    webView.loadUrl(url);
                                }
                            });
                        }
                    };
                    Thread t = new Thread(r);
                    t.start();
                }
                return handled;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()){
            webView.goBack();
        }else super.onBackPressed();
    }

    public String getValidateUrl(String url){
        if (!url.startsWith("http://")&&!url.startsWith("https://")) {
            try {
                Jsoup.connect("http://" + url).get();
                return "http://" + url;
            } catch (final IOException e) {
                try {
                    Jsoup.connect("https://" + url).get();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return "https://" + url;
            }
        }
        return url;
    }

    public void getLinks(String url){
        Document doc = null;
        Elements links=null;
        try {
            doc = Jsoup.connect(url).get();
            links= doc.select("a");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (links!=null){
            for (Element link : links) {
                Log.e("URL from page ",link.attr("href"));
                writeFile(link.attr("href"));
            }
        }else{
            Log.e("URL from page ","Nothing URLs");
        }


    }
    public void writeFile (String data)
    {
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
        String date = df.format(Calendar.getInstance().getTime());
        data=data+" "+date;

        File file = null;
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        file = new File(baseDir, "browserURLs.txt");

        BufferedWriter bw = null;
        FileWriter fw = null;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(data);
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException ioe2) {
            }
        }
    }
    public void hideFocus(EditText editText){
        editText.clearFocus();
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(
                editText.getWindowToken(), 0);
    }

}
