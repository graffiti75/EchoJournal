package br.android.cericatto.echojournal.ui

import br.android.cericatto.echojournal.navigation.Route

sealed class UiEvent {
	data class Navigate(val route: Route): UiEvent()
	data object NavigateUp: UiEvent()
	data class ShowErrorSnackbar(val messages: List<UiText>): UiEvent()
	data class ShowSnackbar(val message: UiText): UiEvent()
}