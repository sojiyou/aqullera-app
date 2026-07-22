package com.example.aquallera.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.model.User
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val number: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseDatabase: FirebaseDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

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
                _uiState.value = EditProfileUiState(
                    name = user?.displayName ?: firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    number = user?.displayPhone ?: "",
                    loading = false,
                )
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState(
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    loading = false,
                    error = e.message,
                )
            }
        }
    }

    fun onNameChanged(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onEmailChanged(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    fun onNumberChanged(value: String) { _uiState.value = _uiState.value.copy(number = value) }

    fun save() {
        val s = _uiState.value
        if (s.name.isBlank() || s.email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Name and email are required.")
            return
        }
        val user = authRepository.getCurrentUser() ?: return
        _uiState.value = _uiState.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.updateUserProfile(user.uid, s.name.trim(), s.email.trim(), s.number.trim())
                _uiState.value = _uiState.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message ?: "Failed to update profile")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
