package com.myappcompany.steve.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Intent intent = getIntent();
        final int id = intent.getIntExtra("id", 0);
        final String source = intent.getStringExtra("source");

        Cursor c;


        if(source.equals("top")) {
            //MainActivity.myDatabase.execSQL("CREATE TABLE IF NOT EXISTS topStories (id INTEGER, title VARCHAR, url VARCHAR)");
            c = MainActivity.myDatabase.rawQuery("SELECT * FROM topStories WHERE id = " + id, null);
        } else {
            c = MainActivity.favDatabase.rawQuery("SELECT * FROM favStories WHERE id = " + id, null);
        }


        int urlIndex = c.getColumnIndex("url");
        String url = "";

        c.moveToFirst();
        if (!c.isAfterLast()) {
            url = c.getString(urlIndex);
        }

        c.close();

        Log.i("URL from DB: ", url);

        WebView webView = findViewById(R.id.webView);

        //Java Script not enabled by default
        webView.getSettings().setJavaScriptEnabled(true);

        //otherwise it will open the default browser on the phone instead of opening it in the app
        webView.setWebViewClient(new WebViewClient());

        //loads the website
        webView.loadUrl(url);
    }

}
