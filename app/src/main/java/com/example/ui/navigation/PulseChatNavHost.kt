package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.preference.UserPreferences
import com.example.ui.chat.ChatThreadScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.newchat.NewChatScreen
import com.example.ui.newchat.NewChatViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val NEW_CHAT = "new_chat"
    const val CHAT = "chat/{conversationId}"
    const val PROFILE = "profile"

    fun buildChatRoute(conversationId: String) = "chat/$conversationId"
}

@Composable
fun PulseChatNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)
    val userProfile by userPreferences.userProfile.collectAsState()

    val startDestination = if (userProfile.isOnboarded) Routes.HOME else Routes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            val factory = PulseChatViewModelFactory(context)
            val viewModel: OnboardingViewModel = viewModel(factory = factory)
            OnboardingScreen(
                viewModel = viewModel,
                onOnboardingComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val factory = PulseChatViewModelFactory(context)
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChat = { conversationId ->
                    navController.navigate(Routes.buildChatRoute(conversationId))
                },
                onNavigateToNewChat = {
                    navController.navigate(Routes.NEW_CHAT)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        composable(Routes.NEW_CHAT) {
            val factory = PulseChatViewModelFactory(context)
            val viewModel: NewChatViewModel = viewModel(factory = factory)
            NewChatScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectContact = { phoneNumber ->
                    navController.navigate(Routes.buildChatRoute(phoneNumber)) {
                        popUpTo(Routes.NEW_CHAT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val factory = PulseChatViewModelFactory(context, conversationId)
            val viewModel: ChatViewModel = viewModel(factory = factory)
            ChatThreadScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            val factory = PulseChatViewModelFactory(context)
            val viewModel: ProfileViewModel = viewModel(factory = factory)
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
