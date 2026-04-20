package dev.jagoba.skinholder.views.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfirmationDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val title = args.getString(ARG_TITLE, "")
        val message = args.getString(ARG_MESSAGE, "")
        val confirmText = args.getString(ARG_CONFIRM_TEXT, "Sí")
        val cancelText = args.getString(ARG_CANCEL_TEXT, "Cancelar")
        val requestKey = args.getString(ARG_REQUEST_KEY, DEFAULT_REQUEST_KEY)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmText) { _, _ ->
                setFragmentResult(requestKey, bundleOf(RESULT_KEY to true))
            }
            .setNegativeButton(cancelText) { _, _ ->
                setFragmentResult(requestKey, bundleOf(RESULT_KEY to false))
            }
            .create()
    }

    companion object {
        const val DEFAULT_REQUEST_KEY = "confirmation_result"
        const val RESULT_KEY = "confirmed"

        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CONFIRM_TEXT = "confirm_text"
        private const val ARG_CANCEL_TEXT = "cancel_text"
        private const val ARG_REQUEST_KEY = "request_key"

        fun newInstance(
            title: String,
            message: String,
            confirmText: String = "Sí",
            cancelText: String = "Cancelar",
            requestKey: String = DEFAULT_REQUEST_KEY
        ): ConfirmationDialogFragment {
            return ConfirmationDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_MESSAGE to message,
                    ARG_CONFIRM_TEXT to confirmText,
                    ARG_CANCEL_TEXT to cancelText,
                    ARG_REQUEST_KEY to requestKey
                )
            }
        }
    }
}
