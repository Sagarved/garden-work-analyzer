package com.gardenworkanalyzer.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.gardenworkanalyzer.domain.model.PermissionStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify

class PermissionManagerTest : DescribeSpec({

    val permissionManager = PermissionManager()

    beforeSpec {
        mockkStatic(ContextCompat::class)
    }

    afterSpec {
        unmockkAll()
    }

    describe("checkPermission") {
        it("returns GRANTED when permission is granted") {
            val activity = mockk<Activity>(relaxed = true)
            every {
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            } returns PackageManager.PERMISSION_GRANTED

            permissionManager.checkPermission(activity, Manifest.permission.CAMERA) shouldBe
                    PermissionStatus.GRANTED
        }

        it("returns DENIED when rationale should be shown") {
            val activity = mockk<Activity>(relaxed = true)
            every {
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            } returns PackageManager.PERMISSION_DENIED
            every {
                activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            } returns true

            permissionManager.checkPermission(activity, Manifest.permission.CAMERA) shouldBe
                    PermissionStatus.DENIED
        }

        it("returns PERMANENTLY_DENIED when rationale should not be shown") {
            val activity = mockk<Activity>(relaxed = true)
            every {
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            } returns PackageManager.PERMISSION_DENIED
            every {
                activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            } returns false

            permissionManager.checkPermission(activity, Manifest.permission.CAMERA) shouldBe
                    PermissionStatus.PERMANENTLY_DENIED
        }

        it("returns DENIED when context is not an Activity") {
            val context = mockk<Context>(relaxed = true)
            every {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            } returns PackageManager.PERMISSION_DENIED

            permissionManager.checkPermission(context, Manifest.permission.CAMERA) shouldBe
                    PermissionStatus.DENIED
        }
    }

    describe("requestPermission") {
        it("launches the permission request via the launcher") {
            val launcher = mockk<ActivityResultLauncher<String>>(relaxed = true)

            permissionManager.requestPermission(Manifest.permission.CAMERA, launcher)

            verify { launcher.launch(Manifest.permission.CAMERA) }
        }
    }
})
