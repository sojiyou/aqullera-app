package com.example.aquallera.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquallera.auth.data.AuthRepository
import com.example.aquallera.core.model.User
import com.example.aquallera.orders.data.OrderRepository
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
    val checkingOrders: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showActiveOrders: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
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

    /** Updates the name field. */
    fun onNameChanged(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    /** Updates the email field. */
    fun onEmailChanged(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    /** Updates the phone field. */
    fun onNumberChanged(value: String) { _uiState.value = _uiState.value.copy(number = value) }

    /** Validates and saves name, email and phone. */
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
