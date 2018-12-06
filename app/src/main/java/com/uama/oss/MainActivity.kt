package com.uama.oss

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.alibaba.sdk.android.oss.common.auth.OSSCustomSignerCredentialProvider
import com.alibaba.sdk.android.oss.common.utils.OSSUtils
import com.uama.oss.RealOssUpload.Companion.TAG
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        OssProvider.getInstance().init(OSSAuthCredentialsProvider("http://192.168.20.14:7666/"),application)
        val credentialProvider = object : OSSCustomSignerCredentialProvider() {
            override fun signContent(content: String?): String {
                return OSSUtils.sign("LTAIAubF6GXRgmHC","Ki094wdAUcmAW9QZ7jpLQxBi8DrKbP",content)
            }
        }
        OssProvider.getInstance().init(OssConfig.OssBucketName,credentialProvider,application)
        tx_search.setOnClickListener {
            selectFile()
        }
        tx_submit.setOnClickListener {
            uploadFile()
        }

    }

    lateinit var upload:IOssUpload
    private val pathList:MutableList<String> = mutableListOf()
    private fun uploadFile(){
        upload=RealOssUpload()
        upload.upLoad("user", pathList,object : UploadListener{
            override fun onError(msg: String) {
                tx_result.text = msg
            }

            override fun onSuccess(mutableList: MutableList<String>) {
                val a =  mutableList.joinToString ("\n")
                Log.i(TAG,"result-->$a")
                tx_result.text = a
                Log.i(TAG,"tx_result.text -->"+tx_result.text )
            }
        })
    }

    /**
     * 通过手机选择文件
     */
    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = ("*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "选择文件上传"), 345)
        } catch (no: ActivityNotFoundException) {
            Toast.makeText(this,"请安装一个文件管理器.",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && requestCode == 345) {
            if (data.data == null) return
            val path = ContentUtil.getPath(this, data.data)
            pathList.add(path)
            tx_fileName.text = pathList.joinToString("\n") {it.getFileName()}
        }
    }

    fun String?.isOEmpty():Boolean= TextUtils.isEmpty(this)
    fun String?.getFileName():String{
        if(isOEmpty())return ""
        if(this?.lastIndexOf("/")==-1){
            return ""
        }
        return this!!.substring(this.lastIndexOf("/")+1,this.length)
    }
}
