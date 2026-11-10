package com.example.lagallens.presentation.feature.dashboard.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeDashboardViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class Contracts(private val source: () -> Flow<List<EmploymentContract>>) : ContractRepository {
        override fun observeContracts() = source()
        override suspend fun getContract(contractId: String): EmploymentContract? = null
        override suspend fun createContract(contract: EmploymentContract) = contract
        override suspend fun updateContract(contract: EmploymentContract) = false
        override suspend fun renameContract(contractId: String, title: String) = false
        override suspend fun deleteContract(contractId: String) = false
        override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = null
    }

    private object Profiles : ProfileRepository {
        override suspend fun getProfile() = UserProfile(name = "Trần B", email = "b@example.test")
        override suspend fun saveProfile(name: String, phone: String, photoUri: String?) = getProfile()
    }

    private fun contract(id: String, updatedAt: Long, risk: EmploymentContractRisk) =
        EmploymentContract(id, "document-$id", "Title $id", "", 0, updatedAt, risk = risk)

    @Test fun statisticsAndRecentContractsComeFromTheRepository() = runTest {
        val source = MutableStateFlow(listOf(
            contract("old", 1, EmploymentContractRisk.LOW),
            contract("newest", 3, EmploymentContractRisk.HIGH),
            contract("middle", 2, EmploymentContractRisk.NOT_ANALYZED)
        ))
        val vm = HomeDashboardViewModel(Contracts { source }, Profiles)
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.totalContracts)
        assertEquals(2, state.analyzedContracts)
        assertEquals(1, state.reviewContracts)
        assertEquals(listOf("newest", "middle"), state.recentContracts.map { it.contract.id })

        source.value = source.value.filterNot { it.id == "newest" }
        assertEquals(2, vm.uiState.value.totalContracts)
        assertEquals(0, vm.uiState.value.reviewContracts)
        assertEquals(listOf("middle", "old"), vm.uiState.value.recentContracts.map { it.contract.id })
    }

    @Test fun noContractsIsAnEmptyStateNotAnError() = runTest {
        val vm = HomeDashboardViewModel(Contracts { MutableStateFlow(emptyList()) }, Profiles)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.recentContracts.isEmpty())
        assertNull(vm.uiState.value.errorMessageRes)
    }

    @Test fun failedLoadShowsErrorAndRetryRecovers() = runTest {
        var fail = true
        val vm = HomeDashboardViewModel(Contracts {
            if (fail) throw IllegalStateException("offline") else MutableStateFlow(listOf(contract("a", 1, EmploymentContractRisk.LOW)))
        }, Profiles)
        assertEquals(R.string.dashboard_load_failed, vm.uiState.value.errorMessageRes)
        fail = false
        vm.onEvent(HomeDashboardUiEvent.RetryClicked)
        assertNull(vm.uiState.value.errorMessageRes)
        assertEquals(1, vm.uiState.value.totalContracts)
    }

    @Test fun userNameAndPhotoRefreshFromTheProfileWhenHomeResumes() = runTest {
        var profile = Profiles.getProfile()
        val profiles = object : ProfileRepository by Profiles {
            override suspend fun getProfile() = profile
        }
        val vm = HomeDashboardViewModel(Contracts { MutableStateFlow(emptyList()) }, profiles)
        vm.onEvent(HomeDashboardUiEvent.ScreenResumed)
        assertEquals("Trần B", vm.uiState.value.userName)
        assertNull(vm.uiState.value.photoUri)

        profile = profile.copy(photoUri = "content://profile/new-photo")
        vm.onEvent(HomeDashboardUiEvent.ScreenResumed)
        assertEquals(profile.photoUri, vm.uiState.value.photoUri)
    }
}
