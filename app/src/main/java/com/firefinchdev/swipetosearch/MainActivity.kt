// ... existing code ...
package com.firefinchdev.swipetosearch

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firefinchdev.swipetosearch.ui.theme.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openDialog = remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Auto Keyboard Opener",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = stringResource(R.string.app_description),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        )

        Text(
            text = stringResource(R.string.accessibility_settings_instruction),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        )

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = stringResource(R.string.open_accessibility_settings))
        }

        Text(
            text = stringResource(R.string.accessibility_permission_trouble),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable {
                    openDialog.value = true
                },
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,

            )

        if (openDialog.value) {
            HelpDialog(
                onDismissRequest = { openDialog.value = false },
                dialogTitle = stringResource(R.string.help_dialog_title)
            )

        }
    }
}

@Composable
fun HelpDialog(
    onDismissRequest: () -> Unit,
    dialogTitle: String
) {
    AlertDialog(
        title = {
            Text(text = dialogTitle)
        },
        text = {
            SelectionContainer {

                Text(

                    buildAnnotatedString {
                        append("If you are a OnePlus user, a bug present in OxygenOS is likely the cause. Check for OS updates, and if that doesn't resolve the issue, try the following steps:\n\n")

                        append("1. Download ")
                        withLink(
                            LinkAnnotation.Url(
                                "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api",
                                TextLinkStyles(style = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                            )
                        ) {
                            append("Shizuku")
                        }
                        append(" and run it.\n\n")

                        append("2. Download ")
                        withLink(
                            LinkAnnotation.Url(
                                "https://github.com/zacharee/InstallWithOptions/releases/",
                                TextLinkStyles(style = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                            )
                        ) {
                            append("Install With Options")
                        }
                        append(", open it, and grant it access to Shizuku.\n\n")

                        append("3. Scroll down and, in the \"Installer Package\" field, type: \n")
                        withStyle(
                            SpanStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        )
                        {
                            append("com.android.vending\n\n")
                        }
                        append("4. Click on \"Choose files\" and select the APK file of this app, then proceed with the installation.\n\n")

                        append("5. Open the app; you should now be able to enable the permission. You can uninstall the other apps afterward.")
                    },
                    fontSize = 12.sp)
            }

        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Confirm")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        MainScreen()
    }
}