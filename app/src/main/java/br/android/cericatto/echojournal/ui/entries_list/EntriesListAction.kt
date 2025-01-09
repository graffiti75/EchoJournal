package br.android.cericatto.echojournal.ui.entries_list

sealed interface EntriesListAction {
	data class OnFABClicked(val isSheetOpen: Boolean) : EntriesListAction
	data class OnMemeImageClicked(val resId: Int) : EntriesListAction
}