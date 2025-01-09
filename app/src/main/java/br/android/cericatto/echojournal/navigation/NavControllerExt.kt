package br.android.cericatto.echojournal.navigation

import androidx.navigation.NavController
import br.android.cericatto.echojournal.ui.UiEvent

fun NavController.navigate(event: UiEvent.Navigate) {
    this.navigate(event.route)
}