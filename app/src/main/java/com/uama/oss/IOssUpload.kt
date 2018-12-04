package com.uama.oss

import java.io.File


/**
 * Created by guozhen.hou on 2018/12/3.
 * 上传抽象，包括上传和取消操作
 */
interface IOssUpload {
    /**
     * @param isStrongCheck true开启强校验：文件不合法，直接提示上传失败 false：对不合法文件过滤，只上传合法文件，默认false
     */
    fun upLoad(uploadType: String, filePaths: List<String>, uploadListener: UploadListener,isStrongCheck:Boolean = false)
    fun cancelUpload()
}

interface UploadListener{
    fun onSuccess(mutableList: MutableList<String>)
    fun onError(msg:String)
}

/**
 * oss 文件上传路径接口
 */
interface IOssUploadPath {
    fun getOssUploadPath(uploadType: String, file: File): String
}