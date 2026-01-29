package com.example.internetspeedtest.pages

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebViewScreen(modifier: Modifier = Modifier) {
  AndroidView(
    factory = { context ->
      WebView(context).apply {
        webViewClient = WebViewClient()

        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          loadWithOverviewMode = true
          useWideViewPort = true
        }

        loadUrl("https://www.youtube.com/")
      }
    },
    modifier = modifier.fillMaxSize()
  )
}

@Composable
@Preview(showBackground = true, widthDp = 400, heightDp = 600)
fun WebViewPreview() {
  WebViewScreen()
}