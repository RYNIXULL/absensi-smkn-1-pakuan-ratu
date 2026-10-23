package com.absensi.app

import com.absensi.app.data.remote.dto.ClassDto
import com.absensi.app.data.remote.dto.ClassMemberDto
import com.absensi.app.data.remote.dto.LocationDto
import com.absensi.app.data.remote.dto.PaginationMeta
import com.absensi.app.data.remote.dto.PaginatedResponse
import com.absensi.app.data.remote.dto.ResetPasswordResponse
import com.absensi.app.data.remote.dto.ScheduleDto
import com.absensi.app.data.remote.dto.SubjectDto
import com.absensi.app.data.remote.dto.UserImportResponse
import com.absensi.app.data.remote.dto.UserImportSummaryDto
import com.absensi.app.data.remote.dto.UserListItemDto
import com.absensi.app.data.repository.MasterDataRepository
import okhttp3.MultipartBody

class FakeMasterDataRepository : MasterDataRepository {

    var getClassesResult: Result<PaginatedResponse<ClassDto>> = Result.success(
        PaginatedResponse(data = emptyList(), pagination = PaginationMeta(1, 20, 0, 1))
    )
    var createClassResult: Result<ClassDto>? = null
    var updateClassResult: Result<ClassDto>? = null
    var deleteClassResult: Result<String> = Result.success("Kelas berhasil dihapus")
    var getClassMembersResult: Result<List<ClassMemberDto>> = Result.success(emptyList())
    var addClassMembersResult: Result<String> = Result.success("Anggota berhasil ditambahkan")
    var removeClassMemberResult: Result<String> = Result.success("Anggota berhasil dihapus")

    var getUsersResult: Result<PaginatedResponse<UserListItemDto>> = Result.success(
        PaginatedResponse(data = emptyList(), pagination = PaginationMeta(1, 20, 0, 1))
    )
    var createUserResult: Result<UserListItemDto>? = null
    var resetPasswordResult: Result<ResetPasswordResponse>? = null
    var importUsersResult: Result<UserImportResponse>? = null

    var getSubjectsResult: Result<PaginatedResponse<SubjectDto>> = Result.success(
        PaginatedResponse(data = emptyList(), pagination = PaginationMeta(1, 20, 0, 1))
    )
    var createSubjectResult: Result<SubjectDto>? = null
    var updateSubjectResult: Result<SubjectDto>? = null
    var deleteSubjectResult: Result<String> = Result.success("Mata pelajaran berhasil dihapus")

    var getLocationsResult: Result<PaginatedResponse<LocationDto>> = Result.success(
        PaginatedResponse(data = emptyList(), pagination = PaginationMeta(1, 20, 0, 1))
    )
    var createLocationResult: Result<LocationDto>? = null
    var updateLocationResult: Result<LocationDto>? = null
    var deleteLocationResult: Result<String> = Result.success("Lokasi berhasil dinonaktifkan")

    var getSchedulesResult: Result<PaginatedResponse<ScheduleDto>> = Result.success(
        PaginatedResponse(data = emptyList(), pagination = PaginationMeta(1, 20, 0, 1))
    )
    var createScheduleResult: Result<ScheduleDto>? = null
    var updateScheduleResult: Result<ScheduleDto>? = null
    var deleteScheduleResult: Result<String> = Result.success("Jadwal berhasil dihapus")
    var getMeSchedulesResult: Result<List<ScheduleDto>> = Result.success(emptyList())

    override suspend fun getUsers(page: Int, pageSize: Int, q: String?, role: String?): Result<PaginatedResponse<UserListItemDto>> {
        return getUsersResult
    }

    override suspend fun createUser(nama: String, nomorInduk: String, email: String?, role: String, password: String?): Result<UserListItemDto> {
        return createUserResult ?: Result.success(
            UserListItemDto(id = "user-new", nama = nama, nomor_induk = nomorInduk, role = role, email = email, is_active = true)
        )
    }

    override suspend fun resetPassword(userId: String): Result<ResetPasswordResponse> {
        return resetPasswordResult ?: Result.success(ResetPasswordResponse("temp1234", "Password berhasil di-reset"))
    }

    override suspend fun importUsers(file: MultipartBody.Part): Result<UserImportResponse> {
        return importUsersResult ?: Result.success(
            UserImportResponse(summary = UserImportSummaryDto(1, 1, 0), rows = emptyList(), excel_base64 = null)
        )
    }

    override suspend fun getClasses(page: Int, pageSize: Int, q: String?, tahunAjaran: String?): Result<PaginatedResponse<ClassDto>> {
        return getClassesResult
    }

    override suspend fun createClass(nama: String, tahunAjaran: String): Result<ClassDto> {
        return createClassResult ?: Result.success(ClassDto(id = "class-new", nama = nama, tahun_ajaran = tahunAjaran))
    }

    override suspend fun updateClass(id: String, nama: String?, tahunAjaran: String?): Result<ClassDto> {
        return updateClassResult ?: Result.success(ClassDto(id = id, nama = nama ?: "Updated", tahun_ajaran = tahunAjaran ?: "2024/2025"))
    }

    override suspend fun deleteClass(id: String): Result<String> {
        return deleteClassResult
    }

    override suspend fun getClassMembers(classId: String): Result<List<ClassMemberDto>> {
        return getClassMembersResult
    }

    override suspend fun addClassMembers(classId: String, userIds: List<String>): Result<String> {
        return addClassMembersResult
    }

    override suspend fun removeClassMember(classId: String, userId: String): Result<String> {
        return removeClassMemberResult
    }

    override suspend fun getSubjects(page: Int, pageSize: Int, q: String?): Result<PaginatedResponse<SubjectDto>> {
        return getSubjectsResult
    }

    override suspend fun createSubject(nama: String): Result<SubjectDto> {
        return createSubjectResult ?: Result.success(SubjectDto(id = "sub-new", nama = nama))
    }

    override suspend fun updateSubject(id: String, nama: String): Result<SubjectDto> {
        return updateSubjectResult ?: Result.success(SubjectDto(id = id, nama = nama))
    }

    override suspend fun deleteSubject(id: String): Result<String> {
        return deleteSubjectResult
    }

    override suspend fun getLocations(page: Int, pageSize: Int, q: String?, isActive: String?): Result<PaginatedResponse<LocationDto>> {
        return getLocationsResult
    }

    override suspend fun createLocation(nama: String, latitude: Double, longitude: Double, radiusM: Int, isActive: Boolean): Result<LocationDto> {
        return createLocationResult ?: Result.success(
            LocationDto(id = "loc-new", nama = nama, latitude = latitude, longitude = longitude, radius_m = radiusM, is_active = isActive)
        )
    }

    override suspend fun updateLocation(id: String, nama: String?, latitude: Double?, longitude: Double?, radiusM: Int?, isActive: Boolean?): Result<LocationDto> {
        return updateLocationResult ?: Result.success(
            LocationDto(id = id, nama = nama ?: "Loc", latitude = latitude ?: 0.0, longitude = longitude ?: 0.0, radius_m = radiusM ?: 50, is_active = isActive ?: true)
        )
    }

    override suspend fun deleteLocation(id: String): Result<String> {
        return deleteLocationResult
    }

    override suspend fun getSchedules(page: Int, pageSize: Int, classId: String?, teacherId: String?, dayOfWeek: Int?, q: String?): Result<PaginatedResponse<ScheduleDto>> {
        return getSchedulesResult
    }

    override suspend fun createSchedule(classId: String, subjectId: String, teacherId: String, locationId: String, dayOfWeek: Int, startTime: String, endTime: String): Result<ScheduleDto> {
        return createScheduleResult ?: Result.success(
            ScheduleDto(id = "sched-new", class_id = classId, subject_id = subjectId, teacher_id = teacherId, location_id = locationId, day_of_week = dayOfWeek, start_time = startTime, end_time = endTime)
        )
    }

    override suspend fun updateSchedule(id: String, classId: String?, subjectId: String?, teacherId: String?, locationId: String?, dayOfWeek: Int?, startTime: String?, endTime: String?): Result<ScheduleDto> {
        return updateScheduleResult ?: Result.success(
            ScheduleDto(id = id, class_id = classId ?: "c", subject_id = subjectId ?: "s", teacher_id = teacherId ?: "t", location_id = locationId ?: "l", day_of_week = dayOfWeek ?: 1, start_time = startTime ?: "07:00", end_time = endTime ?: "08:30")
        )
    }

    override suspend fun deleteSchedule(id: String): Result<String> {
        return deleteScheduleResult
    }

    override suspend fun getMeSchedules(): Result<List<ScheduleDto>> {
        return getMeSchedulesResult
    }
}
