package dev.openvw.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.theme.Bg
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted
import dev.openvw.theme.Red

@Composable
fun LoginScreen(vm: AppViewModel) {
  Column(
    Modifier.fillMaxSize().background(Bg).statusBarsPadding().imePadding().padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.Start,
  ) {
    Spacer(Modifier.height(56.dp))
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).border(2.dp, Ink, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
      Text("VW", style = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
    }
    Spacer(Modifier.height(20.dp))
    Text("Welcome to\nOpenVW", style = TextStyle(color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.4).sp, lineHeight = 44.sp))
    Spacer(Modifier.height(10.dp))
    Text(
      if (vm.phase == Phase.OTP) "Enter the 6-digit code sent to your phone by SMS."
      else "Sign in with your mobile number to connect your Volkswagen.",
      style = TextStyle(color = Muted, fontSize = 15.sp, lineHeight = 21.sp),
    )

    Spacer(Modifier.weight(1f))

    BackHandler(enabled = vm.phase == Phase.OTP) { vm.backToPhone() }
    if (vm.phase == Phase.OTP) OtpStep(vm) else PhoneStep(vm)

    vm.message?.let { Text(it, style = TextStyle(color = if (it == "Code re-sent") Muted else Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 12.dp)) }
    Spacer(Modifier.height(28.dp))
  }
}

@Composable
private fun PhoneStep(vm: AppViewModel) {
  var p by remember { mutableStateOf(vm.phone) }
  Text("MOBILE NUMBER", style = label())
  Spacer(Modifier.height(8.dp))
  Row(
    Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("+91", style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold))
    Spacer(Modifier.width(12.dp))
    BasicTextField(
      value = p,
      onValueChange = { p = it.filter { c -> c.isDigit() }.take(10) },
      textStyle = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      modifier = Modifier.weight(1f),
      decorationBox = { inner -> if (p.isEmpty()) Text("00000 00000", style = TextStyle(color = Muted, fontSize = 20.sp)); inner() },
    )
  }
  Spacer(Modifier.height(20.dp))
  CtaButton(if (vm.busy) "Sending OTP…" else "Send OTP", enabled = !vm.busy && p.length == 10) { vm.startLogin(p) }
}

@Composable
private fun OtpStep(vm: AppViewModel) {
  var code by remember { mutableStateOf("") }
  BasicTextField(
    value = code,
    onValueChange = {
      code = it.filter { c -> c.isDigit() }.take(6)
      if (code.length == 6) vm.verify(code)
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    modifier = Modifier.fillMaxWidth(),
    decorationBox = {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(6) { i ->
          val ch = code.getOrNull(i)?.toString() ?: ""
          Box(
            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp)).background(Card)
              .border(if (code.length == i) 2.dp else 1.dp, if (code.length == i) Ink else Border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
          ) { Text(ch, style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)) }
        }
      }
    },
  )
  Spacer(Modifier.height(20.dp))
  CtaButton(if (vm.busy) "Verifying…" else "Verify & continue", enabled = !vm.busy && code.length == 6) { vm.verify(code) }
  Spacer(Modifier.height(8.dp))
  Box(Modifier.fillMaxWidth().clickable { vm.resend() }.padding(12.dp), contentAlignment = Alignment.Center) {
    Text("Resend code", style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
  }
}

@Composable
private fun CtaButton(text: String, enabled: Boolean, onClick: () -> Unit) {
  Box(
    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
      .background(if (enabled) Ink else Border).let { if (enabled) it.clickable(onClick = onClick) else it },
    contentAlignment = Alignment.Center,
  ) {
    Text(text, style = TextStyle(color = if (enabled) Card else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold))
  }
}
