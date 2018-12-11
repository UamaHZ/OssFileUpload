package com.uama.oss

import android.util.Log
import com.alibaba.sdk.android.oss.common.OSSLog

class OssUploadLog{
    companion object {
        private var isOpen = false
        fun i(tag:String,info:String){
            if(isOpen)Log.i(tag,info)
        }
        fun e(tag:String,error:String){
            if(isOpen)Log.i(tag,error)
        }
        fun w(tag:String,warnings:String){
            if(isOpen)Log.i(tag,warnings)
        }

        fun enableLog(){
            isOpen = true
            OSSLog.enableLog()
        }
    }

}