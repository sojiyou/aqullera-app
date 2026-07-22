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
    val statusMsg: String? = null,
    val sending: Boolean = false,
    val verified: Boolean = false,
)

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

    private var authListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null

    init {
        authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
        }
        firebaseAuth.addAuthStateListener(authListener!!)
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { firebaseAuth.removeAuthStateListener(it) }
    }

    fun onLoginEmailChanged(email: String) {
        _loginState.value = _loginState.value.copy(email = email, error = null)
    }

    fun onLoginPasswordChanged(password: String) {
        _loginState.value = _loginState.value.copy(password = password, error = null)
    }

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
                        authRepository.sendVerificationEmail()
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

    fun resendVerification() {
        viewModelScope.launch {
            _verifyState.value = _verifyState.value.copy(sending = true, statusMsg = null)
            try {
                authRepository.sendVerificationEmail()
                _verifyState.value = _verifyState.value.copy(
                    sending = false,
                    statusMsg = "Verification email sent! Check your inbox.",
                )
            } catch (e: Exception) {
                _verifyState.value = _verifyState.value.copy(
                    sending = false,
                    statusMsg = "Failed to send verification email. Try again later.",
                )
            }
        }
    }

    fun startVerificationPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                try {
                    authRepository.reloadUser()
                    if (authRepository.isEmailVerified()) {
                        _verifyState.value = _verifyState.value.copy(verified = true)
                        break
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun logout() {
        authRepository.signOut()
        _authState.value = AuthState.Unauthenticated
        _loginState.value = LoginUiState()
        _signupState.value = SignupUiState()
        _verifyState.value = VerifyEmailUiState()
    }
}
