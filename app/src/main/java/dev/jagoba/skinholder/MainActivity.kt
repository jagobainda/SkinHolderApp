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
import dev.jagoba.skinholder.core.SessionExpiredNotifier
import dev.jagoba.skinholder.databinding.ActivityMainBinding
import dev.jagoba.skinholder.dataservice.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var authSessionManager: AuthSessionManager

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var sessionExpiredNotifier: SessionExpiredNotifier

    private val globalViewModel: GlobalViewModel by viewModels()

    private var tokenExpiryWatchdogJob: Job? = null

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                globalViewModel.globalEvents.collect { event ->
                    when (event) {
                        is GlobalEvent.ShowError -> {
                            Snackbar.make(binding.container, event.message, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.navView)
                                .show()
                        }
                        is GlobalEvent.SessionExpired -> {
                            authSessionManager.clearAuthToken()
                            if (navController.currentDestination?.id != R.id.navigation_login) {
                                Snackbar.make(
                                    binding.container,
                                    getString(R.string.session_expired),
                                    Snackbar.LENGTH_LONG
                                ).setAnchorView(binding.navView).show()
                                navigateToLoginFresh(navController)
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                globalViewModel.isAuthenticated.collect { authenticated ->
                    if (authenticated) {
                        startTokenExpiryWatchdog()
                    } else {
                        tokenExpiryWatchdogJob?.cancel()
                        tokenExpiryWatchdogJob = null
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

    override fun onStop() {
        tokenExpiryWatchdogJob?.cancel()
        tokenExpiryWatchdogJob = null
        super.onStop()
    }

    /**
     * While the app is in the foreground, proactively detect token expiration so
     * the user is sent to the login screen without needing to relaunch the app.
     *
     * Strategy:
     *  1. If the JWT `exp` is already in the past, notify immediately.
     *  2. Otherwise, schedule a delay until that exact moment and notify then.
     *  3. As a safety net (clock skew / missing exp claim), also re-validate the
     *     token against the server on every foreground entry.
     */
    private fun startTokenExpiryWatchdog() {
        tokenExpiryWatchdogJob?.cancel()
        if (!authSessionManager.isLoggedIn()) return

        if (authSessionManager.isTokenExpired()) {
            sessionExpiredNotifier.notifySessionExpired()
            return
        }

        tokenExpiryWatchdogJob = lifecycleScope.launch {
            // Confirm with the server in case the local clock is wrong or the
            // token was revoked server-side. The interceptor handles 401s.
            runCatching { authRepository.validateToken() }

            val expiry = authSessionManager.getTokenExpiryMillis()
            if (expiry != null) {
                val waitMs = expiry - System.currentTimeMillis()
                if (waitMs > 0L) {
                    delay(waitMs)
                }
                if (authSessionManager.isLoggedIn()) {
                    sessionExpiredNotifier.notifySessionExpired()
                }
            }
        }
    }
}