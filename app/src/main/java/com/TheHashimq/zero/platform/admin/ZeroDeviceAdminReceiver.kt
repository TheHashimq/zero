/*
 * MODULE: app (device administrator)
 * PURPOSE: The receiver Android uses to recognize Zero as a device administrator.
 * TAKES: System admin callbacks.
 * GIVES: Nothing. It requests no device policies.
 * OWNS: No state.
 * DOES NOT OWN: Protection decisions. Removal blocking is handled by the protection module.
 * DEPENDENCIES: Android DeviceAdminReceiver.
 * CONTRACT: The empty policy file means Zero cannot lock the screen, wipe data, or change passwords.
 * FAILURE BEHAVIOR: Not applicable.
 */
package com.TheHashimq.zero.platform.admin

import android.app.admin.DeviceAdminReceiver

class ZeroDeviceAdminReceiver : DeviceAdminReceiver()
