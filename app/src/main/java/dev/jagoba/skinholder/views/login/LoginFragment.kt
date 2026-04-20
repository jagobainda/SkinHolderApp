package dev.jagoba.skinholder.views.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
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
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            binding.progressLoading.isVisible = false
                            binding.btnLogin.isEnabled = true
                            binding.textError.isVisible = false
                        }

                        is LoginUiState.Loading -> {
                            binding.progressLoading.isVisible = true
                            binding.btnLogin.isEnabled = false
                            binding.textError.isVisible = false
                        }

                        is LoginUiState.Success -> {
                            binding.progressLoading.isVisible = false
                            findNavController().navigate(R.id.action_login_to_home)
                        }

                        is LoginUiState.Error -> {
                            binding.progressLoading.isVisible = false
                            binding.btnLogin.isEnabled = true
                            binding.textError.isVisible = true
                            binding.textError.text = state.message
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
