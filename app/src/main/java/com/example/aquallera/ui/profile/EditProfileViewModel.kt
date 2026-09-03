package com.example.aquallera.ui.profile

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.model.User
import com.example.aquallera.orders.data.OrderRepository
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val originalEmail: String = "",
    val number: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val checkingOrders: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showActiveOrders: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val showOtpDialog: Boolean = false,
    val otpCode: String = "",
    val password: String = "",
    val verifying: Boolean = false,
    val sendingOtp: Boolean = false,
    val otpCooldown: Int = 0,
    val otpError: String? = null,
    val otpSentTo: String? = null,
)

/** Backs Edit Profile, including account deletion and its active-order guard. */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val firebaseDatabase: FirebaseDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /** Pre-fills the form from the profile row, falling back to the Auth record. */
    private fun loadProfile() {
        val firebaseUser = authRepository.getCurrentUser() ?: run {
            _uiState.value = EditProfileUiState(error = "Not logged in", loading = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                val snapshot = firebaseDatabase.getReference("users").child(firebaseUser.uid).get().await()
                val user = snapshot.getValue(User::class.java)
                val currentEmail = firebaseUser.email ?: ""
                _uiState.value = EditProfileUiState(
                    name = user?.displayName ?: firebaseUser.displayName ?: "",
                    email = currentEmail,
                    originalEmail = currentEmail,
                    number = user?.displayPhone ?: "",
                    loading = false,
                )
            } catch (e: Exception) {
                val currentEmail = firebaseUser.email ?: ""
                _uiState.value = EditProfileUiState(
                    name = firebaseUser.displayName ?: "",
                    email = currentEmail,
                    originalEmail = currentEmail,
                    loading = false,
                    error = e.message,
                )
            }
        }
    }

    /** Updates the name field. */
    fun onNameChanged(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    /** Updates the email field. */
    fun onEmailChanged(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    /** Updates the phone field. */
    fun onNumberChanged(value: String) { _uiState.value = _uiState.value.copy(number = value) }

    /** Validates and saves name, email and phone. Triggers OTP re-verification when email changes. */
    fun save() {
        val s = _uiState.value
        if (s.name.isBlank() || s.email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Name and email are required.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(s.email.trim()).matches()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address.")
            return
        }
        val user = authRepository.getCurrentUser() ?: return
        val emailChanged = s.email.trim() != s.originalEmail
        _uiState.value = _uiState.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                if (emailChanged) {
                    val emailTaken = authRepository.isEmailTakenInUsers(s.email.trim())
                            || authRepository.isEmailTakenInStations(s.email.trim())
                    if (emailTaken) {
                        _uiState.value = _uiState.value.copy(
                            saving = false,
                            error = "This email is already in use.",
                        )
                        return@launch
                    }
                    // Send the OTP to the NEW email and show the dialog. The email is only
                    // persisted after successful verification + re-auth (in verifyOtp).
                    authRepository.sendEmailChangeCode(s.email.trim())
                    _uiState.value = _uiState.value.copy(
                        saving = false,
                        showOtpDialog = true,
                        otpSentTo = s.email.trim(),
                        otpCode = "",
                        password = "",
                        otpError = null,
                    )
                } else {
                    authRepository.updateUserProfile(user.uid, s.name.trim(), s.email.trim(), s.number.trim())
                    _uiState.value = _uiState.value.copy(saving = false, saved = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message ?: "Failed to update profile")
            }
        }
    }

    /** Keeps only digits, capped at the 6 the code requires. */
    fun onOtpChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            otpCode = value.filter { it.isDigit() }.take(6),
            otpError = null,
        )
    }

    /** Updates the password entered for re-authentication. */
    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, otpError = null)
    }

    /** Verifies the OTP then re-authenticates with the password to update the Auth email. */
    fun verifyOtp() {
        val code = _uiState.value.otpCode
        if (code.length != 6) return
        val s = _uiState.value
        if (s.password.isBlank()) {
            _uiState.value = _uiState.value.copy(otpError = "Please enter your password.")
            return
        }
        val user = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(verifying = true, otpError = null)
            try {
                val error = authRepository.verifyEmailChangeCode(code)
                if (error == null) {
                    val name = _uiState.value.name.trim()
                    val newEmail = _uiState.value.email.trim()
                    val number = _uiState.value.number.trim()
                    val password = _uiState.value.password
                    // Re-authenticate and update the Firebase Auth email.
                    authRepository.reauthenticateAndUpdateEmail(password, newEmail)
                    // Persist the profile to RTDB only after Auth email is confirmed.
                    authRepository.updateUserProfile(user.uid, name, newEmail, number)
                    _uiState.value = _uiState.value.copy(
                        verifying = false,
                        showOtpDialog = false,
                        saved = true,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(verifying = false, otpError = error)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    verifying = false,
                    otpError = e.message ?: "Verification failed. Please try again.",
                )
            }
        }
    }

    /** Issues a new OTP code, guarded by a cooldown timer. */
    fun resendOtp() {
        val s = _uiState.value
        if (s.otpCooldown > 0 || s.sendingOtp) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendingOtp = true, otpError = null)
            try {
                authRepository.sendEmailChangeCode(s.otpSentTo!!)
                _uiState.value = _uiState.value.copy(sendingOtp = false)
                startOtpCooldown()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    sendingOtp = false,
                    otpError = "Failed to resend code. Please try again.",
                )
            }
        }
    }

    private fun startOtpCooldown() {
        viewModelScope.launch {
            for (i in 30 downTo 1) {
                _uiState.value = _uiState.value.copy(otpCooldown = i)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(otpCooldown = 0)
        }
    }

    /** Closes the OTP dialog. Nothing was saved yet, so the profile keeps the old email. */
    fun dismissOtpDialog() {
        _uiState.value = _uiState.value.copy(showOtpDialog = false)
    }

    /** An account can only be deleted once every order is completed or cancelled. */
    fun onDeleteAccountClicked() {
        val user = authRepository.getCurrentUser() ?: return
        _uiState.value = _uiState.value.copy(checkingOrders = true, error = null)
        viewModelScope.launch {
            val orders = orderRepository.getOrdersByUser(user.uid).getOrNull()
            if (orders == null) {
                _uiState.value = _uiState.value.copy(
                    checkingOrders = false,
                    error = "Unable to check your orders. Please try again.",
                )
                return@launch
            }
            val hasActive = orders.any {
                it.status.lowercase() !in listOf("completed", "cancelled")
            }
            _uiState.value = _uiState.value.copy(
                checkingOrders = false,
                showActiveOrders = hasActive,
                showDeleteConfirm = !hasActive,
            )
        }
    }

    /** Closes both the delete-confirmation and active-orders dialogs. */
    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false, showActiveOrders = false)
    }

    /** Deletes the account after the confirmation dialog, ignoring repeat taps. */
    fun deleteAccount() {
        if (_uiState.value.deleting) return
        _uiState.value = _uiState.value.copy(deleting = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.deleteAccount()
                _uiState.value = _uiState.value.copy(deleting = false, showDeleteConfirm = false, deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    deleting = false,
                    showDeleteConfirm = false,
                    error = e.message
                        ?: "Failed to delete account. Please log out and log in again, then retry.",
                )
            }
        }
    }
}
