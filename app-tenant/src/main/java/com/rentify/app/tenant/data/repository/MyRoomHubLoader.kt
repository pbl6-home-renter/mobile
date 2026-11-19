package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.api.IssueApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.invoice.InvoiceMyListResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubItemDto
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomHubContractDto
import com.rentify.app.core.network.dto.room.RoomHubInvoiceDto
import com.rentify.app.core.network.dto.room.RoomHubLandlordDto
import com.rentify.app.core.network.dto.room.RoomHubRoomDto
import com.rentify.app.core.network.dto.room.RoomHubRoommateDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class MyRoomHubLoader(
    private val roomRepository: TenantRoomRepository,
    private val invoiceRepository: TenantInvoiceRepository,
    private val contractRepository: TenantContractRepository,
    private val issueRepository: TenantIssueRepository,
    private val currentUserId: suspend () -> String?
) {

    suspend fun load(): NetworkResult<List<MyRoomHubResponseDto>> {
        return when (val result = roomRepository.getMyRoomHub()) {
            is NetworkResult.Success -> {
                val userId = currentUserId()
                NetworkResult.Success(
                    coroutineScope {
                        result.data.data.map { item -> async { item.toHub(userId) } }.awaitAll()
                    }
                )
            }
            is NetworkResult.ApiError -> result
            is NetworkResult.NetworkError -> result
            is NetworkResult.UnknownError -> result
        }
    }

    private suspend fun MyRoomHubItemDto.toHub(userId: String?): MyRoomHubResponseDto = coroutineScope {
        val detail = async { roomRepository.getRoomDetail(roomId) }
        val invoice = async { latestInvoiceOf(contractId) }
        val roommates = async { roommatesOf(contractId, userId) }
        val activeIssues = async { activeIssueCountOf(roomId) }

        val roomDetail = (detail.await() as? NetworkResult.Success)?.data?.data
        val landlord = roomDetail?.landlord

        MyRoomHubResponseDto(
            room = RoomHubRoomDto(
                id = roomId,
                name = roomName,
                area = area?.toString(),
                rentPrice = rentPrice.toLong(),
                buildingId = buildingId,
                buildingName = buildingName,
                address = listOf(address, ward, province).filter { it.isNotBlank() }.joinToString(", "),
                photoUrls = roomDetail?.photoUrls.orEmpty()
            ),
            landlord = landlord?.let {
                RoomHubLandlordDto(id = it.id, name = it.name, phone = it.phone, avatarUrl = it.avatarUrl)
            },
            contract = RoomHubContractDto(
                id = contractId,
                startDate = startDate,
                endDate = endDate
            ),
            currentInvoice = invoice.await(),
            roommates = roommates.await(),
            activeIssuesCount = activeIssues.await()
        )
    }

    private suspend fun latestInvoiceOf(contractId: String): RoomHubInvoiceDto? {
        val result = invoiceRepository.getMyInvoices(contractId = contractId, pageSize = INVOICE_PAGE_SIZE)
        if (result !is NetworkResult.Success) return null
        return result.data.data.maxByOrNull { it.period }?.toHubInvoice()
    }

    private suspend fun roommatesOf(contractId: String, userId: String?): List<RoomHubRoommateDto> {
        val result = contractRepository.getContractMembers(contractId)
        if (result !is NetworkResult.Success) return emptyList()
        return result.data.data
            .filter { it.status.equals(ACTIVE_STATUS, ignoreCase = true) && (userId == null || it.tenantId != userId) }
            .map {
                RoomHubRoommateDto(
                    id = it.tenantId.orEmpty(),
                    name = it.tenantName?.takeIf { name -> name.isNotBlank() } ?: DEFAULT_ROOMMATE_NAME,
                    phone = it.tenantPhone
                )
            }
    }

    private suspend fun activeIssueCountOf(roomId: String): Int {
        val result = issueRepository.getMyIssues(roomId = roomId, pageSize = ISSUE_PAGE_SIZE)
        if (result !is NetworkResult.Success) return 0
        return result.data.data.count { it.status in OPEN_ISSUE_STATUSES }
    }

    private fun InvoiceMyListResponseDto.toHubInvoice(): RoomHubInvoiceDto {
        return RoomHubInvoiceDto(
            id = id,
            code = code,
            period = period,
            totalAmount = totalAmountLong,
            remainingAmount = remainingAmount,
            dueDate = dueDate,
            invoiceStatus = invoiceStatus
        )
    }

    companion object {
        private const val INVOICE_PAGE_SIZE = 12
        private const val ISSUE_PAGE_SIZE = 50
        private const val ACTIVE_STATUS = "active"
        private const val DEFAULT_ROOMMATE_NAME = "Thành viên"
        private val OPEN_ISSUE_STATUSES = setOf("open", "in_progress")

        fun create(
            tokenDataStore: TokenDataStore,
            tokenProvider: () -> String?,
            roomRepository: TenantRoomRepository
        ): MyRoomHubLoader {
            return MyRoomHubLoader(
                roomRepository = roomRepository,
                invoiceRepository = TenantInvoiceRepositoryImpl(
                    NetworkClient.createService<InvoiceApiService>(tokenProvider = tokenProvider)
                ),
                contractRepository = TenantContractRepositoryImpl(
                    NetworkClient.createService<ContractApiService>(tokenProvider = tokenProvider)
                ),
                issueRepository = TenantIssueRepositoryImpl(
                    NetworkClient.createService<IssueApiService>(tokenProvider = tokenProvider)
                ),
                currentUserId = { tokenDataStore.userId.first() }
            )
        }
    }
}
