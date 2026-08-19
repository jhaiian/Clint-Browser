package com.jhaiian.clint.quiver.engine

internal object QuiverGuardNative {

    init {
        System.loadLibrary("adblockrust")
    }

    @JvmStatic
    external fun nativeNewFilterSetBuilder(): Long

    @JvmStatic
    external fun nativeAddFilterListRules(builderHandle: Long, rules: String): String

    @JvmStatic
    external fun nativeDestroyFilterSetBuilder(builderHandle: Long)

    @JvmStatic
    external fun nativeFinalizeEngine(builderHandle: Long, outputPath: String): String

    @JvmStatic
    external fun nativeLoadEngine(path: String): Long

    @JvmStatic
    external fun nativeDestroyEngine(handle: Long)

    @JvmStatic
    external fun nativeCheckNetworkRequest(
        handle: Long,
        url: String,
        sourceUrl: String,
        requestType: String,
        method: String,
    ): String

    @JvmStatic
    external fun nativeUrlCosmeticResources(handle: Long, url: String): String

    @JvmStatic
    external fun nativeHiddenClassIdSelectors(
        handle: Long,
        classesJson: String,
        idsJson: String,
        exceptionsJson: String,
    ): String
}
