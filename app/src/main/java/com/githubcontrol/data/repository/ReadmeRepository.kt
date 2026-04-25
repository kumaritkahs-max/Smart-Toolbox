package com.githubcontrol.data.repository

import com.githubcontrol.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a repository README using GitHub's own HTML rendering endpoint
 * (`/repos/{owner}/{repo}/readme` with `Accept: application/vnd.github.html+json`).
 * The result is identical to what github.com displays — including images,
 * tables, task lists, mentions, and code blocks.
 *
 * Returned HTML is wrapped in a self-contained document that bundles the
 * official `github-markdown.css` styling for true visual parity.
 */
@Singleton
class ReadmeRepository @Inject constructor(private val client: RetrofitClient) {

    suspend fun renderedReadmeHtml(owner: String, repo: String, ref: String? = null): String =
        withContext(Dispatchers.IO) {
            val url = StringBuilder("https://api.github.com/repos/$owner/$repo/readme")
            if (!ref.isNullOrBlank()) url.append("?ref=$ref")
            val req = Request.Builder()
                .url(url.toString())
                .header("Accept", "application/vnd.github.html+json")
                .build()
            val body = client.rawClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("README request failed: ${resp.code}")
                resp.body?.string().orEmpty()
            }
            wrap(owner, repo, ref ?: "HEAD", body)
        }

    /** Wrap GitHub's HTML fragment with github-markdown.css for pixel-perfect parity. */
    private fun wrap(owner: String, repo: String, ref: String, html: String): String = """
<!doctype html>
<html lang="en"><head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"/>
<base href="https://github.com/$owner/$repo/raw/$ref/"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/5.5.1/github-markdown.min.css"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.10.0/styles/github.min.css" media="(prefers-color-scheme: light)"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.10.0/styles/github-dark.min.css" media="(prefers-color-scheme: dark)"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.10.0/highlight.min.js"></script>
<style>
  :root { color-scheme: light dark; }
  body { margin:0; padding:24px 16px; background: var(--bgColor-default, #fff); }
  @media (prefers-color-scheme: dark) {
    body { background:#0d1117; }
    .markdown-body { color-scheme: dark; --color-canvas-default:#0d1117; }
  }
  .markdown-body { box-sizing:border-box; max-width:980px; margin:0 auto; }
  img { max-width:100%; height:auto; }
  pre { overflow:auto; }
</style>
</head>
<body>
<article class="markdown-body">
$html
</article>
<script>
  document.querySelectorAll('pre code').forEach(el => { try { hljs.highlightElement(el); } catch(e) {} });
</script>
</body></html>
""".trimIndent()
}
