package com.cafeapp.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val dispatcher: CafeDispatchers)

enum class CafeDispatchers {
    IO,
    Default,
}
