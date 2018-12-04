package com.uama.oss

import android.app.Activity
import android.os.Bundle
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OssProvider.getInstance().init(OSSAuthCredentialsProvider("http://192.168.20.14:7666/"),application)
        val upload:IOssUpload = RealOssUpload()
        upload.upLoad("user", mutableListOf(),object : UploadListener{
            override fun onError(msg: String) {

            }

            override fun onSuccess(mutableList: MutableList<String>) {
            }
        })
    }
}
