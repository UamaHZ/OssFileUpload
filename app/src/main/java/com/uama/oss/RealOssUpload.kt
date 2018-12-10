package com.uama.oss

import android.text.TextUtils
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
        var cancel: Boolean = false
        //判断looper循环是否需要执行
        private fun containsWhitUpload(): Boolean {
            //先遍历得到正在上传的队列数据
            val realOnUploadData = uploadQueue.filter { it.code != UploadResultEnum.DEFAULT }.toMutableList()
            //判断是否可以继续往该队列塞数据，size>0可以塞
            val canInSize =OssConfig.MaxUploadNumber - realOnUploadData.size
            //此处对
            val deRepeat=whitUploadQueue.distinctBy { it.getMapKey() }
                    .filterNot{it-> uploadQueue.any {upload->upload.getMapKey()==it.getMapKey() } }
            OssLog.i(TAG,"deRepeat length"+deRepeat.size)
            OssLog.i(TAG,"whitUploadQueue length"+whitUploadQueue.size)
            if(whitUploadQueue.size!=deRepeat.size){
                whitUploadQueue.clear()
                whitUploadQueue.addAll(deRepeat)
            }
            if(canInSize>0){
                //此处 待上传数据池，获取可以添加至正在上传的数据集合
               val needUpload=when(whitUploadQueue.size>=canInSize){
                    true->whitUploadQueue.subList(0,canInSize-1)
                    false->whitUploadQueue
                }

                needUpload.forEach{
                    realUpload(it)
                }
                uploadQueue.clear()
                //池待上传 ->迁移到上传中的池
                uploadQueue.addAll(realOnUploadData)
                uploadQueue.addAll(needUpload)
                whitUploadQueue.removeAll(needUpload)
            }
            //如果上传中的池子为空时，不需要执行looper不断获取上传结果来了
//            Log.i(TAG,"queue length"+uploadQueue.size)
            return uploadQueue.isNotEmpty()
        }

        val uploadHandler = UploadHandler()
        var ossAsyncTasks: MutableList<OSSAsyncTask<*>> =  mutableListOf()
        fun getLooperThread(): Thread = object :Thread("OssLooperThread"){
            override fun run() {
                while (!cancel) {
                    if(!(containsWhitUpload()||uploadMessageQueue.isNotEmpty())){
                        sleep(2000)
                        OssLog.i(TAG,"sleep -- begin"+System.currentTimeMillis())
                    }
//                    Log.i(TAG,"uploadMessageQueue"+uploadMessageQueue.size)
                    loopQueue()
                }
            }
        }
        val whitUploadQueue: MutableList<UploadResult> = mutableListOf()//待上传的图片队列
        val uploadQueue: ArrayList<UploadResult> = ArrayList(OssConfig.MaxUploadNumber) //正在上传的图片队列

        //这个是消息池；可以不断往这个池子中添加消息 当消息满足某个条件时，会将消息打包发送给handler
        val uploadMessageQueue: MutableList<UploadResult> = mutableListOf()

        val uploadGroupMap: MutableMap<String, UploadGroupInfo> = mutableMapOf()
        var uploadResultCache: MutableMap<String, UploadResult> = HashMap() //用于缓存文件上传结果，避免上传重复文件；现简单根据文件名判断；后可追加文件md5判断

        /**
         * 启动Looper 线程，用于不断遍历待上传图片池中是否有数据
         * @cancel 用户选择手动结束图片上传，此时也不在looper了
         */
        fun startLooper() {
            getLooperThread().start()
        }

        private fun loopQueue() {
            //不断读取queue中的消息，处理->将这个结果加入缓存图片列表；遍历uploadGroupMap相关消息，是否能够满足完全队列情况，满足发送消息出去
            if (uploadMessageQueue.isEmpty()) return
            OssLog.i(TAG,"uploadMessageQueue"+uploadMessageQueue.size+uploadMessageQueue[0].getMapKey())
            val uploadResult = uploadMessageQueue[0]
            uploadResult.idSet.forEach { it ->
                //如果缓存中某个item处理完毕，此时我们发送对应的事件通知他，可以结束了;同时将服务器地址从缓存中取出来，放在组队列中
                if (uploadGroupMap[it]?.isDealOver() == true) {
                    //缓存的上传任务中，清除改任务合集
                    removeCompleteTask()
                    //handler接收queue发送的消息
                    uploadHandler.handlerResult(uploadGroupMap[it]!!)
                    //上传结果为false时，此时肯定存在某些数据是上传失败，uploadResultCache移除掉这些失败的数据
                    if(uploadGroupMap[it]?.isSuccess()!=true){
                        //转换成对应的key
                        val uploadData = uploadGroupMap[it]?.keyMapList
                        //找到此次上传的缓存队列，再找到非成功数据
                        val unSuccessData = uploadResultCache.filter { map-> uploadData?.contains(map.value.getMapKey())?:false}
                                .filter {  it.value.code != UploadResultEnum.SUCCESS}
                        //移除缓存
                        unSuccessData.forEach{  uploadResultCache.remove(it.key)}
                    }
                    //待办的列表中移除该item，表示结果已传递
                    uploadGroupMap.remove(it)
                }
            }
            //此处移除队列中的消息
            uploadMessageQueue.removeAt(0)
        }
        private  val iOssUploadPath = RealOssUploadPath()
        private fun UploadResult.getServePath():String = iOssUploadPath.getOssUploadPath(uploadType,File(filePath))
        private fun realUpload(uploadResult: UploadResult){
            val put = PutObjectRequest(OssProvider.OssBucketName, uploadResult.getServePath(), uploadResult.filePath)
            val ossAsyncTask = OssProvider.getInstance().providerOss().asyncPutObject(put, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                    OssLog.i(TAG,"上传成功"+request.uploadFilePath)
                    uploadResult.code = UploadResultEnum.SUCCESS
                    uploadResult.servicePath = request.objectKey
                    uploadHandler.postUploadResult(uploadResult)
                    uploadResultCache[uploadResult.getMapKey()] = uploadResult
                }

                override fun onFailure(request: PutObjectRequest, clientExcepion: ClientException?, serviceException: ServiceException?) {
                    // 请求异常
                    clientExcepion?.printStackTrace()
                    uploadResult.code = UploadResultEnum.FAILED
                    uploadHandler.postUploadResult(uploadResult)
                    if (serviceException != null) {
                        // 服务异常
                        OssLog.e("ErrorCode", serviceException.errorCode)
                        OssLog.e("RequestId", serviceException.requestId)
                        OssLog.e("HostId", serviceException.hostId)
                        OssLog.e("RawMessage", serviceException.rawMessage)
                    }
                }
            })
            ossAsyncTasks.add(ossAsyncTask)
        }

        private fun removeCompleteTask(){
            ossAsyncTasks.removeAll(ossAsyncTasks.filter { it.isCompleted||it.isCanceled })
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
                        OssLog.w(TAG, "the strongCheck is open and the $unLegalStr is illegal")
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
     * @param uploadType 上传类型：文件夹
     * @param filePaths 文件路径列表
     * @param uploadListener 上传回调
     */
    private fun filesInQueue(uploadType: String, filePaths: List<String>, uploadListener: UploadListener) {
        if(filePaths.isEmpty()){
            uploadListener.onError("filePaths is empty,are you sure?")
            return
        }
        //id用来区分该上传队列
        val id = Math.round(100f).toString() + "-" + System.currentTimeMillis().toString()
        //创建数据实体映射，真实上传数据（文件路径，文件夹，上传结果）,服务器路径统一在上传结束获取
        val fileRealList = filePaths.map { UploadResult(it, idSet = mutableSetOf(id),uploadType = uploadType) }.toMutableList()
        //此处，过滤掉本地已经上传成功的文件
        val unUploadPathList = fileRealList
                .filter { uploadResultCache[it.getMapKey()]?.code != UploadResultEnum.SUCCESS }
        //存在待上传文件（缓存的上传成功文件不包含所有uploadType+filePaths数据）
        if (unUploadPathList.isNotEmpty()) {
            val uploadGroupInfo = UploadGroupInfo(keyMapList = fileRealList.map { it.getMapKey() }.toMutableList(),uploadListener= uploadListener)
            uploadGroupMap[id] = uploadGroupInfo

            //文件上传简单池数据入池，如果有数据位于上传中的池子，则将id放入改池子中的匹配数据，并将此数据移除
            val filterOnLoadData = unUploadPathList.filterNot { it ->
                uploadQueue.any { upload ->
                    if (upload.getMapKey() == it.getMapKey()) {
                        upload.idSet.add(id)
                        true
                    } else false
                }
            }

            //待上传数据池，数据过滤合并
            val endData = filterOnLoadData.filterNot {
                whitUploadQueue.any { whitUpload ->
                    if (whitUpload.getMapKey() == it.getMapKey()) {
                        whitUpload.idSet.add(id)
                        true
                    } else false
                }
            }
            whitUploadQueue.addAll(endData.distinctBy { it.getMapKey() })
            fileRealList.map { it.getMapKey() }

            //判断线程状态，没启动的情况下，启动该looper
            when (getLooperThread().state) {
                Thread.State.RUNNABLE -> {
                }
                else -> startLooper()
            }
        }else{
            //缓存中已经上传成功了，此时直接回调给前台上传成功即可
            uploadListener.onSuccess(fileRealList.map { uploadResultCache[uploadType+it.filePath]?.servicePath?:""}.toMutableList())
        }
    }

//    private fun getCacheOrCreatePath(uploadType: String, filePath:String):String{
//        return when(uploadResultCache[uploadType+filePath]?.code==null){
//            true->iOssUploadPath.getOssUploadPath(uploadType, File(filePath))
//            false->uploadResultCache[uploadType+filePath]?.serveFilePath?:""
//        }
//    }

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
        //首先关闭正在上传的所有任务栈
        ossAsyncTasks.forEach { if(!it.isCanceled)it.cancel() }
        ossAsyncTasks.clear()
        //用户手动取消时：待上传队列，上传队列全部清空；
        if(cancel){
            whitUploadQueue.clear()
            uploadQueue.clear()
            uploadMessageQueue.clear()
            uploadGroupMap.forEach { it.value.uploadListener.onError("is canceled by user") }
            uploadGroupMap.clear()
        }
    }

}

/**
 * 每次上传将其拼接起来
 */
data class UploadGroupInfo(var keyMapList: MutableList<String> = mutableListOf(),var filePaths: MutableList<String> = mutableListOf(),
                           var uploadListener: UploadListener,var isHandler: Boolean = false)

fun UploadGroupInfo.isDealOver(): Boolean {
    filePaths.clear()
    keyMapList.forEach {
        if(uploadResultCache[it] == null||uploadResultCache[it]?.code == UploadResultEnum.DEFAULT){
            return false
        }else{
            filePaths.add(uploadResultCache[it]!!.servicePath)
        }
    }
    return true
}

fun UploadGroupInfo.isSuccess(): Boolean {
    return !filePaths.any {
        return when (uploadResultCache[it] == null) {
            true -> true
            false -> uploadResultCache[it]?.code != UploadResultEnum.SUCCESS
        }
    }
}
