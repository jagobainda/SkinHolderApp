package dev.jagoba.skinholder

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.core.GlobalEvent
import dev.jagoba.skinholder.core.GlobalViewModel
import dev.jagoba.skinholder.databinding.ActivityMainBinding
import dev.jagoba.skinholder.dataservice.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var authSessionManager: AuthSessionManager

    @Inject
    lateinit var authRepository: AuthRepository

    private val globalViewModel: GlobalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val graph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        graph.setStartDestination(
            if (authSessionManager.isLoggedIn()) R.id.navigation_home
            else R.id.navigation_login
        )
        navController.graph = graph

        navView.setupWithNavController(navController)

        // Hide bottom nav on login screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.visibility = if (destination.id == R.id.navigation_login) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        if (authSessionManager.isLoggedIn()) {
            lifecycleScope.launch {
                runCatching { authRepository.validateToken() }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                globalViewModel.globalEvents.collect { event ->
                    when (event) {
                        is GlobalEvent.ShowError -> {
                            Snackbar.make(binding.container, event.message, Snackbar.LENGTH_LONG).show()
                        }
                        is GlobalEvent.SessionExpired -> {
                            authSessionManager.clearAuthToken()
                            if (navController.currentDestination?.id != R.id.navigation_login) {
                                Snackbar.make(
                                    binding.container,
                                    getString(R.string.session_expired),
                                    Snackbar.LENGTH_LONG
                                ).show()
                                navigateToLoginFresh(navController)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToLoginFresh(navController: androidx.navigation.NavController) {
        val freshGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        freshGraph.setStartDestination(R.id.navigation_login)
        navController.setGraph(freshGraph, null)
    }
}