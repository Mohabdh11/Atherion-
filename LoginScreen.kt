package com.aetherion.noc.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aetherion.noc.presentation.common.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Login Screen
// Developer: Mohammad Abdalftah Ibrahime
// Enterprise secure login with MFA-ready flow and biometric support.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val username  by viewModel.username.collectAsStateWithLifecycle()
    val password  by viewModel.password.collectAsStateWithLifecycle()
    val tenantId  by viewModel.tenantId.collectAsStateWithLifecycle()
    val mfaCode   by viewModel.mfaCode.collectAsStateWithLifecycle()
    val mfaRequired by viewModel.mfaRequired.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
    }

    AetherionNOCTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AetherionColors.SurfaceDark,
                            AetherionColors.SurfaceCard
                        )
                    )
                )
        ) {
            LoginContent(
                username    = username,
                password    = password,
                tenantId    = tenantId,
                mfaCode     = mfaCode,
                mfaRequired = mfaRequired,
                uiState     = uiState,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onTenantIdChange = viewModel::onTenantIdChange,
                onMfaCodeChange  = viewModel::onMfaCodeChange,
                onLoginClick     = viewModel::login,
                onClearError     = viewModel::clearError
            )
        }
    }
}

@Composable
private fun LoginContent(
    username: String,
    password: String,
    tenantId: String,
    mfaCode: String,
    mfaRequired: Boolean,
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTenantIdChange: (String) -> Unit,
    onMfaCodeChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading = uiState is LoginUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ── Logo / Brand ───────────────────────────────────────────
        Icon(
            imageVector = Icons.Outlined.NetworkCheck,
            contentDescription = "Aetherion NOC",
            tint = AetherionColors.AetherBlue,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "AETHERION",
            style = MaterialTheme.typography.headlineLarge,
            color = AetherionColors.TextPrimary
        )
        Text(
            text = "Network Operations Center",
            style = MaterialTheme.typography.bodyMedium,
            color = AetherionColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── Session Expired Banner ─────────────────────────────────
        AnimatedVisibility(visible = uiState is LoginUiState.SessionExpired) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AetherionColors.MajorDim
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LockClock,
                        contentDescription = null,
                        tint = AetherionColors.Major,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Session expired. Please log in again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherionColors.Major
                    )
                }
            }
        }

        // ── Error Banner ───────────────────────────────────────────
        AnimatedVisibility(visible = uiState is LoginUiState.Error) {
            val errorMsg = (uiState as? LoginUiState.Error)?.message ?: ""
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onClearError() },
                colors = CardDefaults.cardColors(containerColor = AetherionColors.CriticalDim)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = AetherionColors.Critical, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = AetherionColors.Critical)
                }
            }
        }

        // ── Tenant ID ──────────────────────────────────────────────
        OutlinedTextField(
            value = tenantId,
            onValueChange = onTenantIdChange,
            label = { Text("Tenant ID") },
            leadingIcon = { Icon(Icons.Outlined.Business, null) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = nocTextFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Username ───────────────────────────────────────────────
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = nocTextFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Password ───────────────────────────────────────────────
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                imeAction = if (mfaRequired) ImeAction.Next else ImeAction.Done,
                keyboardType = KeyboardType.Password
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus(); onLoginClick() }
            ),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = nocTextFieldColors()
        )

        // ── MFA Code ───────────────────────────────────────────────
        AnimatedVisibility(visible = mfaRequired) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = mfaCode,
                    onValueChange = onMfaCodeChange,
                    label = { Text("MFA Code") },
                    leadingIcon = { Icon(Icons.Outlined.Security, null) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus(); onLoginClick() }
                    ),
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = nocTextFieldColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Login Button ───────────────────────────────────────────
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && tenantId.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AetherionColors.AetherBlue
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AetherionColors.SurfaceDark,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Outlined.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Authenticate",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Aetherion v8 NOC — Zero Trust Secured",
            style = MaterialTheme.typography.bodySmall,
            color = AetherionColors.TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun nocTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AetherionColors.AetherBlue,
    unfocusedBorderColor = AetherionColors.SurfaceBorder,
    focusedLabelColor    = AetherionColors.AetherBlue,
    unfocusedLabelColor  = AetherionColors.TextSecondary,
    focusedTextColor     = AetherionColors.TextPrimary,
    unfocusedTextColor   = AetherionColors.TextPrimary,
    cursorColor          = AetherionColors.AetherBlue,
    focusedLeadingIconColor   = AetherionColors.AetherBlue,
    unfocusedLeadingIconColor = AetherionColors.TextSecondary,
    focusedTrailingIconColor  = AetherionColors.AetherBlue,
    unfocusedTrailingIconColor = AetherionColors.TextSecondary
)

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1E)
@Composable
private fun LoginScreenPreview() {
    AetherionNOCTheme {
        LoginContent(
            username = "", password = "", tenantId = "", mfaCode = "",
            mfaRequired = false, uiState = LoginUiState.Idle,
            onUsernameChange = {}, onPasswordChange = {},
            onTenantIdChange = {}, onMfaCodeChange = {},
            onLoginClick = {}, onClearError = {}
        )
    }
}
