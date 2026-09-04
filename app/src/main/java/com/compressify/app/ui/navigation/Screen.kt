package com.compressify.app.ui.navigation

sealed class Screen {
    object Home : Screen()
    object Preview : Screen()
    object Result : Screen()
    object Settings : Screen()
}
