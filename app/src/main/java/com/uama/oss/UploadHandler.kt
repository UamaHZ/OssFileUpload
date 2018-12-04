package com.uama.oss

class UploadHandler:IUploadHandler{
    override fun postUploadResult(result: UploadResult) {
        RealOssUpload.uploadMessageQueue.add(result)
    }

    override fun handlerResult(uploadGroupInfo: UploadGroupInfo) {
        when(uploadGroupInfo.isSuccess()){
            true-> uploadGroupInfo.uploadListener.onSuccess(uploadGroupInfo.filePaths)
            false-> uploadGroupInfo.uploadListener.onError("have some file upload failed")
        }
    }

}