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
import com.example.core.i18n.LocalAppStrings
import com.example.core.security.AuthValidator
import com.example.domain.model.AppSettingsEntity
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentButtonVariant
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentTextField
import com.example.ui.theme.*

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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Spacer(modifier = Modifier.height(24.dp))

    Surface(
      color = Gold500.copy(alpha = 0.15f),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.size(72.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = "Tournament Logo",
          tint = Gold400,
          modifier = Modifier.size(42.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "AD TOURNAMENT",
      style = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
      ),
      color = Color.White,
    )
    Text(
      text = strings.appTagline,
      style = MaterialTheme.typography.bodySmall,
      color = Gold400,
    )

    Spacer(modifier = Modifier.height(28.dp))

    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      Text(
        text = strings.playerSignIn,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
      Text(
        text = strings.playerSignInSubtitle,
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
        modifier = Modifier.padding(bottom = 16.dp),
      )

      val displayError = clientError ?: uiState.errorMessage?.let { rawError ->
        when {
          rawError.contains("Mobile number cannot be empty", ignoreCase = true) -> strings.phoneEmptyError
          rawError.contains("valid 11-digit", ignoreCase = true) || rawError.contains("Invalid mobile number", ignoreCase = true) -> strings.invalidPhoneError
          rawError.contains("Password cannot be empty", ignoreCase = true) -> strings.passwordEmptyError
          rawError.contains("at least 6 characters", ignoreCase = true) -> strings.passwordMinLengthError
          rawError.contains("Invalid mobile number or password", ignoreCase = true) || rawError.contains("Invalid password", ignoreCase = true) -> strings.invalidCredentialsError
          rawError.contains("No account registered", ignoreCase = true) -> strings.accountNotFoundError
          rawError.contains("Network", ignoreCase = true) -> strings.offlineMode
          else -> rawError
        }
      }

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
        value = mobileNumber,
        onValueChange = {
          if (it.length <= 11) {
            mobileNumber = it
            clientError = null
            viewModel.clearMessages()
          }
        },
        label = strings.phoneNumber,
        placeholder = "01XXXXXXXXX",
        leadingIcon = Icons.Default.Phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        testTag = "login_mobile_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      TournamentTextField(
        value = password,
        onValueChange = {
          password = it
          clientError = null
          viewModel.clearMessages()
        },
        label = strings.password,
        placeholder = strings.enterPassword,
        leadingIcon = Icons.Default.Lock,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = strings.togglePassword,
              tint = Slate400,
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
            style = MaterialTheme.typography.labelSmall,
            color = Gold400,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      TournamentButton(
        text = strings.signInButton,
        onClick = {
          clientError = null
          val phoneVal = AuthValidator.validateMobileNumber(mobileNumber)
          if (!phoneVal.isValid) {
            clientError = when (phoneVal.errorMessage) {
              "Mobile number cannot be empty" -> strings.phoneEmptyError
              else -> strings.invalidPhoneError
            }
            return@TournamentButton
          }
          val passVal = AuthValidator.validatePassword(password)
          if (!passVal.isValid) {
            clientError = when (passVal.errorMessage) {
              "Password cannot be empty" -> strings.passwordEmptyError
              else -> strings.passwordMinLengthError
            }
            return@TournamentButton
          }
          viewModel.login(mobileNumber, password)
        },
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
        testTag = "login_button",
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = strings.noAccountPrompt,
        style = MaterialTheme.typography.bodyMedium,
        color = Slate400,
      )
      TextButton(onClick = onNavigateToRegister) {
        Text(
          text = strings.createAccountAction,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = Gold400,
        )
      }
    }
  }
}

@Composable
fun RegisterScreen(
  viewModel: AuthViewModel,
  onRegisterSuccess: () -> Unit,
  onNavigateToLogin: () -> Unit,
) {
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
      color = Gold500.copy(alpha = 0.15f),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.size(64.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.PersonAdd,
          contentDescription = "Register",
          tint = Gold400,
          modifier = Modifier.size(36.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Create Player Account",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = Color.White,
    )
    Text(
      text = "Join AD TOURNAMENT and compete in 1v1 battles",
      style = MaterialTheme.typography.bodySmall,
      color = Slate400,
    )

    Spacer(modifier = Modifier.height(24.dp))

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
        value = name,
        onValueChange = {
          name = it
          clientError = null
          viewModel.clearMessages()
        },
        label = "Full Name",
        placeholder = "e.g. Shakib Al Hasan",
        leadingIcon = Icons.Default.Person,
        testTag = "register_name_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      TournamentTextField(
        value = mobileNumber,
        onValueChange = {
          if (it.length <= 11) {
            mobileNumber = it
            clientError = null
            viewModel.clearMessages()
          }
        },
        label = "Bangladesh Mobile Number",
        placeholder = "01XXXXXXXXX (11 digits)",
        helperText = "Must start with 013, 014, 015, 016, 017, 018, or 019",
        leadingIcon = Icons.Default.Phone,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        testTag = "register_mobile_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      TournamentTextField(
        value = password,
        onValueChange = {
          password = it
          clientError = null
          viewModel.clearMessages()
        },
        label = "Password",
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
        testTag = "register_password_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      TournamentTextField(
        value = confirmPassword,
        onValueChange = {
          confirmPassword = it
          clientError = null
          viewModel.clearMessages()
        },
        label = "Confirm Password",
        placeholder = "Re-enter your password",
        leadingIcon = Icons.Default.LockReset,
        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
            Icon(
              imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Toggle confirm password",
              tint = Slate400,
            )
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        testTag = "register_confirm_password_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      TournamentTextField(
        value = referralCode,
        onValueChange = {
          if (it.length <= 10) {
            referralCode = it.uppercase()
            clientError = null
            viewModel.clearMessages()
          }
        },
        label = "রেফার কোড (ঐচ্ছিক / Optional)",
        placeholder = "e.g. ABC123",
        leadingIcon = Icons.Default.CardGiftcard,
        testTag = "register_referral_input",
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Checkbox(
          checked = agreedToTerms,
          onCheckedChange = { agreedToTerms = it },
          colors = CheckboxDefaults.colors(
            checkedColor = Gold500,
            checkmarkColor = Slate950,
            uncheckedColor = Slate600,
          ),
        )
        Text(
          text = "I agree to fair-play rules & platform terms",
          style = MaterialTheme.typography.bodySmall,
          color = Slate300,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      TournamentButton(
        text = "CREATE ACCOUNT",
        onClick = {
          clientError = null
          val nameVal = AuthValidator.validateFullName(name)
          if (!nameVal.isValid) {
            clientError = nameVal.errorMessage
            return@TournamentButton
          }
          val phoneVal = AuthValidator.validateMobileNumber(mobileNumber)
          if (!phoneVal.isValid) {
            clientError = phoneVal.errorMessage
            return@TournamentButton
          }
          val passVal = AuthValidator.validatePassword(password)
          if (!passVal.isValid) {
            clientError = passVal.errorMessage
            return@TournamentButton
          }
          if (password != confirmPassword) {
            clientError = "Passwords do not match. Please re-enter."
            return@TournamentButton
          }
          if (!agreedToTerms) {
            clientError = "Please agree to the tournament fair-play terms."
            return@TournamentButton
          }
          viewModel.register(
            name = name, 
            mobileNumber = mobileNumber, 
            password = password, 
            referralCode = referralCode
          )
        },
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
        testTag = "register_button",
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "Already have an account?",
        style = MaterialTheme.typography.bodyMedium,
        color = Slate400,
      )
      TextButton(onClick = onNavigateToLogin) {
        Text(
          text = "Sign In",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = Gold400,
        )
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
        text = "পাসওয়ার্ড রিসেট ও সহায়তা",
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
        text = "অ্যাকাউন্ট রিকভারি সহায়তা",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Gold400,
      )
      Text(
        text = "অ্যাকাউন্টের সুরক্ষার জন্য সরাসরি অফিসিয়াল সাপোর্টে যোগাযোগ করে আপনার নতুন পাসওয়ার্ড সেট করে নিন। আমাদের এডমিন প্যানেল আপনাকে দ্রুত সহায়তা করবে।",
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        lineHeight = 18.sp
      )

      // 1. WhatsApp Support Button
      SupportContactButton(
        title = "WhatsApp-এ মেসেজ দিন",
        subtitle = "অফিসিয়াল অ্যাডমিন সাপোর্ট",
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
        title = "Telegram-এ মেসেজ দিন",
        subtitle = "অফিসিয়াল টেলিগ্রাম চ্যানেল বা চ্যাট",
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
        text = "লগইন স্ক্রিনে ফিরে যান",
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
