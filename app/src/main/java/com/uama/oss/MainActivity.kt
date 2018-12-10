package com.uama.oss

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import com.alibaba.sdk.android.oss.common.auth.OSSCustomSignerCredentialProvider
import com.alibaba.sdk.android.oss.common.utils.OSSUtils
import com.uama.oss.RealOssUpload.Companion.TAG
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    private val code_1 = 1001
    private val code_2= 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        OssProvider.getInstance().init(OSSAuthCredentialsProvider("http://192.168.20.14:7666/"),application)
        val credentialProvider = object : OSSCustomSignerCredentialProvider() {
            override fun signContent(content: String?): String {
                return OSSUtils.sign("LTAIAubF6GXRgmHC","Ki094wdAUcmAW9QZ7jpLQxBi8DrKbP",content)
            }
        }
        OssProvider.getInstance().init("uama",credentialProvider,application)
        tx_search_1.setOnClickListener {
            selectFile(code_1)
        }
        tx_search_2.setOnClickListener {
            selectFile(code_2)
        }
        tx_submit.setOnClickListener {
            uploadFile(tx_result_1,pathList1)
        }
        tx_submit_2.setOnClickListener {
//            uploadFile(tx_result_2,pathList2)
            upload.cancelUpload()
        }
    }

    private lateinit var upload:IOssUpload
    private val pathList1:MutableList<String> = mutableListOf()
    private val pathList2:MutableList<String> = mutableListOf()
    private fun uploadFile(tx:TextView,pathList:MutableList<String>){
        upload=RealOssUpload()
        upload.upLoad("user", pathList,object : UploadListener{
            override fun onError(msg: String) {
                tx.text = msg
            }

            override fun onSuccess(mutableList: MutableList<String>) {
                val a =  mutableList.joinToString ("\n")
                OssLog.i(TAG,"result-->$a")
                tx.text = a
                OssLog.i(TAG,"tx_result.text -->"+tx.text )
            }
        })
    }

    /**
     * 通过手机选择文件
     */
    private fun selectFile(code:Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = ("*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "选择文件上传"), code)
        } catch (no: ActivityNotFoundException) {
            Toast.makeText(this,"请安装一个文件管理器.",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && requestCode == code_1) {
            if (data.data == null) return
            val path = ContentUtil.getPath(this, data.data)
            pathList1.add(path)
            tx_search_1.text = pathList1.joinToString("\n") {it.getFileName()}
        }
        if (resultCode == Activity.RESULT_OK && data != null && requestCode == code_2) {
            if (data.data == null) return
            val path = ContentUtil.getPath(this, data.data)
            pathList2.add(path)
            tx_search_2.text = pathList2.joinToString("\n") {it.getFileName()}
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
