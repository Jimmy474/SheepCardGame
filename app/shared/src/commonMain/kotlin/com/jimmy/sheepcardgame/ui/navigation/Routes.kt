package com.jimmy.sheepcardgame.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Routes: NavKey{
    @Serializable
    object HomeRoute: Routes

    @Serializable
    object RoomRoute: Routes

    @Serializable
    object HowToPlayRoute: Routes

    @Serializable
    object GameRoute: Routes

    @Serializable
    object LocalGameRoute: Routes

    @Serializable
    object SettingsRoute: Routes

    @Serializable
    object AboutRoute: Routes
}

@OptIn(ExperimentalSerializationApi::class)
val RoutesConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<Routes>()
        }
    }
}