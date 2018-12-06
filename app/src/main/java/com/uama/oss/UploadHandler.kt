package com.uama.oss

import android.os.Handler
import android.os.Looper

class UploadHandler:IUploadHandler{
    override fun postUploadResult(result: UploadResult) {
        RealOssUpload.uploadMessageQueue.add(result)
    }

    override fun handlerResult(uploadGroupInfo: UploadGroupInfo) {
        //此处将所有结果同步到主线程
        Handler(Looper.getMainLooper()).post {
            when(uploadGroupInfo.isSuccess()){
                true-> uploadGroupInfo.uploadListener.onSuccess(uploadGroupInfo.filePaths)
                false-> uploadGroupInfo.uploadListener.onError("have some file upload failed")
            }
         }
    }

}