/*
 * NothingPacker – offline APK ⇄ IPA toolkit for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingpacker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.real3rr0r303yt.nothingpacker.data.Workspace
import com.real3rr0r303yt.nothingpacker.ui.navigation.NothingPackerNavHost
import com.real3rr0r303yt.nothingpacker.ui.theme.NothingPackerTheme

/**
 * AppCompatActivity (instead of ComponentActivity) so that per-app language
 * switching via AppCompatDelegate also works on Android 8–12.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
        )
        super.onCreate(savedInstanceState)
        // Fresh start: drop packages copied during an earlier session.
        if (savedInstanceState == null) Workspace(this).clear()

        setContent {
            NothingPackerTheme {
                NothingPackerNavHost()
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) Workspace(this).clear()
        super.onDestroy()
    }
}
