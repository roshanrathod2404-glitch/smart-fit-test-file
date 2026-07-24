package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSilver
import com.example.ui.theme.TextWhite

@Composable
fun PermissionGatewayScreen(
    currentLang: String = "English",
    onPermissionsCompleted: () -> Unit
) {
    val context = LocalContext.current
    var permissionStep by remember { mutableStateOf(0) } // 0: Dialog explanation, 1: Media, 3: Contacts, 4: Notifications, 5: Battery

    // Launchers for sequential permissions
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionStep = 3 // Move to Contacts
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionStep = 4 // Move to Notifications
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionStep = 5 // Move to Battery Optimization
    }

    LaunchedEffect(permissionStep) {
        when (permissionStep) {
            1 -> {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                mediaLauncher.launch(perm)
            }
            3 -> {
                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            4 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissionStep = 5
                }
            }
            5 -> {
                // Request ignore battery optimizations or finish
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
                onPermissionsCompleted()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TextWhite.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.get("permissions_title", currentLang),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = Strings.get("permissions_desc", currentLang),
                    fontSize = 13.sp,
                    color = TextSilver,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                PermissionExplanationItem(
                    title = Strings.get("permissions_media_title", currentLang),
                    desc = Strings.get("permissions_media_desc", currentLang)
                )
                PermissionExplanationItem(
                    title = Strings.get("permissions_contacts_title", currentLang),
                    desc = Strings.get("permissions_contacts_desc", currentLang)
                )
                PermissionExplanationItem(
                    title = Strings.get("permissions_bg_title", currentLang),
                    desc = Strings.get("permissions_bg_desc", currentLang)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        permissionStep = 1 // Start sequential runtime requests
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = Strings.get("next", currentLang),
                        color = BgCharcoal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "• $title",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Text(
            text = desc,
            color = TextSilver,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
