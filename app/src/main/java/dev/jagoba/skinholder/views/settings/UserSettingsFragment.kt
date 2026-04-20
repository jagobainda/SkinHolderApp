package dev.jagoba.skinholder.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.databinding.FragmentUserSettingsBinding
import dev.jagoba.skinholder.views.dialogs.ConfirmationDialogFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserSettingsFragment : Fragment() {

    private var _binding: FragmentUserSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupDialogResults()
        observeState()
        observeEvents()
    }

    private fun setupListeners() {
        binding.btnChangePassword.setOnClickListener {
            viewModel.changePassword(
                currentPassword = binding.editCurrentPassword.text.toString(),
                newPassword = binding.editNewPassword.text.toString(),
                confirmPassword = binding.editConfirmPassword.text.toString()
            )
        }

        binding.btnDeleteAccount.setOnClickListener {
            val password = binding.editDeletePassword.text.toString()
            if (password.isBlank()) {
                showDeleteStatus(getString(R.string.settings_password_empty), isError = true)
                return@setOnClickListener
            }
            ConfirmationDialogFragment.newInstance(
                title = getString(R.string.settings_delete_confirm_title),
                message = getString(R.string.settings_delete_confirm_message),
                confirmText = getString(R.string.settings_delete_btn),
                requestKey = REQUEST_DELETE_ACCOUNT
            ).show(childFragmentManager, "delete_dialog")
        }

        binding.btnLogout.setOnClickListener {
            ConfirmationDialogFragment.newInstance(
                title = getString(R.string.settings_logout_confirm_title),
                message = getString(R.string.settings_logout_confirm_message),
                requestKey = REQUEST_LOGOUT
            ).show(childFragmentManager, "logout_dialog")
        }
    }

    private fun setupDialogResults() {
        childFragmentManager.setFragmentResultListener(REQUEST_DELETE_ACCOUNT, viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean(ConfirmationDialogFragment.RESULT_KEY)) {
                viewModel.deleteAccount(binding.editDeletePassword.text.toString())
            }
        }

        childFragmentManager.setFragmentResultListener(REQUEST_LOGOUT, viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean(ConfirmationDialogFragment.RESULT_KEY)) {
                viewModel.logout()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UserSettingsUiState.Loading -> {
                                binding.progressLoading.isVisible = true
                                binding.layoutContent.isVisible = false
                            }
                            is UserSettingsUiState.Loaded -> {
                                binding.progressLoading.isVisible = false
                                binding.layoutContent.isVisible = true
                                binding.textUsername.text = state.username
                                binding.textCreatedAt.text = state.createdAt
                            }
                            is UserSettingsUiState.Error -> {
                                binding.progressLoading.isVisible = false
                                binding.layoutContent.isVisible = true
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.btnChangePassword.isEnabled = !loading
                        binding.btnDeleteAccount.isEnabled = !loading
                        binding.btnLogout.isEnabled = !loading
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UserSettingsEvent.PasswordChanged -> {
                            clearPasswordFields()
                            showPasswordStatus(event.message, isError = false)
                        }
                        is UserSettingsEvent.PasswordError -> {
                            showPasswordStatus(event.message, isError = true)
                        }
                        is UserSettingsEvent.AccountDeleted -> {
                            navigateToLogin()
                        }
                        is UserSettingsEvent.DeleteError -> {
                            showDeleteStatus(event.message, isError = true)
                        }
                        is UserSettingsEvent.LoggedOut -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun showPasswordStatus(message: String, isError: Boolean) {
        binding.textPasswordStatus.apply {
            text = message
            setTextColor(
                if (isError) resources.getColor(android.R.color.holo_red_dark, null)
                else resources.getColor(android.R.color.holo_green_dark, null)
            )
            isVisible = true
        }
    }

    private fun showDeleteStatus(message: String, isError: Boolean) {
        binding.textDeleteStatus.apply {
            text = message
            setTextColor(
                if (isError) resources.getColor(android.R.color.holo_red_dark, null)
                else resources.getColor(android.R.color.holo_green_dark, null)
            )
            isVisible = true
        }
    }

    private fun clearPasswordFields() {
        binding.editCurrentPassword.text?.clear()
        binding.editNewPassword.text?.clear()
        binding.editConfirmPassword.text?.clear()
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_settings_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_DELETE_ACCOUNT = "request_delete_account"
        private const val REQUEST_LOGOUT = "request_logout"
    }
}
