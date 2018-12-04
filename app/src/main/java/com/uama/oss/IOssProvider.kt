package com.uama.oss

import android.app.Application
import com.alibaba.sdk.android.oss.OSS

interface IOssProvider{
    fun providerOss():OSS
}

