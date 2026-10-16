package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthService
import com.example.data.auth.AuthUser
import com.example.data.sync.FirestoreSyncService
import com.example.data.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthTab {
    SIGN_IN, SIGN_UP
}

data class AuthUiState(
    val currentUser: AuthUser? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val activeTab: AuthTab = AuthTab.SIGN_IN,
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val errorMessage: String? = null,
    val syncSuccessMessage: String? = null,
    val lastSyncTimestamp: Long? = null
)

class AuthViewModel(
    private val authService: AuthService,
    private val syncService: FirestoreSyncService
) : ViewModel() {

    private val _emailInput = MutableStateFlow("")
    private val _passwordInput = MutableStateFlow("")
    private val _confirmPasswordInput = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(AuthTab.SIGN_IN)
    private val _isLoading = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _syncSuccessMessage = MutableStateFlow<String?>(null)
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AuthUiState> = combine(
        authService.currentUserFlow,
        _isLoading,
        _isSyncing,
        _activeTab,
        _emailInput,
        _passwordInput,
        _confirmPasswordInput,
        _errorMessage,
        _syncSuccessMessage,
        _lastSyncTimestamp
    ) { params ->
        val user = params[0] as? AuthUser
        val loading = params[1] as Boolean
        val syncing = params[2] as Boolean
        val tab = params[3] as AuthTab
        val email = params[4] as String
        val pass = params[5] as String
        val confirmPass = params[6] as String
        val err = params[7] as? String
        val syncMsg = params[8] as? String
        val lastSync = params[9] as? Long

        AuthUiState(
            currentUser = user,
            isLoading = loading,
            isSyncing = syncing,
            activeTab = tab,
            emailInput = email,
            passwordInput = pass,
            confirmPasswordInput = confirmPass,
            errorMessage = err,
            syncSuccessMessage = syncMsg,
            lastSyncTimestamp = lastSync
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthUiState()
    )

    fun setEmail(email: String) {
        _emailInput.value = email
        _errorMessage.value = null
    }

    fun setPassword(password: String) {
        _passwordInput.value = password
        _errorMessage.value = null
    }

    fun setConfirmPassword(confirm: String) {
        _confirmPasswordInput.value = confirm
        _errorMessage.value = null
    }

    fun setTab(tab: AuthTab) {
        _activeTab.value = tab
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSyncMessage() {
        _syncSuccessMessage.value = null
    }

    fun signInWithEmail() {
        val email = _emailInput.value.trim()
        val pass = _passwordInput.value.trim()

        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please enter both email and password."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authService.signInWithEmail(email, pass)
            _isLoading.value = false

            result.onSuccess {
                _emailInput.value = ""
                _passwordInput.value = ""
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Sign in failed. Please check your credentials."
            }
        }
    }

    fun signUpWithEmail() {
        val email = _emailInput.value.trim()
        val pass = _passwordInput.value.trim()
        val confirmPass = _confirmPasswordInput.value.trim()

        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please enter email and password."
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }
        if (pass != confirmPass) {
            _errorMessage.value = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authService.signUpWithEmail(email, pass)
            _isLoading.value = false

            result.onSuccess {
                _emailInput.value = ""
                _passwordInput.value = ""
                _confirmPasswordInput.value = ""
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Sign up failed. Please try a different email."
            }
        }
    }

    fun launchGoogleSignIn(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authService.launchGoogleSignIn(context)
            _isLoading.value = false

            result.onSuccess {
                _emailInput.value = ""
                _passwordInput.value = ""
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Google Sign-In was cancelled or failed."
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authService.signInWithGoogleCredential(idToken)
            _isLoading.value = false

            result.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Google Sign-In failed."
            }
        }
    }

    fun signOut() {
        authService.signOut()
        _syncSuccessMessage.value = null
    }

    fun backupToCloud() {
        val userId = authService.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncSuccessMessage.value = null
            _errorMessage.value = null

            val result = syncService.backupToCloud(userId)
            _isSyncing.value = false

            result.onSuccess { syncResult ->
                _syncSuccessMessage.value = syncResult.message
                _lastSyncTimestamp.value = syncResult.timestamp
            }.onFailure { e ->
                _errorMessage.value = "Backup failed: ${e.localizedMessage ?: "Network error"}"
            }
        }
    }

    fun restoreFromCloud() {
        val userId = authService.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncSuccessMessage.value = null
            _errorMessage.value = null

            val result = syncService.restoreFromCloud(userId)
            _isSyncing.value = false

            result.onSuccess { syncResult ->
                _syncSuccessMessage.value = syncResult.message
                _lastSyncTimestamp.value = syncResult.timestamp
            }.onFailure { e ->
                _errorMessage.value = "Restore failed: ${e.localizedMessage ?: "Network error"}"
            }
        }
    }

    companion object {
        fun provideFactory(
            authService: AuthService,
            syncService: FirestoreSyncService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(authService, syncService) as T
            }
        }
    }
}
