package com.antigravity.mobile

import android.app.Application
import com.antigravity.mobile.network.NetworkModule

class AntigravityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化网络模块 (TokenManager、OkHttpClient、AuthInterceptor 等)
        // 必须在使用 NetworkModule 的任何功能之前调用
        NetworkModule.init(this)
    }
}
