package ru.nsu.ccfit.zuev.osu

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.reco1l.verktex.Logger
import java.io.IOException

/**
 * The parts of [SecurityUtils] that need Android.
 */
object AndroidSecurityUtils {

    private const val flashlightCursorHash = "3b3afff3dab87f214053ded3163ff4e91cc3474e"
    private const val flashlightDimLayerHash = "59dfd45eecdfbeb7a91761a7af4b3e0162d13e9f"


    @JvmStatic
    fun getAppSignature(context: Context, packageName: String?) {
        if (!BuildType.hasOnlineAccess()) {
            return
        }
        if (SecurityUtils.appSignature != null || packageName.isNullOrEmpty()) {
            return
        }
        val pkgMgr: PackageManager? = context.packageManager
        val info: PackageInfo?
        val signatures: Array<Signature>

        try {
            if (pkgMgr == null) {
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signInfo = info.signingInfo!!

                signatures = if (signInfo.hasMultipleSigners()) {
                    signInfo.apkContentsSigners
                } else {
                    signInfo.signingCertificateHistory
                }
                SecurityUtils.appSignature = SecurityUtils.getHashCode(signatures[0].toByteArray())
            } else {
                @Suppress("DEPRECATION")
                info = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val infoSignatures = info?.signatures
                if (infoSignatures != null && infoSignatures.isNotEmpty()) {
                    val sign = infoSignatures[0]
                    SecurityUtils.appSignature = SecurityUtils.getHashCode(sign.toByteArray())
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
        }
    }

    @JvmStatic
    fun verifyFileIntegrity(context: Context): Boolean {
        return verifyFileIntegrity(context, "flashlight_cursor.png", flashlightCursorHash)
                && verifyFileIntegrity(context, "flashlight_dim_layer.png", flashlightDimLayerHash)
    }

    private fun verifyFileIntegrity(context: Context, fileName: String, hash: String): Boolean {
        try {
            context.assets.open(fileName).use { input ->
                val bytes = input.readBytes()

                return SecurityUtils.hmacSha1(bytes) == hash
            }
        } catch (e: IOException) {
            Logger.e("SecurityUtils", "Failed to check file integrity for $fileName", e)

            return false
        }
    }
}
