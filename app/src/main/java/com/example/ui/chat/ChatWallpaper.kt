package com.example.ui.chat

import androidx.annotation.DrawableRes
import com.example.R

data class ChatWallpaper(
    val id: String,
    val name: String,
    val description: String,
    @DrawableRes val drawableRes: Int,
    val isDarkTheme: Boolean
)

object ChatWallpapers {
    val LIGHT_WALLPAPERS = listOf(
        ChatWallpaper(
            id = "light_1",
            name = "Pastel Bokeh",
            description = "Soft lavender & peach blur",
            drawableRes = R.drawable.img_wallpaper_light_1,
            isDarkTheme = false
        ),
        ChatWallpaper(
            id = "light_2",
            name = "Watercolor Wash",
            description = "Sky blue & pale mint fluid",
            drawableRes = R.drawable.img_wallpaper_light_2,
            isDarkTheme = false
        ),
        ChatWallpaper(
            id = "light_3",
            name = "Warm Sand",
            description = "Cream waves & neutral dunes",
            drawableRes = R.drawable.img_wallpaper_light_3,
            isDarkTheme = false
        ),
        ChatWallpaper(
            id = "light_4",
            name = "Frosted Rose",
            description = "Translucent blush & rose gold",
            drawableRes = R.drawable.img_wallpaper_light_4,
            isDarkTheme = false
        ),
        ChatWallpaper(
            id = "light_5",
            name = "Dawn Mist",
            description = "Morning periwinkle & soft gold",
            drawableRes = R.drawable.img_wallpaper_light_5,
            isDarkTheme = false
        )
    )

    val DARK_WALLPAPERS = listOf(
        ChatWallpaper(
            id = "dark_1",
            name = "Midnight Nebula",
            description = "Deep cosmic indigo & auroral dust",
            drawableRes = R.drawable.img_wallpaper_dark_1,
            isDarkTheme = true
        ),
        ChatWallpaper(
            id = "dark_2",
            name = "Obsidian Velvet",
            description = "Charcoal & diffuse violet curves",
            drawableRes = R.drawable.img_wallpaper_dark_2,
            isDarkTheme = true
        ),
        ChatWallpaper(
            id = "dark_3",
            name = "Mystic Emerald",
            description = "Deep forest teal & soft bokeh",
            drawableRes = R.drawable.img_wallpaper_dark_3,
            isDarkTheme = true
        ),
        ChatWallpaper(
            id = "dark_4",
            name = "Cyber Twilight",
            description = "Deep purple & ambient magenta",
            drawableRes = R.drawable.img_wallpaper_dark_4,
            isDarkTheme = true
        ),
        ChatWallpaper(
            id = "dark_5",
            name = "Slate Quartz",
            description = "Smoky marble & metallic glow",
            drawableRes = R.drawable.img_wallpaper_dark_5,
            isDarkTheme = true
        )
    )

    val ALL_WALLPAPERS = LIGHT_WALLPAPERS + DARK_WALLPAPERS

    fun getWallpaperById(id: String?): ChatWallpaper? {
        if (id.isNullOrBlank() || id == "NONE") return null
        return ALL_WALLPAPERS.find { it.id == id }
    }
}
