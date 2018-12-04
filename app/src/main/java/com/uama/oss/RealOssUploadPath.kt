package com.uama.oss

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.telephony.TelephonyManager
import android.text.TextUtils
import java.io.File


class RealOssUploadPath : IOssUploadPath {
    private var noNumber = 0
    override fun getOssUploadPath(uploadType: String, file: File): String {
        return String.format("%s%s%s-%s-%s%s", uploadType, File.separator, getUUID(OssProvider.mApplication),
                System.currentTimeMillis(), getPathNumber(), getFileType(file))
    }

    private fun getPathNumber(): String {
        val str = noNumber.rem(OssConfig.MaxUploadNumber).toString()
        ++noNumber
        return str
    }

    @SuppressLint("HardwareIds")
    fun getUUID(application: Application?): String {
        return try {
            val tm =application?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var deviceId = tm.deviceId
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = getLocalGenerateDeviceId()
            }
            deviceId
        } catch (e: SecurityException) {
            getLocalGenerateDeviceId()
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalGenerateDeviceId()
        }

    }

    /**
     * 获取不到ID本地生成15随机数并保存下来
     * @return
     */
    private fun getLocalGenerateDeviceId(): String {
        try {
            val random = Math.random()
            val str = random.toString().substring(2, 11)
            val str2 = random.toString().substring(2, 8)
            return str + str2
        } catch (e: Exception) {
            e.printStackTrace()
            return "888957125800000"
        }

    }

    private fun getFileType(file: File?): String {
        if (file == null || !file.exists() || !file.isFile) return ""
        val path = file.path
        if (TextUtils.isEmpty(path)) return ""
        val splitStr = path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (splitStr.isNotEmpty()) {
            String.format(".%s", splitStr[splitStr.size - 1])
        } else ""
    }

}