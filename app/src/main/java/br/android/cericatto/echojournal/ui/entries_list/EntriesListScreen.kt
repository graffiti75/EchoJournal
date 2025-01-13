package br.android.cericatto.echojournal.ui.entries_list

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.android.cericatto.echojournal.R
import br.android.cericatto.echojournal.navigation.Route
import br.android.cericatto.echojournal.ui.LocalSpacing
import br.android.cericatto.echojournal.ui.ObserveAsEvents
import br.android.cericatto.echojournal.ui.UiEvent
import br.android.cericatto.echojournal.ui.theme.entriesListBackground
import br.android.cericatto.echojournal.ui.theme.splashBlue
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun EntriesListScreenRoot(
	onNavigate: (Route) -> Unit,
	onNavigateUp: () -> Unit,
	viewModel: EntriesListViewModel = hiltViewModel()
) {
	val state by viewModel.state.collectAsStateWithLifecycle()

	val scope = rememberCoroutineScope()
	val snackbarHostState = remember { SnackbarHostState() }
	val context = LocalContext.current
	ObserveAsEvents(viewModel.events) { event ->
		when (event) {
			is UiEvent.ShowSnackbar -> {
				scope.launch {
					snackbarHostState.showSnackbar(
						message = event.message.asString(context)
					)
				}
			}
			is UiEvent.Navigate -> onNavigate(event.route)
			is UiEvent.NavigateUp -> onNavigateUp()
			else -> Unit
		}
	}

	RequestRecordAudioPermission(viewModel = viewModel)

	EntriesListScreen(
		onAction = viewModel::onAction,
		state = state,
		snackbarHostState = snackbarHostState
	)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EntriesListScreen(
	onAction: (EntriesListAction) -> Unit,
	state: EntriesListState,
	snackbarHostState: SnackbarHostState,
) {
	val spacing = LocalSpacing.current
	val context = LocalContext.current
	val audioFile = File(context.cacheDir, "recorded_audio.wav")
	if (state.isLoading) {
		Box(
			modifier = Modifier
				.padding(vertical = 20.dp)
				.fillMaxSize()
				.background(Color.White),
			contentAlignment = Alignment.Center
		) {
			CircularProgressIndicator(
				color = MaterialTheme.colorScheme.primary,
				strokeWidth = 4.dp,
				modifier = Modifier.size(64.dp)
			)
		}
	} else {
		val contentDescription = ""
		Scaffold(
			snackbarHost = {
				SnackbarHost(hostState = snackbarHostState)
			},
			floatingActionButton = {
				FloatingActionButton(
					onClick = {
						// TODO
					},
					containerColor = splashBlue,
					contentColor = Color.White
				) {
					Icon(Icons.Filled.Add, contentDescription)
				}
			}
//		) { innerPadding ->
		) { _ ->
			AudioPlayer(
				state = state,
				onAction = onAction,
				file = audioFile,
				modifier = Modifier
					.padding(vertical = 40.dp),
			)
			/*
			EntriesListMainContent(
				onAction = onAction,
				modifier = Modifier.background(entriesListBackground)
					.padding(vertical = spacing.spaceMediumLarge),
//					.padding(innerPadding),
				state = state
			)
			 */
		}
	}
}

@Composable
fun EntriesListMainContent(
	onAction: (EntriesListAction) -> Unit,
	modifier: Modifier,
	state: EntriesListState
) {
	Column(
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier
			.fillMaxSize()
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Start,
			modifier = modifier
//				.background(Color.Yellow)
				.fillMaxWidth()
				.padding(20.dp)
		) {
			Text(
				text = "Your EchoJournal",
				style = TextStyle(
					color = Color.Black,
					fontStyle = FontStyle.Normal,
					fontSize = 20.sp
				),
				textAlign = TextAlign.Start
			)
		}
		Column(
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = modifier
//				.background(Color.Red)
				.fillMaxSize()
		) {
			Image(
				painter = painterResource(id = R.drawable.no_entries),
				contentDescription = null
			)
			Text(
				text = "No Entries",
				style = TextStyle(
					color = Color.Black,
					fontStyle = FontStyle.Normal,
					fontSize = 20.sp
				)
			)
			Text(
				text = "Start recording your first Echo",
				style = TextStyle(
					color = Color.Gray,
					fontStyle = FontStyle.Normal,
					fontSize = 14.sp
				),
				modifier = Modifier.padding(vertical = 5.dp)
			)
		}
	}
}

@Preview
@Composable
private fun HomeScreenPreview() {
	EntriesListScreen(
		onAction = {},
		snackbarHostState = SnackbarHostState(),
		state = EntriesListState()
	)
}

@Preview
@Composable
private fun HomeMainContentPreview() {
	EntriesListMainContent(
		onAction = {},
		modifier = Modifier,
		state = EntriesListState()
	)
}