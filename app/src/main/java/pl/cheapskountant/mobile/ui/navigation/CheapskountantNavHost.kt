package pl.cheapskountant.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.cheapskountant.mobile.data.AppContainer
import pl.cheapskountant.mobile.ui.expense.ExpenseScreen
import pl.cheapskountant.mobile.ui.landing.LandingScreen
import pl.cheapskountant.mobile.ui.photos.PhotoScreen
import pl.cheapskountant.mobile.ui.receipt.ReceiptReviewScreen

@Composable
fun CheapskountantNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "landing") {
        composable("landing") {
            LandingScreen(
                container = container,
                onReceiptPhoto = { navController.navigate("photos") },
                onRegisterExpense = { navController.navigate("expense") },
            )
        }
        composable("photos") {
            PhotoScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenReview = { localId, origin -> navController.navigate("review/$localId/$origin") },
            )
        }
        composable("expense") {
            ExpenseScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack("landing", inclusive = false) },
            )
        }
        composable(
            route = "review/{localId}/{origin}",
            arguments = listOf(
                navArgument("localId") { type = NavType.StringType },
                navArgument("origin") { type = NavType.StringType },
            ),
        ) { entry ->
            val localId = entry.arguments?.getString("localId").orEmpty()
            val direct = entry.arguments?.getString("origin") == "direct"
            ReceiptReviewScreen(
                container = container,
                localId = localId,
                direct = direct,
                onLanding = { navController.popBackStack("landing", inclusive = false) },
                onPhotos = { navController.popBackStack() },
            )
        }
    }
}
