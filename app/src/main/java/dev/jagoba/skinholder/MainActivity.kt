package dev.jagoba.skinholder

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
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

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        // Hide bottom nav on login screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_login) {
                navView.visibility = View.GONE
            } else {
                navView.visibility = View.VISIBLE
            }
        }

        // Validate token and redirect to login if not authenticated
        lifecycleScope.launch {
            if (authSessionManager.isLoggedIn()) {
                // Validate token before allowing access
                authRepository.validateToken().fold(
                    onSuccess = { isValid ->
                        if (!isValid) {
                            authSessionManager.clearSession()
                            navController.navigate(R.id.navigation_login)
                        }
                    },
                    onFailure = {
                        // On network error, assume token might still be valid
                        // but show warning if critical operations fail
                    }
                )
            } else {
                navController.navigate(R.id.navigation_login)
            }
        }

        // Observe global events (errors, session expiry)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                globalViewModel.globalEvents.collect { event ->
                    when (event) {
                        is GlobalEvent.ShowError -> {
                            Snackbar.make(binding.container, event.message, Snackbar.LENGTH_LONG).show()
                        }
                        is GlobalEvent.SessionExpired -> {
                            authSessionManager.clearSession()
                            Snackbar.make(
                                binding.container,
                                getString(R.string.session_expired),
                                Snackbar.LENGTH_LONG
                            ).show()
                            if (navController.currentDestination?.id != R.id.navigation_login) {
                                navController.navigate(R.id.navigation_login) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}