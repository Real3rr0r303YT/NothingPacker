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

package com.real3rr0r303yt.nothingpacker.core.convert

/** Best-effort mapping between Android permissions and iOS usage-description keys. */
object PermissionMap {

    private val androidToIos = linkedMapOf(
        "android.permission.CAMERA" to "NSCameraUsageDescription",
        "android.permission.RECORD_AUDIO" to "NSMicrophoneUsageDescription",
        "android.permission.ACCESS_FINE_LOCATION" to "NSLocationWhenInUseUsageDescription",
        "android.permission.ACCESS_COARSE_LOCATION" to "NSLocationWhenInUseUsageDescription",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "NSLocationAlwaysAndWhenInUseUsageDescription",
        "android.permission.READ_CONTACTS" to "NSContactsUsageDescription",
        "android.permission.WRITE_CONTACTS" to "NSContactsUsageDescription",
        "android.permission.READ_CALENDAR" to "NSCalendarsUsageDescription",
        "android.permission.WRITE_CALENDAR" to "NSCalendarsUsageDescription",
        "android.permission.READ_MEDIA_IMAGES" to "NSPhotoLibraryUsageDescription",
        "android.permission.READ_MEDIA_VIDEO" to "NSPhotoLibraryUsageDescription",
        "android.permission.READ_EXTERNAL_STORAGE" to "NSPhotoLibraryUsageDescription",
        "android.permission.BLUETOOTH_CONNECT" to "NSBluetoothAlwaysUsageDescription",
        "android.permission.BLUETOOTH_SCAN" to "NSBluetoothAlwaysUsageDescription",
        "android.permission.ACTIVITY_RECOGNITION" to "NSMotionUsageDescription",
        "android.permission.BODY_SENSORS" to "NSHealthShareUsageDescription",
        "android.permission.USE_BIOMETRIC" to "NSFaceIDUsageDescription",
        "android.permission.NEARBY_WIFI_DEVICES" to "NSLocalNetworkUsageDescription",
    )

    private val iosToAndroid: Map<String, List<String>> = mapOf(
        "NSCameraUsageDescription" to listOf("android.permission.CAMERA"),
        "NSMicrophoneUsageDescription" to listOf("android.permission.RECORD_AUDIO"),
        "NSLocationWhenInUseUsageDescription" to listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"),
        "NSLocationAlwaysAndWhenInUseUsageDescription" to listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION"),
        "NSLocationAlwaysUsageDescription" to listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION"),
        "NSContactsUsageDescription" to listOf("android.permission.READ_CONTACTS"),
        "NSCalendarsUsageDescription" to listOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"),
        "NSCalendarsFullAccessUsageDescription" to listOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"),
        "NSPhotoLibraryUsageDescription" to listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"),
        "NSPhotoLibraryAddUsageDescription" to listOf("android.permission.READ_MEDIA_IMAGES"),
        "NSBluetoothAlwaysUsageDescription" to listOf("android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"),
        "NSBluetoothPeripheralUsageDescription" to listOf("android.permission.BLUETOOTH_CONNECT"),
        "NSMotionUsageDescription" to listOf("android.permission.ACTIVITY_RECOGNITION"),
        "NSHealthShareUsageDescription" to listOf("android.permission.BODY_SENSORS"),
        "NSFaceIDUsageDescription" to listOf("android.permission.USE_BIOMETRIC"),
        "NSLocalNetworkUsageDescription" to listOf("android.permission.NEARBY_WIFI_DEVICES"),
        "NSUserTrackingUsageDescription" to listOf("com.google.android.gms.permission.AD_ID"),
    )

    fun iosKeysFor(androidPermissions: List<String>): List<String> =
        androidPermissions.mapNotNull { androidToIos[it] }.distinct()

    fun androidPermissionsFor(iosKeys: List<String>): List<String> =
        iosKeys.flatMap { iosToAndroid[it].orEmpty() }.distinct()
}
