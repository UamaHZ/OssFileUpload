package com.uama.oss

import android.text.TextUtils
import android.util.Log
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.uama.oss.RealOssUpload.Companion.uploadResultCache
import java.io.File

/**
 * 图片上传的最终函数
 */
class RealOssUpload : IOssUpload {
    companion object {
        /**
         * 此方法判断队列中，是否存在待上传的图片
         */
        const val TAG = "OssFileUpload"
        private var cancel: Boolean = false
        private fun containsWhitUpload(): Boolean {
            val realOnUploadData = uploadQueue.filter { it.code != UploadResultEnum.DEFAULT }.toMutableList()
            val canInSize =OssConfig.MaxUploadNumber - realOnUploadData.size
            if(canInSize>0){
               val needUpload=when(whitUploadQueue.size>=canInSize){
                    true->whitUploadQueue.subList(0,canInSize-1)
                    false->whitUploadQueue
                }
                needUpload.forEach{
                    realUpload(it)
                }
                uploadQueue.clear()
                uploadQueue.addAll(realOnUploadData)
                uploadQueue.addAll(needUpload)
            }
            return uploadQueue.isNotEmpty()
        }

        val uploadHandler = UploadHandler()
        private var ossAsyncTasks: MutableList<OSSAsyncTask<*>> =  mutableListOf()
        private fun getLooperThread(): Thread = Thread("Looper")
        val whitUploadQueue: MutableList<UploadResult> = mutableListOf()//待上传的图片队列
        val uploadQueue: ArrayList<UploadResult> = ArrayList(OssConfig.MaxUploadNumber) //正在上传的图片队列

        //这个是消息池；可以不断往这个池子中添加消息 当消息满足某个条件时，会将消息打包发送给handler
        val uploadMessageQueue: MutableList<UploadResult> = mutableListOf()
        private val uploadGroupMap: MutableMap<String, UploadGroupInfo> = mutableMapOf()
        var uploadResultCache: MutableMap<String, UploadResult> = HashMap() //用于缓存文件上传结果，避免上传重复文件；现简单根据文件名判断；后可追加文件md5判断

        /**
         * 启动Looper 线程，用于不断遍历待上传图片池中是否有数据
         */
        fun startLooper() {
            getLooperThread().run {
                when (containsWhitUpload() && !cancel) {
                    true -> loopQueue()
                    false -> return
                }
            }
        }

        private fun loopQueue() {
            //不断读取queue中的消息，处理->将这个结果加入缓存图片列表；遍历uploadGroupMap相关消息，是否能够满足完全队列情况，满足发送消息出去
            if (uploadMessageQueue.isEmpty()) return
            val uploadResult = uploadMessageQueue[0]
            uploadResult.idSet.forEach { it ->
                //如果缓存中某个item处理完毕，此时我们发送对应的事件通知他，可以结束了
                if (uploadGroupMap[it]?.isDealOver() == true) {
                    //handler接收queue发送的消息
                    uploadHandler.handlerResult(uploadGroupMap[it]!!)
                    //待办的列表中移除该item，表示结果已传递
                    uploadGroupMap.remove(it)
                    if(!uploadGroupMap[it]!!.isSuccess()){
                        val uploadData = uploadGroupMap[it]!!.filePaths
                        val unSuccessData = uploadResultCache.filter { map-> uploadData.contains(map.value.serveFilePath)}
                        unSuccessData.forEach{  uploadResultCache.remove(it.key)}
                    }
                }
            }
        }

        private fun realUpload(uploadResult: UploadResult){
            val put = PutObjectRequest(OssConfig.OssBucketName, uploadResult.serveFilePath, uploadResult.filePath)
            val ossAsyncTask = OssProvider.getInstance().providerOss().asyncPutObject(put, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                    uploadResult.code = UploadResultEnum.SUCCESS
                    uploadHandler.postUploadResult(uploadResult)
                }

                override fun onFailure(request: PutObjectRequest, clientExcepion: ClientException?, serviceException: ServiceException?) {
                    // 请求异常
                    clientExcepion?.printStackTrace()
                    uploadResult.code = UploadResultEnum.FAILED
                    uploadHandler.postUploadResult(uploadResult)
                    if (serviceException != null) {
                        // 服务异常
                        Log.e("ErrorCode", serviceException.errorCode)
                        Log.e("RequestId", serviceException.requestId)
                        Log.e("HostId", serviceException.hostId)
                        Log.e("RawMessage", serviceException.rawMessage)
                    }
                }
            })
            ossAsyncTasks.add(ossAsyncTask)
        }
    }

    //单个上传接口
    override fun upLoad(uploadType: String, filePaths: List<String>, uploadListener: UploadListener, isStrongCheck: Boolean) {
        cancel = false
        //获取缓存外，待上传的队列
        var unLegalStr = ""
        when (isStrongCheck) {
            true ->
                when (filePaths.any { it ->
                    unLegalStr = it
                    !verifyFilePath(it)
                }) {
                    true -> {
                        Log.w(TAG, "the strongCheck is open and the $unLegalStr is illegal")
                        uploadListener.onError("the strongCheck is open and the $unLegalStr is illegal")
                        return
                    }
                    false -> {
                        filesInQueue(uploadType, filePaths, uploadListener)
                    }
                }
            false -> {
                filesInQueue(uploadType, filePaths, uploadListener)
            }
        }
    }

    /**
     * 每次传入数据：注意，此时我们要修正所有匹配到的上传失败数据
     */
    private fun filesInQueue(uploadType: String, filePaths: List<String>, uploadListener: UploadListener) {
        val iOssUploadPath = RealOssUploadPath()
        val fileRealList = filePaths.map { UploadResult(it,
                serveFilePath=iOssUploadPath.getOssUploadPath(uploadType, File(it))) }.toMutableList()
        val id = Math.round(100f).toString() + "-" + System.currentTimeMillis().toString()
        val unUploadPathList = fileRealList
                .filter { uploadResultCache[it.serveFilePath]?.code != UploadResultEnum.SUCCESS } //这个是本地为缓存的文件列表
        if (unUploadPathList.isNotEmpty()) {
            val uploadGroupInfo = UploadGroupInfo(fileRealList.map { it.serveFilePath }.toMutableList(), uploadListener)
            uploadGroupMap[id] = uploadGroupInfo

            //文件上传简单池数据入池
            val filterOnLoadData = unUploadPathList.filter { it ->
                uploadQueue.any { upload ->
                    if (upload.serveFilePath == it.serveFilePath) {
                        upload.idSet.add(id)
                        true
                    } else false
                }
            } //如果有数据位于上传中的池子，则将id放入改池子中的匹配数据，并将此数据移除

            val endData = filterOnLoadData.filter {
                whitUploadQueue.any { whitUpload ->
                    if (whitUpload.serveFilePath == it.serveFilePath) {
                        whitUpload.idSet.add(id)
                        true
                    } else false
                }
            }.map { UploadResult(it.filePath, idSet = mutableSetOf(id),serveFilePath=it.serveFilePath) }
            whitUploadQueue.addAll(endData)
            when (getLooperThread().state) {
                Thread.State.RUNNABLE -> {
                }
                else -> startLooper()
            }
        }
    }

    /**
     * 根据路径校验文件是否合法，真实存在
     * @param path 文件路径
     */
    private fun verifyFilePath(path: String?): Boolean {
        return when (TextUtils.isEmpty(path)) {
            true -> false
            false -> verifyFilePath(File(path))
        }
    }

    /**
     * 直接检查文件是否合法
     */
    private fun verifyFilePath(file: File?): Boolean {
        return file?.isFile ?: false
    }

    override fun cancelUpload() {
        cancel = true
        ossAsyncTasks.forEach { if(!it.isCanceled)it.cancel() }
    }

}

/**
 * 每次上传将其拼接起来
 */
data class UploadGroupInfo(var filePaths: MutableList<String>, var uploadListener: UploadListener, var isHandler: Boolean = false)

fun UploadGroupInfo.isDealOver(): Boolean {
    return !filePaths.any {
        return when (uploadResultCache[it] == null) {
            true -> true
            false -> uploadResultCache[it]?.code == UploadResultEnum.DEFAULT
        }
    }
}

fun UploadGroupInfo.isSuccess(): Boolean {
    return !filePaths.any {
        return when (uploadResultCache[it] == null) {
            true -> true
            false -> uploadResultCache[it]?.code != UploadResultEnum.SUCCESS
        }
    }
}
