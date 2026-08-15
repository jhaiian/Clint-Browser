package com.jhaiian.clint.quiver.engine

import android.content.Context
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Owns the single active adblock-rust engine handle for the process.
 *
 * The native `Engine` is safe for concurrent reads from multiple threads (WebView dispatches
 * `shouldInterceptRequest` off a pool, not just the UI thread), but swapping the pointer on
 * reload is not - a query mid-call on the old handle while it's destroyed is a use-after-free.
 * A [ReentrantReadWriteLock] makes that swap safe: queries take the read lock (fully concurrent
 * with each other), [activate] takes the write lock (waits for in-flight queries, then swaps).
 */
object QuiverGuardEngine {

    private val lock = ReentrantReadWriteLock()
    private var handle: Long = 0L

    val isLoaded: Boolean
        get() = lock.read { handle != 0L }

    /** Outcome of a [preload] attempt. */
    enum class PreloadResult {
        /** Loaded successfully, or an engine was already active. */
        LOADED,
        /** No compiled database file exists yet - nothing to load. */
        NO_DATABASE,
        /** The file couldn't even be read (rare - `NO_DATABASE` covers the common case of it
         *  not existing at all). Not necessarily fixable by recompiling, so kept separate from
         *  the deserialization-specific reasons below. */
        FAILED,
        /** The on-disk file was compiled by an incompatible earlier/later version of
         *  adblock-rust. */
        VERSION_MISMATCH,
        /** The file is too short or missing its fixed magic-byte header - not a compiled
         *  engine file at all (e.g. truncated write, or an unrelated file). */
        BAD_HEADER,
        /** The header looked right, but the payload's checksum didn't match - the file was
         *  corrupted or partially overwritten after it was originally written. */
        BAD_CHECKSUM,
        /** The checksum matched, but the payload itself isn't valid flatbuffer data. */
        FLATBUFFER_PARSING_ERROR,
        /** A `DeserializationError` variant not otherwise mapped above (forward-compatibility
         *  fallback for a future adblock-rust release adding a new one). */
        UNKNOWN_DESERIALIZATION_ERROR;

        /** True when the on-disk file exists and was read, but couldn't be turned into an
         *  engine at all - reloading won't help, only recompiling from the stored filter list
         *  text will. Callers can use this to decide whether to prompt for a recompile. */
        val requiresRecompile: Boolean
            get() = this != LOADED && this != NO_DATABASE && this != FAILED
    }

    /** Loads the on-disk compiled engine, if one exists and nothing is loaded yet. */
    fun preload(context: Context): PreloadResult {
        lock.read { if (handle != 0L) return PreloadResult.LOADED }
        val file = QuiverGuardPaths.databaseFile(context)
        if (!file.exists()) return PreloadResult.NO_DATABASE
        val newHandle = QuiverGuardNative.nativeLoadEngine(file.absolutePath)
        val failure = when (newHandle) {
            0L -> PreloadResult.FAILED
            -1L -> PreloadResult.VERSION_MISMATCH
            -2L -> PreloadResult.BAD_HEADER
            -3L -> PreloadResult.BAD_CHECKSUM
            -4L -> PreloadResult.FLATBUFFER_PARSING_ERROR
            -5L -> PreloadResult.UNKNOWN_DESERIALIZATION_ERROR
            else -> null // A real engine pointer - success.
        }
        if (failure != null) return failure
        lock.write {
            if (handle != 0L) {
                // Lost a race with another preload/activate; drop what we just loaded.
                QuiverGuardNative.nativeDestroyEngine(newHandle)
            } else {
                handle = newHandle
            }
        }
        return PreloadResult.LOADED
    }

    /** Atomically activates a freshly compiled engine file, replacing whatever was active. */
    fun activate(path: String): Boolean {
        val newHandle = QuiverGuardNative.nativeLoadEngine(path)
        if (newHandle == 0L) return false
        lock.write {
            val old = handle
            handle = newHandle
            if (old != 0L) QuiverGuardNative.nativeDestroyEngine(old)
        }
        return true
    }

    data class NetworkCheck(
        val matched: Boolean,
        /** A `data:<mime>;base64,<content>` URI, present only for `$redirect=`-matched rules. */
        val redirectDataUrl: String?,
        val rewrittenUrl: String?,
        val csp: String?,
    )

    fun checkNetworkRequest(url: String, sourceUrl: String, requestType: String, method: String): NetworkCheck? =
        lock.read {
            if (handle == 0L) return@read null
            parseNetworkCheck(
                QuiverGuardNative.nativeCheckNetworkRequest(handle, url, sourceUrl, requestType, method)
            )
        }

    data class CosmeticResources(
        val hideSelectors: List<String>,
        /** JSON-encoded operator chains - see quiver_guard_cosmetic.js for the interpreter. */
        val proceduralActions: List<String>,
        val genericHide: Boolean,
        val injectedScript: String,
        /** Feed into [hiddenClassIdSelectors]; meaningless on its own. */
        val exceptions: List<String>,
    )

    fun urlCosmeticResources(url: String): CosmeticResources? =
        lock.read {
            if (handle == 0L) return@read null
            parseCosmeticResources(QuiverGuardNative.nativeUrlCosmeticResources(handle, url))
        }

    /**
     * Generic (non-hostname-specific) hiding rules matching the given class/id tokens, which
     * should be ones actually observed in the current page's DOM - see the kdoc on the native
     * declaration for why this can't just be folded into [urlCosmeticResources]. Returns null
     * (rather than an empty list) if the engine isn't loaded, same as the other query methods.
     */
    fun hiddenClassIdSelectors(classes: List<String>, ids: List<String>, exceptions: List<String>): List<String>? =
        lock.read {
            if (handle == 0L) return@read null
            try {
                JSONArray(
                    QuiverGuardNative.nativeHiddenClassIdSelectors(
                        handle,
                        JSONArray(classes).toString(),
                        JSONArray(ids).toString(),
                        JSONArray(exceptions).toString(),
                    )
                ).toStringList()
            } catch (e: JSONException) {
                null
            }
        }

    /**
     * Raw passthrough for [QuiverGuardJsBridge] - unlike [urlCosmeticResources] and
     * [checkNetworkRequest], this hands the native JSON straight back rather than parsing it
     * into a Kotlin object, since the bridge's only job is to relay it to page JS, which parses
     * it itself. Always returns well-formed JSON (an `{"error": "..."}` object rather than null
     * when nothing is loaded), since - unlike Kotlin call sites - page JS has no equivalent of a
     * nullable return to fall back on.
     */
    fun urlCosmeticResourcesJson(url: String): String =
        lock.read {
            if (handle == 0L) return@read """{"error":"engine not loaded"}"""
            QuiverGuardNative.nativeUrlCosmeticResources(handle, url)
        }

    /** Same idea as [urlCosmeticResourcesJson], for the network-check side (used for the page's
     * own address-bar $removeparam= cleanup - see [QuiverGuardJsBridge]). */
    fun checkNetworkRequestJson(url: String, sourceUrl: String, requestType: String, method: String): String =
        lock.read {
            if (handle == 0L) return@read """{"error":"engine not loaded"}"""
            QuiverGuardNative.nativeCheckNetworkRequest(handle, url, sourceUrl, requestType, method)
        }

    private fun parseNetworkCheck(json: String): NetworkCheck? = try {
        val obj = JSONObject(json)
        if (obj.has("error")) {
            null
        } else {
            NetworkCheck(
                matched = obj.optBoolean("matched", false),
                redirectDataUrl = obj.stringOrNull("redirect"),
                rewrittenUrl = obj.stringOrNull("rewrittenUrl"),
                csp = obj.stringOrNull("csp"),
            )
        }
    } catch (e: JSONException) {
        null
    }

    private fun parseCosmeticResources(json: String): CosmeticResources? = try {
        val obj = JSONObject(json)
        if (obj.has("error")) {
            null
        } else {
            CosmeticResources(
                hideSelectors = obj.optJSONArray("hideSelectors").toStringList(),
                proceduralActions = obj.optJSONArray("proceduralActions").toStringList(),
                genericHide = obj.optBoolean("genericHide", false),
                injectedScript = obj.optString("injectedScript", ""),
                exceptions = obj.optJSONArray("exceptions").toStringList(),
            )
        }
    } catch (e: JSONException) {
        null
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
