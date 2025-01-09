package br.android.cericatto.echojournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.android.cericatto.echojournal.navigation.NavHostComposable
import br.android.cericatto.echojournal.ui.theme.EchoJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			EchoJournalTheme {
				NavHostComposable()
			}
		}
	}
}