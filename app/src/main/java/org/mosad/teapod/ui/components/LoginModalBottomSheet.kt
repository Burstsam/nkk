package org.mosad.teapod.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.mosad.teapod.databinding.ModalBottomSheetLoginBinding

/**
 * A bottom sheet with login credential input fields.
 *
 * To initialize login or password values, use apply.
 */
class LoginModalBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: ModalBottomSheetLoginBinding

    var login = ""
    var password = ""

    lateinit var positiveAction: LoginModalBottomSheet.() -> Unit
    lateinit var negativeAction: LoginModalBottomSheet.() -> Unit

    companion object {
        const val TAG = "LoginModalBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ModalBottomSheetLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTextLogin.setText(login)
        binding.editTextPassword.setText(password)

        binding.positiveButton.setOnClickListener {
            login = binding.editTextLogin.text.toString()
            password = binding.editTextPassword.text.toString()

            positiveAction.invoke(this)
        }
        binding.negativeButton.setOnClickListener {
            negativeAction.invoke(this)
        }
    }
}
