package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import com.example.BuildConfig

object UpdateChecker {
    private const val TAG = "UpdateChecker"

    suspend fun getLatestRelease(): Pair<String, String>? = withContext(Dispatchers.IO) {
        val owner = try { BuildConfig.GITHUB_OWNER } catch(e: Exception) { "Brave290" }
        val repo = try { BuildConfig.GITHUB_REPO } catch(e: Exception) { "HifzGuard" }
        
        val urlString = "https://api.github.com/repos/$owner/$repo/releases/latest"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            // Crucial: GitHub API requires User-Agent header, otherwise returns a 403 Forbidden Response
            connection.setRequestProperty("User-Agent", "HifzGuardUpdateChecker")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                
                // Extract direct APK asset link if it exists in assets list
                var downloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i)
                        if (asset != null) {
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                downloadUrl = asset.optString("browser_download_url", downloadUrl)
                                break
                            }
                        }
                    }
                }
                Log.i(TAG, "Successfully retrieved latest version: $tagName, download link: $downloadUrl")
                return@withContext Pair(tagName, downloadUrl)
            } else {
                Log.e(TAG, "API call failed with HTTP response status: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception encountered checking for application updates", e)
        } finally {
            connection?.disconnect()
        }
        return@withContext null
    }
}
