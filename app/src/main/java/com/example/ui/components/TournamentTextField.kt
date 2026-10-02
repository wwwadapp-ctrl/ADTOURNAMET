package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun TournamentTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  errorMessage: String? = null,
  helperText: String? = null,
  enabled: Boolean = true,
  singleLine: Boolean = true,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = "tournament_text_field",
) {
  val isError = !errorMessage.isNullOrBlank()
  Column(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier
        .fillMaxWidth()
        .testTag(testTag),
      enabled = enabled,
      singleLine = singleLine,
      isError = isError,
      label = { Text(label) },
      placeholder = if (placeholder.isNotBlank()) {
        { Text(placeholder, color = Slate600) }
      } else null,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      visualTransformation = visualTransformation,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold500,
        unfocusedBorderColor = Slate700,
        focusedLabelColor = Gold500,
        unfocusedLabelColor = Slate400,
        focusedTextColor = Slate50,
        unfocusedTextColor = Slate100,
        errorBorderColor = Rose500,
        errorLabelColor = Rose500,
        cursorColor = Gold500,
        focusedContainerColor = Slate850,
        unfocusedContainerColor = Slate900,
        disabledContainerColor = Slate950,
      ),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
    )
    if (isError && !errorMessage.isNullOrBlank()) {
      Text(
        text = errorMessage,
        color = Rose500,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
      )
    } else if (!helperText.isNullOrBlank()) {
      Text(
        text = helperText,
        color = Slate400,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
      )
    }
  }
}

@Composable
fun TournamentTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  leadingIcon: ImageVector,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  errorMessage: String? = null,
  helperText: String? = null,
  enabled: Boolean = true,
  singleLine: Boolean = true,
  trailingIcon: @Composable (() -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = "tournament_text_field",
) {
  TournamentTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    placeholder = placeholder,
    errorMessage = errorMessage,
    helperText = helperText,
    enabled = enabled,
    singleLine = singleLine,
    leadingIcon = {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = Gold400,
      )
    },
    trailingIcon = trailingIcon,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    testTag = testTag,
  )
}
