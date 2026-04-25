#!/usr/bin/env python3
"""Lightweight project status server for the GitHub Control Android project.

This project is a native Android Kotlin app (built into an APK by GitHub
Actions). Replit cannot build native APKs, so this small server simply serves
a status page describing the project state, the file count, and the latest
git commit so the workspace preview is not empty.
"""
from __future__ import annotations

import http.server
import json
import os
import socketserver
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PORT = int(os.environ.get("PORT", "5000"))


def _git(*args: str) -> str:
    try:
        return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()
    except Exception:
        return ""


def _stats() -> dict:
    src = ROOT / "app" / "src" / "main" / "java"
    kt_files = list(src.rglob("*.kt"))
    total_lines = 0
    for f in kt_files:
        try:
            total_lines += sum(1 for _ in f.open("rb"))
        except Exception:
            pass
    return {
        "kotlinFiles": len(kt_files),
        "kotlinLines": total_lines,
        "lastCommit": _git("log", "-1", "--pretty=%h %s"),
        "branch": _git("rev-parse", "--abbrev-ref", "HEAD"),
        "appId": "com.githubcontrol",
    }


HTML = """<!doctype html>
<html lang=\"en\">
<head>
<meta charset=\"utf-8\">
<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">
<title>GitHub Control · Android Source</title>
<style>
:root{color-scheme:dark light}
*{box-sizing:border-box}
body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#0d1117;color:#e6edf3;min-height:100vh}
header{padding:24px;background:linear-gradient(135deg,#1f6feb,#8957e5);color:#fff}
h1{margin:0;font-size:22px;font-weight:700}
p.sub{margin:6px 0 0;opacity:.85;font-size:14px}
main{max-width:780px;margin:0 auto;padding:24px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin:16px 0 24px}
.card{background:#161b22;border:1px solid #30363d;border-radius:12px;padding:16px}
.k{font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:#8b949e}
.v{font-size:20px;font-weight:700;margin-top:6px}
.note{background:#161b22;border:1px solid #30363d;border-radius:12px;padding:16px;line-height:1.55;font-size:14px}
code{background:#0d1117;border:1px solid #30363d;border-radius:6px;padding:2px 6px;font-size:12px}
a{color:#58a6ff}
</style>
</head>
<body>
<header>
  <h1>GitHub Control — Android source workspace</h1>
  <p class=\"sub\">Native Kotlin + Jetpack Compose client. Built into APK by GitHub Actions.</p>
</header>
<main>
  <div class=\"grid\" id=\"grid\"></div>
  <div class=\"note\">
    Replit cannot build or run Android APKs in the workspace. The full app is
    compiled by the GitHub Actions workflow at
    <code>.github/workflows/build-debug-apk.yml</code> and the resulting APK is
    attached to the workflow run as an artifact (and to a GitHub Release on tag
    pushes).
    <br><br>
    To test locally, open the project in <strong>Android Studio</strong> or run
    <code>./gradlew assembleDebug</code> on a machine with Android SDK 35.
  </div>
</main>
<script>
fetch('/status').then(r=>r.json()).then(s=>{
  const g=document.getElementById('grid');
  const items=[
    ['App ID',s.appId],
    ['Kotlin files',s.kotlinFiles],
    ['Kotlin lines',s.kotlinLines],
    ['Branch',s.branch||'—'],
    ['Last commit',s.lastCommit||'—']
  ];
  g.innerHTML=items.map(([k,v])=>`<div class=\"card\"><div class=\"k\">${k}</div><div class=\"v\">${v}</div></div>`).join('');
});
</script>
</body>
</html>
"""


class Handler(http.server.BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):  # quiet
        print(fmt % args)

    def do_GET(self):
        if self.path == "/status":
            data = json.dumps(_stats()).encode("utf-8")
            self.send_response(200)
            self.send_header("content-type", "application/json")
            self.send_header("cache-control", "no-store")
            self.send_header("content-length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        body = HTML.encode("utf-8")
        self.send_response(200)
        self.send_header("content-type", "text/html; charset=utf-8")
        self.send_header("cache-control", "no-store")
        self.send_header("content-length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main():
    with socketserver.TCPServer(("0.0.0.0", PORT), Handler) as httpd:
        print(f"Project status server listening on 0.0.0.0:{PORT}")
        httpd.serve_forever()


if __name__ == "__main__":
    main()
