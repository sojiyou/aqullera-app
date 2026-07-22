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

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseDatabase: FirebaseDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val firebaseUser = authRepository.getCurrentUser() ?: run {
            _uiState.value = ProfileUiState(error = "Not logged in", loading = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                val snapshot = firebaseDatabase.getReference("users").child(firebaseUser.uid).get().await()
                val user = snapshot.getValue(User::class.java)
                _uiState.value = ProfileUiState(
                    displayName = user?.displayName ?: firebaseUser.displayName ?: firebaseUser.email ?: "",
                    email = firebaseUser.email ?: "",
                    phone = user?.displayPhone ?: "",
                    loading = false,
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    displayName = firebaseUser.displayName ?: firebaseUser.email ?: "",
                    email = firebaseUser.email ?: "",
                    loading = false,
                    error = e.message,
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
