package com.uama.oss

import android.util.Log

class OssLog{
    companion object {
        var isOpen = true
        fun i(tag:String,info:String){
            if(isOpen)Log.i(tag,info)
        }
        fun e(tag:String,error:String){
            if(isOpen)Log.i(tag,error)
        }
        fun w(tag:String,warnings:String){
            if(isOpen)Log.i(tag,warnings)
        }
    }

}