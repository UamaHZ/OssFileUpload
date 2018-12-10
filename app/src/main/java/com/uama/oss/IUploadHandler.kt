package com.uama.oss

/**
 * 定制一个读取消息池的handler类
 * 可读取
 */
interface IUploadHandler{
    fun postUploadResult(result:UploadResult)
    fun handlerResult(uploadGroupInfo: UploadGroupInfo)
}


//成功及失败code对应枚举
enum class UploadResultEnum(code:Int){
    SUCCESS(1),
    FAILED(-1),
    DEFAULT(0)
}

/**
 * 文件上传结果，是消息的单元体
 * 可以参考Message，每次发送消息都是发送这个
 * 此处加一个id索引列表 idSet
 */
data class UploadResult(var filePath:String,var code:UploadResultEnum = UploadResultEnum.DEFAULT,
                        val idSet:MutableSet<String> = mutableSetOf(),var uploadType:String="",var servicePath:String = "")

fun UploadResult.getMapKey():String = uploadType+filePath