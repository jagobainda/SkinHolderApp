package dev.jagoba.skinholder.views.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dev.jagoba.skinholder.core.GlobalViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private val globalViewModel: GlobalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillSavedCredentials()
        setupListeners()
        observeState()
    }

    private fun prefillSavedCredentials() {
        // Only pre-fill when the fields are empty so we don't clobber text the
        // user may have typed before a configuration change.
        val saved = viewModel.getSavedCredentials() ?: return
        if (binding.editUsername.text.isNullOrEmpty()) {
            binding.editUsername.setText(saved.username)
        }
        if (binding.editPassword.text.isNullOrEmpty() && saved.password.isNotEmpty()) {
            binding.editPassword.setText(saved.password)
        }
        binding.checkRememberMe.isChecked = saved.rememberMe
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.editPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else false
        }

        binding.checkRememberMe.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSavePassword(isChecked)
        }

        binding.btnRegister.setOnClickListener {
            showRegisterDialog()
        }
    }

    private fun performLogin() {
        val username = binding.editUsername.text?.toString().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        viewModel.login(username, password)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is LoginUiState.Idle -> {
                                binding.btnLogin.isEnabled = true
                                binding.textError.visibility = View.INVISIBLE
                            }

                            is LoginUiState.Loading -> {
                                binding.btnLogin.isEnabled = false
                                binding.textError.visibility = View.INVISIBLE
                            }

                            is LoginUiState.Success -> {
                                globalViewModel.refreshSessionState()
                                findNavController().navigate(R.id.action_login_to_home)
                            }

                            is LoginUiState.Error -> {
                                binding.btnLogin.isEnabled = true
                                binding.textError.visibility = View.VISIBLE
                                binding.textError.text = state.message
                            }
                        }
                    }
                }
                launch {
                    viewModel.savePassword.collect { checked ->
                        if (binding.checkRememberMe.isChecked != checked) {
                            binding.checkRememberMe.isChecked = checked
                        }
                    }
                }
            }
        }
    }

    private fun showRegisterDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.register_private_title))
            .setMessage(getString(R.string.register_private_message))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("email", "skinholder@jagoba.dev")
                clipboard.setPrimaryClip(clip)
                Snackbar.make(binding.root, getString(R.string.register_email_copied), Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
