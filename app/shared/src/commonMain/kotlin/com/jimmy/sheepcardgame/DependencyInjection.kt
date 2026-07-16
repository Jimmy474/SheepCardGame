package com.jimmy.sheepcardgame

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.modules

@Module
@ComponentScan("com.jimmy.sheepcardgame")
class DependencyInjection

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(DependencyInjection::class)
}
