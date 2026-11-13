package com.example.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.AuthValidator
import com.example.domain.model.AppSettingsEntity
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentButtonVariant
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentTextField
import com.example.ui.theme.*

private fun convertBengaliDigitsToEnglish(input: String): String {
  return input.map { ch ->
    when (ch) {
      '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
      '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
      else -> ch
    }
  }.joinToString("")
}

@Composable
fun LoginScreen(
  viewModel: AuthViewModel,
  onLoginSuccess: () -> Unit,
  onNavigateToRegister: () -> Unit,
  onNavigateToForgotPassword: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()

  var mobileNumber by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var clientError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(uiState.isLoginSuccess) {
    if (uiState.isLoginSuccess && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
      viewModel.clearSuccessFlags()
      onLoginSuccess()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AdAuthBackground(modifier = Modifier.fillMaxSize())

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .imePadding()
        .systemBarsPadding()
        .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      AdAuthLogo()

      Spacer(modifier = Modifier.height(4.dp))

      AdAuthTitle(tagline = strings.appTagline)

      Spacer(modifier = Modifier.height(22.dp))

      AdAuthCard(modifier = Modifier.fillMaxWidth()) {
        AdAuthCardHeader(
          title = strings.playerSignIn,
          subtitle = strings.playerSignInSubtitle,
        )

        val displayError = clientError ?: uiState.errorMessage?.let { rawError ->
          when {
            rawError.contains("Invalid mobile number or password", ignoreCase = true) ||
            rawError.contains("Invalid password", ignoreCase = true) ||
            rawError.contains("No account registered", ignoreCase = true) ||
            rawError.contains("User account not found", ignoreCase = true) ||
            rawError.contains("User profile not found", ignoreCase = true) -> strings.invalidCredentialsError
            rawError.contains("Mobile number cannot be empty", ignoreCase = true) -> strings.phoneEmptyError
            rawError.contains("valid 11-digit", ignoreCase = true) || rawError.contains("Invalid mobile number", ignoreCase = true) -> strings.invalidPhoneError
            rawError.contains("Password cannot be empty", ignoreCase = true) -> strings.passwordEmptyError
            rawError.contains("at least 6 characters", ignoreCase = true) -> strings.passwordMinLengthError
            rawError.contains("Too many failed attempts", ignoreCase = true) || rawError.contains("Too many attempts", ignoreCase = true) -> strings.tooManyAttemptsError
            rawError.contains("banned", ignoreCase = true) || rawError.contains("suspended", ignoreCase = true) -> strings.accountSuspendedError
            rawError.contains("Network", ignoreCase = true) || rawError.contains("timed out", ignoreCase = true) -> strings.offlineMode
            else -> rawError
          }
        }

        if (displayError != null) {
          AdAuthErrorBox(text = displayError)
        }

        AdAuthTextField(
          value = mobileNumber,
          onValueChange = {
            val cleaned = convertBengaliDigitsToEnglish(it)
            if (cleaned.length <= 11) {
              mobileNumber = cleaned
              clientError = null
              viewModel.clearMessages()
            }
          },
          placeholder = strings.phoneNumber,
          leadingIcon = Icons.Default.Phone,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          testTag = "login_mobile_input",
        )

        AdAuthTextField(
          value = password,
          onValueChange = {
            password = it
            clientError = null
            viewModel.clearMessages()
          },
          placeholder = strings.enterPassword,
          leadingIcon = Icons.Default.Lock,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailing = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = strings.togglePassword,
                tint = Color(0xFFFFC94A),
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          testTag = "login_password_input",
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onNavigateToForgotPassword) {
            Text(
              text = strings.forgotPasswordLink,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFFFFD27A),
            )
          }
        }

        AdAuthGoldButton(
          text = strings.signInButton,
          loadingText = strings.authPleaseWait,
          isLoading = uiState.isLoading,
          onClick = {
            clientError = null
            val phoneVal = AuthValidator.validateMobileNumber(mobileNumber)
            if (!phoneVal.isValid) {
              clientError = when (phoneVal.errorMessage) {
                "Mobile number cannot be empty" -> strings.phoneEmptyError
                else -> strings.invalidPhoneError
              }
              return@AdAuthGoldButton
            }
            val passVal = AuthValidator.validatePassword(password)
            if (!passVal.isValid) {
              clientError = when (passVal.errorMessage) {
                "Password cannot be empty" -> strings.passwordEmptyError
                else -> strings.passwordMinLengthError
              }
              return@AdAuthGoldButton
            }
            viewModel.login(mobileNumber, password)
          },
          testTag = "login_button",
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = strings.noAccountPrompt,
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFE8D7A8),
        )
        TextButton(onClick = onNavigateToRegister) {
          Text(
            text = strings.createAccountAction,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFFFD45A),
          )
        }
      }
    }
  }
}

@Composable
fun RegisterScreen(
  viewModel: AuthViewModel,
  onRegisterSuccess: () -> Unit,
  onNavigateToLogin: () -> Unit,
  onOpenRules: () -> Unit = {},
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()

  var name by remember { mutableStateOf("") }
  var mobileNumber by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }
  var agreedToTerms by remember { mutableStateOf(true) }
  var referralCode by remember { mutableStateOf("") }
  var clientError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(uiState.isRegistrationSuccess) {
    if (uiState.isRegistrationSuccess && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
      viewModel.clearSuccessFlags()
      onRegisterSuccess()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AdAuthBackground(modifier = Modifier.fillMaxSize())

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .imePadding()
        .systemBarsPadding()
        .padding(horizontal = 22.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AdAuthRegisterHeader(title = strings.authRegisterTitle, subtitle = strings.authRegisterSubtitle)

      Spacer(modifier = Modifier.height(20.dp))

      AdAuthCard {
        val displayError = (clientError ?: uiState.errorMessage)?.let { adRegisterErrorText(it, strings) }
        if (displayError != null) {
          AdAuthErrorBox(text = displayError)
        }

        AdAuthTextField(
          value = name,
          onValueChange = {
            name = it
            clientError = null
            viewModel.clearMessages()
          },
          placeholder = strings.authFullNameLabel,
          leadingIcon = Icons.Default.Person,
          testTag = "register_name_input",
        )

        Column {
          AdAuthTextField(
            value = mobileNumber,
            onValueChange = {
              val cleaned = convertBengaliDigitsToEnglish(it)
              if (cleaned.length <= 11) {
                mobileNumber = cleaned
                clientError = null
                viewModel.clearMessages()
              }
            },
            placeholder = strings.authMobileNumberLabel,
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            testTag = "register_mobile_input",
          )
          Spacer(modifier = Modifier.height(7.dp))
          AdAuthHelperText(text = strings.authMobileNumberHelperText)
        }

        AdAuthTextField(
          value = password,
          onValueChange = {
            password = it
            clientError = null
            viewModel.clearMessages()
          },
          placeholder = strings.password,
          leadingIcon = Icons.Default.Lock,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailing = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = strings.togglePassword,
                tint = Color(0xFFE8C88A),
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          testTag = "register_password_input",
        )

        AdAuthTextField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            clientError = null
            viewModel.clearMessages()
          },
          placeholder = strings.authConfirmPasswordLabel,
          leadingIcon = Icons.Default.LockReset,
          visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailing = {
            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
              Icon(
                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = strings.authToggleConfirmPassword,
                tint = Color(0xFFE8C88A),
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          testTag = "register_confirm_password_input",
        )

        AdAuthTextField(
          value = referralCode,
          onValueChange = {
            if (it.length <= 10) {
              referralCode = it.uppercase()
              clientError = null
              viewModel.clearMessages()
            }
          },
          placeholder = strings.authReferralCodeLabel,
          leadingIcon = Icons.Default.CardGiftcard,
          testTag = "register_referral_input",
        )

        AdAuthTermsCheck(
          checked = agreedToTerms,
          onCheckedChange = { agreedToTerms = it },
          text = strings.authFairPlayTermsText,
          highlight = if (strings === BengaliStrings) "ফেয়ার-প্লে শর্তাবলীতে" else "fair-play terms",
          onHighlightClick = onOpenRules,
        )

        AdAuthGoldButton(
          text = strings.registerButton,
          loadingText = strings.authPleaseWait,
          onClick = {
            clientError = null
            val nameVal = AuthValidator.validateFullName(name)
            if (!nameVal.isValid) {
              clientError = nameVal.errorMessage
              return@AdAuthGoldButton
            }
            val phoneVal = AuthValidator.validateMobileNumber(mobileNumber)
            if (!phoneVal.isValid) {
              clientError = phoneVal.errorMessage
              return@AdAuthGoldButton
            }
            val passVal = AuthValidator.validatePassword(password)
            if (!passVal.isValid) {
              clientError = passVal.errorMessage
              return@AdAuthGoldButton
            }
            if (password != confirmPassword) {
              clientError = strings.authPasswordsMismatchError
              return@AdAuthGoldButton
            }
            if (!agreedToTerms) {
              clientError = strings.authAgreeToTermsError
              return@AdAuthGoldButton
            }
            viewModel.register(
              name = name, 
              mobileNumber = mobileNumber, 
              password = password, 
              referralCode = referralCode
            )
          },
          isLoading = uiState.isLoading,
          testTag = "register_button",
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = strings.authAlreadyHaveAccountPrompt,
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
          color = Color(0xFFC9D3FF),
        )
        TextButton(onClick = onNavigateToLogin) {
          Text(
            text = strings.authSignInAction,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = Color(0xFFFFC94A),
          )
        }
      }
    }
  }
}

@Composable
fun ForgotPasswordScreen(
  viewModel: AuthViewModel,
  onNavigateToOtp: (String) -> Unit,
  onNavigateBack: () -> Unit,
  appSettings: AppSettingsEntity? = null,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onNavigateBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Text(
        text = strings.authForgotPasswordHeader,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      Text(
        text = strings.authForgotPasswordCardTitle,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Gold400,
      )
      Text(
        text = strings.authForgotPasswordCardDesc,
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        lineHeight = 18.sp
      )

      // 1. WhatsApp Support Button
      SupportContactButton(
        title = strings.authWhatsappSupportTitle,
        subtitle = strings.authWhatsappSupportSubtitle,
        icon = Icons.Default.Chat,
        gradientColors = listOf(Color(0xFF16A34A), Color(0xFF15803D)),
        onClick = {
          val phone = appSettings?.activeWhatsappNumber?.replace("+", "")?.replace(" ", "")?.ifBlank { "8801700000000" } ?: "8801700000000"
          val msg = "Hello Admin, I forgot my password in AD Tournament. Please help me reset it."
          val url = "https://wa.me/$phone?text=${Uri.encode(msg)}"
          try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          } catch (_: Exception) {}
        }
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Telegram Support Button
      SupportContactButton(
        title = strings.authTelegramSupportTitle,
        subtitle = strings.authTelegramSupportSubtitle,
        icon = Icons.Default.Send,
        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
        onClick = {
          val rawTelegram = appSettings?.activeTelegramUrl.orEmpty()
          val url = if (rawTelegram.startsWith("http")) rawTelegram else "https://t.me/${rawTelegram.removePrefix("@")}"
          val finalUrl = url.ifBlank { "https://t.me/adtournament" }
          try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
          } catch (_: Exception) {}
        }
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    TextButton(onClick = onNavigateBack) {
      Text(
        text = strings.authBackToLoginAction,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = Gold400,
      )
    }
  }
}

@Composable
private fun SupportContactButton(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  gradientColors: List<Color>,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() },
    color = Color.Transparent,
  ) {
    Row(
      modifier = Modifier
        .background(Brush.horizontalGradient(gradientColors))
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        modifier = Modifier.size(40.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
      }
      
      Spacer(modifier = Modifier.width(14.dp))
      
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = Color.White,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.8f),
        )
      }
      
      Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
    }
  }
}

@Composable
fun OtpScreen(
  phoneNumber: String,
  viewModel: AuthViewModel,
  onOtpVerified: (String, String) -> Unit,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  var otp by remember { mutableStateOf("") }

  LaunchedEffect(uiState.isOtpVerified) {
    if (uiState.isOtpVerified) {
      onOtpVerified(phoneNumber, otp)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onNavigateBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Text(
        text = "Verify Code",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      Text(
        text = "Verification Code Sent",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
      Text(
        text = "Enter the 6-digit code sent to $phoneNumber",
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
      )

      if (uiState.errorMessage != null) {
        Surface(
          color = Rose900.copy(alpha = 0.4f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        ) {
          Text(
            text = uiState.errorMessage ?: "",
            color = Rose400,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      TournamentTextField(
        value = otp,
        onValueChange = {
          if (it.length <= 6) {
            otp = it
            viewModel.clearMessages()
          }
        },
        label = "6-Digit Code",
        placeholder = "123456",
        leadingIcon = Icons.Default.Security,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        testTag = "otp_input",
      )

      Spacer(modifier = Modifier.height(20.dp))

      TournamentButton(
        text = "VERIFY CODE",
        onClick = { viewModel.verifyOtp(phoneNumber, otp) },
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
        testTag = "verify_otp_button",
      )
    }
  }
}

@Composable
fun ResetPasswordScreen(
  phoneNumber: String,
  otp: String,
  viewModel: AuthViewModel,
  onResetSuccess: () -> Unit,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var clientError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(uiState.isPasswordResetSuccess) {
    if (uiState.isPasswordResetSuccess) {
      viewModel.clearSuccessFlags()
      onResetSuccess()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onNavigateBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Text(
        text = "Create New Password",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      val displayError = clientError ?: uiState.errorMessage
      if (displayError != null) {
        Surface(
          color = Rose900.copy(alpha = 0.4f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        ) {
          Text(
            text = displayError,
            color = Rose400,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      TournamentTextField(
        value = newPassword,
        onValueChange = {
          newPassword = it
          clientError = null
          viewModel.clearMessages()
        },
        label = "New Password",
        placeholder = "Minimum 6 characters",
        leadingIcon = Icons.Default.Lock,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Toggle password",
              tint = Slate400,
            )
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        testTag = "reset_new_password_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      TournamentTextField(
        value = confirmPassword,
        onValueChange = {
          confirmPassword = it
          clientError = null
          viewModel.clearMessages()
        },
        label = "Confirm New Password",
        placeholder = "Re-enter new password",
        leadingIcon = Icons.Default.LockReset,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        testTag = "reset_confirm_password_input",
      )

      Spacer(modifier = Modifier.height(20.dp))

      TournamentButton(
        text = "SAVE NEW PASSWORD",
        onClick = {
          clientError = null
          val passVal = AuthValidator.validatePassword(newPassword)
          if (!passVal.isValid) {
            clientError = passVal.errorMessage
            return@TournamentButton
          }
          if (newPassword != confirmPassword) {
            clientError = "Passwords do not match."
            return@TournamentButton
          }
          viewModel.resetPassword(phoneNumber, otp, newPassword)
        },
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
        testTag = "reset_password_button",
      )
    }
  }
}
