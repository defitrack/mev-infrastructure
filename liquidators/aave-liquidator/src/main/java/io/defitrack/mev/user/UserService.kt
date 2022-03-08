package io.defitrack.mev.user

import io.defitrack.mev.user.domain.AaveUser
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserService(private val aaveUserRepository: AaveUserRepository) {

    @Transactional(readOnly = true)
    fun getAlllUsers() = aaveUserRepository.findAll()

    @Transactional
    fun saveUser(address: String) {
        if (aaveUserRepository.findByIdOrNull(address) == null) {
            aaveUserRepository.save(AaveUser(address))
        }
    }

    @Transactional
    fun updateUserHF(aaveUser: AaveUser, hf: Double) {
        aaveUserRepository.findByIdOrNull(aaveUser.address)?.let {
            aaveUserRepository.save(
                it.copy(
                    latestHf = hf
                )
            )
        }
    }

    @Transactional
    fun setIgnored(aaveUser: AaveUser, ignored: Boolean) {
        aaveUserRepository.findByIdOrNull(aaveUser.address)?.let {
            aaveUserRepository.save(
                it.copy(
                    ignored = ignored
                )
            )
        }
    }

    @Transactional(readOnly = true)
    fun getRiskyUsers(): List<AaveUser> = aaveUserRepository.findRiskyUsers(1.03)
}