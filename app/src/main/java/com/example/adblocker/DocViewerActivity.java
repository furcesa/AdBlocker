package com.example.adblocker;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class DocViewerActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_RAW_ID = "raw_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_viewer);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        int rawId = getIntent().getIntExtra(EXTRA_RAW_ID, 0);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(title == null ? "文档" : title);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        WebView webView = findViewById(R.id.web_view);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.getSettings().setJavaScriptEnabled(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(0x00000000);

        if (rawId != 0) {
            try (InputStream is = getResources().openRawResource(rawId)) {
                byte[] buf = new byte[is.available()];
                int read = is.read(buf);
                String html = new String(buf, 0, read, "UTF-8");
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            } catch (Exception e) {
                webView.loadData("<h2>文档加载失败</h2>", "text/html", "UTF-8");
            }
        }
    }
}
