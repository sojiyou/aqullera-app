package com.example.aquallera.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.util.ErrorUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data object Unauthenticated : AuthState()
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

data class SignupUiState(
    val fullName: String = "",
    val email: String = "",
    val number: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

data class VerifyEmailUiState(
    val email: String? = null,
    val code: String = "",
    val error: String? = null,
    val verifying: Boolean = false,
    val sending: Boolean = false,
    val cooldown: Int = 0,
    /** Set once the code is accepted — the screen then shows the data privacy notice. */
    val showPrivacy: Boolean = false,
)

/**
 * Backs the whole auth flow: landing, login, signup and the 6-digit email verification.
 * Auth state comes from a Firebase listener rather than a one-shot check, so signing out and
 * back in cannot leave stale state behind.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow(SignupUiState())
    val signupState: StateFlow<SignupUiState> = _signupState.asStateFlow()

    private val _verifyState = MutableStateFlow(VerifyEmailUiState())
    val verifyState: StateFlow<VerifyEmailUiState> = _verifyState.asStateFlow()

    /** null while the RTDB check is in flight, so callers can wait instead of assuming unverified. */
    private val _emailVerified = MutableStateFlow<Boolean?>(null)
    val emailVerified: StateFlow<Boolean?> = _emailVerified.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    /** True only between a fresh signup and its OTP being accepted, so the delete-on-leave
     *  guard never removes an established, already-verified account (e.g. mid email change). */
    private var pendingSignup = false

    init {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
            if (user != null) refreshEmailVerified() else _emailVerified.value = null
        }
        firebaseAuth.addAuthStateListener(authListener!!)
    }

    /** Re-reads the verification flag from the database into [emailVerified]. */
    fun refreshEmailVerified() {
        viewModelScope.launch {
            _emailVerified.value = authRepository.isEmailVerified()
        }
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { firebaseAuth.removeAuthStateListener(it) }
    }

    /** Updates the login email and clears any previous error. */
    fun onLoginEmailChanged(email: String) {
        _loginState.value = _loginState.value.copy(email = email, error = null)
    }

    /** Updates the login password and clears any previous error. */
    fun onLoginPasswordChanged(password: String) {
        _loginState.value = _loginState.value.copy(password = password, error = null)
    }

    /** Validates the form and signs in. The auth listener then drives navigation. */
    fun login() {
        val state = _loginState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _loginState.value = state.copy(error = "Please fill all fields")
            return
        }
        _loginState.value = state.copy(loading = true, error = null)

        viewModelScope.launch {
            val result = authRepository.signIn(state.email, state.password)
            result.fold(
                onSuccess = { user ->
                    _loginState.value = _loginState.value.copy(loading = false)
                    _authState.value = AuthState.Authenticated(user)
                    _verifyState.value = VerifyEmailUiState(email = user.email)
                },
                onFailure = { e ->
                    val err = if (e is Exception) e else Exception(e)
                    _loginState.value = _loginState.value.copy(
                        loading = false,
                        error = ErrorUtils.getFirebaseErrorMessage(err),
                    )
                },
            )
        }
    }

    /** Updates one signup field by name; the phone field is digit-only and capped at 11. */
    fun onSignupFieldChanged(field: String, value: String) {
        val current = _signupState.value
        _signupState.value = when (field) {
            "fullName" -> current.copy(fullName = value, error = null)
            "email" -> current.copy(email = value, error = null)
            "number" -> current.copy(number = value.filter { it.isDigit() }.take(11), error = null)
            "password" -> current.copy(password = value, error = null)
            "confirmPassword" -> current.copy(confirmPassword = value, error = null)
            else -> current
        }
    }

    /**
     * Validates the form, rejects emails or phone numbers already in use, creates the account
     * and profile row, then sends the first verification code.
     */
    fun signup() {
        val state = _signupState.value

        if (state.fullName.isBlank() || state.email.isBlank() ||
            state.number.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()
        ) {
            _signupState.value = state.copy(error = "Please fill all fields")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _signupState.value = state.copy(error = "Please enter a valid email")
            return
        }

        if (state.password != state.confirmPassword) {
            _signupState.value = state.copy(error = "Passwords do not match")
            return
        }

        if (state.number.length != 11) {
            _signupState.value = state.copy(error = "Please enter a valid 11-digit phone number")
            return
        }

        if (state.password.length < 6) {
            _signupState.value = state.copy(error = "Password must be at least 6 characters")
            return
        }

        _signupState.value = state.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                if (authRepository.isPhoneTaken(state.number)) {
                    _signupState.value = _signupState.value.copy(
                        loading = false,
                        error = "Phone number already registered. Please use a different number.",
                    )
                    return@launch
                }

                if (authRepository.isEmailTakenInUsers(state.email)) {
                    _signupState.value = _signupState.value.copy(
                        loading = false,
                        error = "This email is already registered as a customer. Please log in instead.",
                    )
                    return@launch
                }

                if (authRepository.isEmailTakenInStations(state.email)) {
                    _signupState.value = _signupState.value.copy(
                        loading = false,
                        error = "This email is already registered as a station owner.",
                    )
                    return@launch
                }

                val result = authRepository.signUp(state.email, state.password)
                result.fold(
                    onSuccess = { firebaseUser ->
                        authRepository.updateProfile(state.fullName)
                        authRepository.createUserProfile(
                            uid = firebaseUser.uid,
                            fullName = state.fullName,
                            email = state.email,
                            number = state.number,
                        )
                        authRepository.sendVerificationCode()
                        pendingSignup = true
                        _authState.value = AuthState.Authenticated(firebaseUser)
                        _signupState.value = _signupState.value.copy(loading = false, error = null)
                        _verifyState.value = VerifyEmailUiState(email = state.email)
                    },
                    onFailure = { e ->
                        val err = if (e is Exception) e else Exception(e)
                        _signupState.value = _signupState.value.copy(
                            loading = false,
                            error = ErrorUtils.getFirebaseErrorMessage(err),
                        )
                    },
                )
            } catch (e: Exception) {
                _signupState.value = _signupState.value.copy(
                    loading = false,
                    error = ErrorUtils.getFirebaseErrorMessage(e),
                )
            }
        }
    }

    /** Keeps only digits, capped at the 6 the code requires. */
    fun onCodeChanged(value: String) {
        _verifyState.value = _verifyState.value.copy(
            code = value.filter { it.isDigit() }.take(6),
            error = null,
        )
    }

    /** Checks the entered code; on success shows the privacy notice instead of navigating away. */
    fun verifyCode() {
        val code = _verifyState.value.code
        if (code.length != 6) return
        viewModelScope.launch {
            _verifyState.value = _verifyState.value.copy(verifying = true, error = null)
            try {
                val error = authRepository.verifyCode(code)
                _verifyState.value = _verifyState.value.copy(
                    verifying = false,
                    error = error,
                    showPrivacy = error == null,
                )
                if (error == null) {
                        pendingSignup = false
                        refreshEmailVerified()
                    }
            } catch (_: Exception) {
                _verifyState.value = _verifyState.value.copy(
                    verifying = false,
                    error = "Verification failed. Please try again.",
                )
            }
        }
    }

    /** Issues a new code, ignored while a send is in flight or the cooldown is running. */
    fun resendVerification() {
        if (_verifyState.value.cooldown > 0 || _verifyState.value.sending) return
        viewModelScope.launch {
            _verifyState.value = _verifyState.value.copy(sending = true, error = null)
            try {
                authRepository.sendVerificationCode()
                _verifyState.value = _verifyState.value.copy(sending = false)
                startCooldown()
            } catch (_: Exception) {
                _verifyState.value = _verifyState.value.copy(
                    sending = false,
                    error = "Failed to resend code. Please try again.",
                )
            }
        }
    }

    /** Counts [cooldown] down from 30s so the resend link cannot be spammed. */
    private fun startCooldown() {
        viewModelScope.launch {
            for (remaining in 30 downTo 1) {
                _verifyState.value = _verifyState.value.copy(cooldown = remaining)
                delay(1000)
            }
            _verifyState.value = _verifyState.value.copy(cooldown = 0)
        }
    }

    /**
     * Leaving the verify screen without entering the code discards the account, so the email stays
     * free for another signup attempt. The account is only kept once the OTP has been accepted.
     */
    fun abandonUnverifiedSignup() {
        viewModelScope.launch {
            try {
                // Only delete an account created during the current signup session. An
                // established, already-verified user (e.g. mid email change) is never deleted.
                if (pendingSignup && !authRepository.isEmailVerified()) {
                    pendingSignup = false
                    authRepository.deleteAccount()
                }
            } catch (_: Exception) {
                // Deletion can fail (e.g. stale credentials); sign out anyway so the user isn't stuck.
            }
            logout()
        }
    }

    /** Signs out and clears every auth-related UI state. */
    fun logout() {
        pendingSignup = false
        authRepository.signOut()
        _authState.value = AuthState.Unauthenticated
        _loginState.value = LoginUiState()
        _signupState.value = SignupUiState()
        _verifyState.value = VerifyEmailUiState()
    }
}
