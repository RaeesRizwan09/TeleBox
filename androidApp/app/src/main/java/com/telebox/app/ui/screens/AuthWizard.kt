package com.telebox.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.telebox.app.data.AppThemeMode
import com.telebox.app.data.AuthStep
import com.telebox.app.data.LoginMethod
import com.telebox.app.ui.theme.TeleBoxTheme
import com.telebox.app.util.formatFloodWait
import com.telebox.app.viewmodel.AuthUiState

@Composable
fun AuthWizardScreen(
    state: AuthUiState,
    theme: AppThemeMode,
    onToggleTheme: () -> Unit,
    onApiIdChange: (String) -> Unit,
    onApiHashChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSetupSubmit: () -> Unit,
    onPhoneSubmit: () -> Unit,
    onCodeSubmit: () -> Unit,
    onPasswordSubmit: () -> Unit,
    onStartQr: () -> Unit,
    onSwitchPhone: () -> Unit,
    onRefreshQr: () -> Unit,
    onGoSetup: () -> Unit,
    onGoPhone: () -> Unit,
    onGoCode: () -> Unit,
    onShowHelp: (Boolean) -> Unit,
    onShowDonate: (Boolean) -> Unit,
    onQrPoll: () -> Unit,
    onDevLogin: () -> Unit
) {
    val colors = TeleBoxTheme.colors
    val context = LocalContext.current

    LaunchedEffect(state.qrPolling) {
        if (state.qrPolling) onQrPoll()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.authGradient)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = if (theme == AppThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = "Toggle theme",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.authGlass)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.primary, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Telegram Drive", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Self-Hosted Secure Storage", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(32.dp))

            if (state.floodWait != null) {
                FloodWaitBlock(state.floodWait)
            } else {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "auth-step"
                ) { step ->
                    when (step) {
                        AuthStep.SETUP -> SetupStep(
                            state = state,
                            onApiIdChange = onApiIdChange,
                            onApiHashChange = onApiHashChange,
                            onSubmit = onSetupSubmit,
                            onShowHelp = { onShowHelp(true) },
                            onDevLogin = onDevLogin
                        )
                        AuthStep.PHONE -> PhoneStep(
                            state = state,
                            onPhoneChange = onPhoneChange,
                            onSubmit = onPhoneSubmit,
                            onStartQr = onStartQr,
                            onSwitchPhone = onSwitchPhone,
                            onRefreshQr = onRefreshQr,
                            onBack = onGoSetup
                        )
                        AuthStep.CODE -> CodeStep(
                            state = state,
                            onCodeChange = onCodeChange,
                            onSubmit = onCodeSubmit,
                            onBack = onGoPhone
                        )
                        AuthStep.PASSWORD -> PasswordStep(
                            state = state,
                            onPasswordChange = onPasswordChange,
                            onSubmit = onPasswordSubmit,
                            onBack = onGoCode
                        )
                    }
                }
            }

            if (state.error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AEF4444))
                        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Text(state.error, color = Color(0xFFF87171), fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(0.dp))
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onShowDonate(true) }) {
                    Icon(Icons.Outlined.Favorite, contentDescription = null, tint = Color(0xCCEF4444), modifier = Modifier.size(14.dp))
                    Text("Donate", color = colors.subtext, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        if (state.showHelp) {
            HelpDialog(onClose = { onShowHelp(false) }, onOpenPortal = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.telegram.org")))
            })
        }
        if (state.showDonate) {
            DonateDialog(onClose = { onShowDonate(false) }, onOpen = { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            })
        }
    }
}

@Composable
private fun FloodWaitBlock(seconds: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x33EF4444)),
            contentAlignment = Alignment.Center
        ) {
            Text("WAIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("Too Many Requests", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Telegram has temporarily limited your actions.", color = Color.Gray, fontSize = 14.sp)
        Text("Please wait before trying again.", color = Color.Gray, fontSize = 14.sp)
        Text(formatFloodWait(seconds), color = Color(0xFF60A5FA), fontSize = 48.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
        Text("Do not restart the app. The timer will reset if you do.", color = Color(0x99F87171), fontSize = 12.sp)
    }
}

@Composable
private fun SetupStep(
    state: AuthUiState,
    onApiIdChange: (String) -> Unit,
    onApiHashChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onShowHelp: () -> Unit,
    onDevLogin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AuthField("API ID", state.apiId, "12345678", KeyboardType.Number, onApiIdChange)
        AuthField("API HASH", state.apiHash, "abcdef123456...", KeyboardType.Ascii, onApiHashChange)
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Configure", fontWeight = FontWeight.Bold)
            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(16.dp))
        }
        TextButton(onClick = onShowHelp, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(12.dp))
            Text("How do I get my API credentials?", color = Color(0xFF93C5FD), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
        TextButton(onClick = onDevLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Dev Mode", color = Color(0x99F87171), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PhoneStep(
    state: AuthUiState,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onStartQr: () -> Unit,
    onSwitchPhone: () -> Unit,
    onRefreshQr: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            MethodTab("Phone Number", Icons.Outlined.Phone, state.loginMethod == LoginMethod.PHONE, Modifier.weight(1f), onSwitchPhone)
            MethodTab("QR Code", Icons.Outlined.QrCode, state.loginMethod == LoginMethod.QR, Modifier.weight(1f), onStartQr)
        }
        if (state.loginMethod == LoginMethod.PHONE) {
            AuthField("PHONE NUMBER", state.phone, "+1 234 567 8900", KeyboardType.Phone, onPhoneChange, leading = Icons.Outlined.Phone)
            Button(
                onClick = onSubmit,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.loading) Text("Connecting...", fontWeight = FontWeight.Bold)
                else {
                    Text("Continue", fontWeight = FontWeight.Bold)
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Configuration", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            QrBlock(state, onRefreshQr, onBack)
        }
    }
}

@Composable
private fun MethodTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(if (active) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (active) Color.White else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Text(
            label,
            color = if (active) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun QrBlock(state: AuthUiState, onRefresh: () -> Unit, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (state.loading && state.qrUrl == null) {
            Box(
                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF60A5FA), modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
        }
        state.qrUrl?.let { url ->
            val bitmap = remember(url) { qrBitmap(url) }
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp)) {
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(180.dp))
                }
            }
            Text("Scan with your Telegram app", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
            Text("Settings > Devices > Link Desktop Device", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            if (state.qrPolling) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    CircularProgressIndicator(color = Color(0xFF93C5FD), modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    Text("Waiting for scan...", color = Color(0xFF93C5FD), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
            TextButton(onClick = onRefresh) {
                Text("Refresh QR Code", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        TextButton(onClick = onBack) {
            Text("Back to Configuration", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CodeStep(
    state: AuthUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AuthField("TELEGRAM CODE", state.code, "1 2 3 4 5", KeyboardType.Number, onCodeChange)
        Button(
            onClick = onSubmit,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (state.loading) "Verifying..." else "Sign In", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Change Phone Number", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PasswordStep(
    state: AuthUiState,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Your account has Two-Factor Authentication enabled. Please enter your cloud password to continue.",
            color = Color(0xFF93C5FD),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1A3B82F6))
                .border(1.dp, Color(0x333B82F6), RoundedCornerShape(12.dp))
                .padding(12.dp)
        )
        AuthField(
            label = "CLOUD PASSWORD",
            value = state.password,
            placeholder = "Enter your password",
            keyboardType = KeyboardType.Password,
            onChange = onPasswordChange,
            leading = Icons.Outlined.Lock,
            password = true
        )
        Button(
            onClick = onSubmit,
            enabled = !state.loading && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (state.loading) "Verifying..." else "Unlock", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Code Entry", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
    leading: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Key,
    password: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            leadingIcon = { Icon(leading, contentDescription = null, tint = Color.White) },
            trailingIcon = if (password) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else null,
            singleLine = true,
            visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun HelpDialog(onClose: () -> Unit, onOpenPortal: () -> Unit) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 512.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .clickable(enabled = false) {}
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Getting Started", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.subtext)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Telegram Drive uses your Telegram account as secure cloud storage. You'll need a Telegram account and API credentials to get started.",
                color = colors.subtext,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.1f))
                    .padding(16.dp)
            )
            HelpStep(1, "Go to Telegram's Developer Portal", "Visit my.telegram.org and log in with your phone number.")
            HelpStep(2, "Create a New Application", "Click on \"API development tools\" and create a new application. Use any name and description you like.")
            HelpStep(3, "Copy Your Credentials", "After creating the app, you'll see your API ID (a number) and API Hash (a string). Copy both and paste them into the fields on the previous screen.")
            Text(
                "Privacy: Your credentials are stored locally on your device and are never sent to any third-party servers. All data goes directly between you and Telegram.",
                color = colors.subtext,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.hover)
                    .padding(16.dp)
            )
            Button(
                onClick = onOpenPortal,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Open my.telegram.org", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun HelpStep(number: Int, title: String, body: String) {
    val colors = TeleBoxTheme.colors
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
        Text(body, color = colors.subtext, fontSize = 14.sp, modifier = Modifier.padding(start = 32.dp, top = 8.dp))
    }
}

@Composable
private fun DonateDialog(onClose: () -> Unit, onOpen: (String) -> Unit) {
    val colors = TeleBoxTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("Support the Project", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.subtext)
                }
            }
            Text(
                "If you find Telegram Drive useful, consider supporting its development!",
                color = colors.subtext,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            DonateLink("Donate with PayPal", Color(0xFF003087)) { onOpen("https://www.paypal.me/Caamer20") }
            DonateLink("Donate LTC", Color(0xFF345D9D)) { onOpen("https://link.trustwallet.com/send?address=ltc1q6wkr5ac4u0pxx4hx7xgwn0gsaku25ws0df73rp&asset=c2") }
            DonateLink("Donate BTC", Color(0xFFF7931A)) { onOpen("https://link.trustwallet.com/send?asset=c0&address=bc1q5pt7m2fk6w0dzsnf6vvd5k6nw5k44785286ujy") }
        }
    }
}

@Composable
private fun DonateLink(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun qrBitmap(content: String): Bitmap? {
    return runCatching {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    }.getOrNull()
}
