package br.android.cericatto.echojournal.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
	@Serializable
	data object EntriesList: Route
}