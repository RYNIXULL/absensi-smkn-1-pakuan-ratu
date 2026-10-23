package com.absensi.app.data.repository

import com.absensi.app.data.remote.MasterDataApi
import com.absensi.app.data.remote.dto.AddClassMembersRequest
import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.ClassMemberDto
import com.absensi.app.data.remote.dto.CreateClassRequest
import com.absensi.app.data.remote.dto.CreateLocationRequest
import com.absensi.app.data.remote.dto.CreateScheduleRequest
import com.absensi.app.data.remote.dto.CreateSubjectRequest
import com.absensi.app.data.remote.dto.CreateUserRequest
import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.ResetPasswordResponse
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.SubjectDto
import com.absensi.app.data.remote.dto.UpdateClassRequest
import com.absensi.app.data.remote.dto.UpdateLocationRequest
import com.absensi.app.data.remote.dto.UpdateScheduleRequest
import com.absensi.app.data.remote.dto.UpdateSubjectRequest
import com.absensi.app.data.remote.dto.UserImportResponse
import com.absensi.app.data.remote.dto.UserListItemDto
import com.absensi.app.ui.common.ErrorMapper
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

interface MasterDataRepository {
    // Users
    suspend fun getUsers(page: Int = 1, pageSize: Int = 20, q: String? = null, role: String? = null): Result<PaginatedResponse<UserListItemDto>>
    suspend fun createUser(nama: String, nomorInduk: String, email: String?, role: String, password: String?): Result<UserListItemDto>
    suspend fun resetPassword(userId: String): Result<ResetPasswordResponse>
    suspend fun importUsers(file: MultipartBody.Part): Result<UserImportResponse>

    // Classes
    suspend fun getClasses(page: Int = 1, pageSize: Int = 20, q: String? = null, tahunAjaran: String? = null): Result<PaginatedResponse<ClassDto>>
    suspend fun createClass(nama: String, tahunAjaran: String): Result<ClassDto>
    suspend fun updateClass(id: String, nama: String?, tahunAjaran: String?): Result<ClassDto>
    suspend fun deleteClass(id: String): Result<String>
    suspend fun getClassMembers(classId: String): Result<List<ClassMemberDto>>
    suspend fun addClassMembers(classId: String, userIds: List<String>): Result<String>
    suspend fun removeClassMember(classId: String, userId: String): Result<String>

    // Subjects
    suspend fun getSubjects(page: Int = 1, pageSize: Int = 20, q: String? = null): Result<PaginatedResponse<SubjectDto>>
    suspend fun createSubject(nama: String): Result<SubjectDto>
    suspend fun updateSubject(id: String, nama: String): Result<SubjectDto>
    suspend fun deleteSubject(id: String): Result<String>

    // Locations
    suspend fun getLocations(page: Int = 1, pageSize: Int = 20, q: String? = null, isActive: String? = null): Result<PaginatedResponse<LocationDto>>
    suspend fun createLocation(nama: String, latitude: Double, longitude: Double, radiusM: Int, isActive: Boolean): Result<LocationDto>
    suspend fun updateLocation(id: String, nama: String?, latitude: Double?, longitude: Double?, radiusM: Int?, isActive: Boolean?): Result<LocationDto>
    suspend fun deleteLocation(id: String): Result<String>

    // Schedules
    suspend fun getSchedules(page: Int = 1, pageSize: Int = 20, classId: String? = null, teacherId: String? = null, dayOfWeek: Int? = null, q: String? = null): Result<PaginatedResponse<ScheduleDto>>
    suspend fun createSchedule(classId: String, subjectId: String, teacherId: String, locationId: String, dayOfWeek: Int, startTime: String, endTime: String): Result<ScheduleDto>
    suspend fun updateSchedule(id: String, classId: String?, subjectId: String?, teacherId: String?, locationId: String?, dayOfWeek: Int?, startTime: String?, endTime: String?): Result<ScheduleDto>
    suspend fun deleteSchedule(id: String): Result<String>
    suspend fun getMeSchedules(): Result<List<ScheduleDto>>
}

@Singleton
class MasterDataRepositoryImpl @Inject constructor(
    private val api: MasterDataApi,
    private val json: Json
) : MasterDataRepository {

    private fun <T> handleError(e: Throwable): Result<T> {
        val message = ErrorMapper.parseError(e, json)
        return Result.failure(Exception(message))
    }

    private fun <T> handleResponse(response: retrofit2.Response<T>): T {
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        }
        throw HttpException(response)
    }

    // Users
    override suspend fun getUsers(page: Int, pageSize: Int, q: String?, role: String?): Result<PaginatedResponse<UserListItemDto>> {
        return try {
            val response = api.getUsers(page, pageSize, q, role)
            Result.success(handleResponse(response))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun createUser(nama: String, nomorInduk: String, email: String?, role: String, password: String?): Result<UserListItemDto> {
        return try {
            val res = api.createUser(CreateUserRequest(nama, nomorInduk, email, role, password))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun resetPassword(userId: String): Result<ResetPasswordResponse> {
        return try {
            val res = api.resetPassword(userId)
            Result.success(handleResponse(res))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun importUsers(file: MultipartBody.Part): Result<UserImportResponse> {
        return try {
            val res = api.importUsers(file)
            Result.success(handleResponse(res))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    // Classes
    override suspend fun getClasses(page: Int, pageSize: Int, q: String?, tahunAjaran: String?): Result<PaginatedResponse<ClassDto>> {
        return try {
            val res = api.getClasses(page, pageSize, q, tahunAjaran)
            Result.success(handleResponse(res))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun createClass(nama: String, tahunAjaran: String): Result<ClassDto> {
        return try {
            val res = api.createClass(CreateClassRequest(nama, tahunAjaran))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun updateClass(id: String, nama: String?, tahunAjaran: String?): Result<ClassDto> {
        return try {
            val res = api.updateClass(id, UpdateClassRequest(nama, tahunAjaran))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun deleteClass(id: String): Result<String> {
        return try {
            val res = api.deleteClass(id)
            Result.success(handleResponse(res).message)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun getClassMembers(classId: String): Result<List<ClassMemberDto>> {
        return try {
            val res = api.getClassMembers(classId)
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun addClassMembers(classId: String, userIds: List<String>): Result<String> {
        return try {
            val res = api.addClassMembers(classId, AddClassMembersRequest(userIds))
            Result.success(handleResponse(res).message)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun removeClassMember(classId: String, userId: String): Result<String> {
        return try {
            val res = api.removeClassMember(classId, userId)
            Result.success(handleResponse(res).message)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    // Subjects
    override suspend fun getSubjects(page: Int, pageSize: Int, q: String?): Result<PaginatedResponse<SubjectDto>> {
        return try {
            val res = api.getSubjects(page, pageSize, q)
            Result.success(handleResponse(res))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun createSubject(nama: String): Result<SubjectDto> {
        return try {
            val res = api.createSubject(CreateSubjectRequest(nama))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun updateSubject(id: String, nama: String): Result<SubjectDto> {
        return try {
            val res = api.updateSubject(id, UpdateSubjectRequest(nama))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun deleteSubject(id: String): Result<String> {
        return try {
            val res = api.deleteSubject(id)
            Result.success(handleResponse(res).message)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    // Locations
    override suspend fun getLocations(page: Int, pageSize: Int, q: String?, isActive: String?): Result<PaginatedResponse<LocationDto>> {
        return try {
            val res = api.getLocations(page, pageSize, q, isActive)
            Result.success(handleResponse(res))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun createLocation(nama: String, latitude: Double, longitude: Double, radiusM: Int, isActive: Boolean): Result<LocationDto> {
        return try {
            val res = api.createLocation(CreateLocationRequest(nama, latitude, longitude, radiusM, isActive))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun updateLocation(id: String, nama: String?, latitude: Double?, longitude: Double?, radiusM: Int?, isActive: Boolean?): Result<LocationDto> {
        return try {
            val res = api.updateLocation(id, UpdateLocationRequest(nama, latitude, longitude, radiusM, isActive))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun deleteLocation(id: String): Result<String> {
        return try {
            val res = api.deleteLocation(id)
            Result.success(handleResponse(res).message)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    // Schedules
    override suspend fun getSchedules(page: Int, pageSize: Int, classId: String?, teacherId: String?, dayOfWeek: Int?, q: String?): Result<PaginatedResponse<ScheduleDto>> {
        return try {
            val res = api.getSchedules(page, pageSize, classId, teacherId, dayOfWeek, q)
            Result.success(handleResponse(res))
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun createSchedule(classId: String, subjectId: String, teacherId: String, locationId: String, dayOfWeek: Int, startTime: String, endTime: String): Result<ScheduleDto> {
        return try {
            val res = api.createSchedule(CreateScheduleRequest(classId, subjectId, teacherId, locationId, dayOfWeek, startTime, endTime))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun updateSchedule(id: String, classId: String?, subjectId: String?, teacherId: String?, locationId: String?, dayOfWeek: Int?, startTime: String?, endTime: String?): Result<ScheduleDto> {
        return try {
            val res = api.updateSchedule(id, UpdateScheduleRequest(classId, subjectId, teacherId, locationId, dayOfWeek, startTime, endTime))
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun deleteSchedule(id: String): Result<String> {
        return try {
            val res = api.deleteSchedule(id)
            Result.success(handleResponse(res).message)
        } catch (e: Throwable) {
            handleError(e)
        }
    }

    override suspend fun getMeSchedules(): Result<List<ScheduleDto>> {
        return try {
            val res = api.getMeSchedules()
            Result.success(handleResponse(res).data)
        } catch (e: Throwable) {
            handleError(e)
        }
    }
}
