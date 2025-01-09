package br.android.cericatto.echojournal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.android.cericatto.echojournal.ui.entries_list.EntriesListScreenRoot

@Composable
fun NavHostComposable() {
	val navController = rememberNavController()
	NavHost(
		navController = navController,
		startDestination = Route.EntriesList
	) {
		composable<Route.EntriesList> {
			EntriesListScreenRoot(
				onNavigate = { navController.navigate(it) },
				onNavigateUp = { navController.navigateUp() }
			)
		}
	}
}