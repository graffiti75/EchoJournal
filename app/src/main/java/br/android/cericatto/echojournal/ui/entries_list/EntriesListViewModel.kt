package br.android.cericatto.echojournal.ui.entries_list

import androidx.lifecycle.ViewModel
import br.android.cericatto.echojournal.ui.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class EntriesListViewModel @Inject constructor(
): ViewModel() {

	private val _events = Channel<UiEvent>()
	val events = _events.receiveAsFlow()

	private val _state = MutableStateFlow(EntriesListState())
	val state: StateFlow<EntriesListState> = _state.asStateFlow()

	init {
		_state.update { state ->
			state.copy(
				isLoading = false
			)
		}
	}

	fun onAction(action: EntriesListAction) {
		when (action) {
			is EntriesListAction.OnFABClicked -> TODO()
			is EntriesListAction.OnMemeImageClicked -> TODO()
		}
	}
}