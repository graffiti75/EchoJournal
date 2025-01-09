package br.android.cericatto.echojournal.ui.entries_list

import android.annotation.SuppressLint
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.android.cericatto.echojournal.navigation.Route
import br.android.cericatto.echojournal.ui.ObserveAsEvents
import br.android.cericatto.echojournal.ui.UiEvent
import kotlinx.coroutines.launch

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

	EntriesListScreen(
//		onAction = viewModel::onAction,
//		state = state,
		snackbarHostState = snackbarHostState
	)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EntriesListScreen(
//	onAction: (HomeAction) -> Unit,
//	state: HomeState,
	snackbarHostState: SnackbarHostState,
) {
}