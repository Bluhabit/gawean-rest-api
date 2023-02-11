package com.bluehabit.budgetku.data.wallet

import com.bluehabit.budgetku.common.ValidationUtil
import com.bluehabit.budgetku.common.exception.DataNotFoundException
import com.bluehabit.budgetku.common.model.BaseResponse
import com.bluehabit.budgetku.common.model.PagingDataResponse
import com.bluehabit.budgetku.common.model.baseResponse
import com.bluehabit.budgetku.common.model.pagingResponse
import com.bluehabit.budgetku.data.user.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus.OK
import org.springframework.stereotype.Service

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val validationUtil: ValidationUtil
) {
    suspend fun getListWalletByUser(
        userId:String,
        pageable: Pageable
    ):BaseResponse<PagingDataResponse<WalletResponse>> {
        val findUser = userRepository.findByIdOrNull(userId)
            ?: throw DataNotFoundException("Cannot find data user!")

        val findWalletByUser = walletRepository.findAllByUser(
            findUser,
            pageable
        )

        if(findWalletByUser.isEmpty) throw DataNotFoundException(
            "No account corresponding with user account ${findUser.userFullName}"
        )

        return baseResponse {
            code = OK.value()
            data = pagingResponse {
                page = findWalletByUser.number
                size = findWalletByUser.size
                items = findWalletByUser.toListResponse()
                totalData = findWalletByUser.totalElements
                totalPages = findWalletByUser.totalPages
            }
            message = "Success"
        }

    }
}