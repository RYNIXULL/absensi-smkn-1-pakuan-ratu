package com.absensi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.absensi.app.ui.absen.AbsenScreen
import com.absensi.app.ui.absen.AbsenViewModel
import com.absensi.app.ui.auth.DevicePendingScreen
import com.absensi.app.ui.auth.LoginScreen
import com.absensi.app.ui.auth.LoginViewModel
import com.absensi.app.ui.history.SiswaHistoryScreen
import com.absensi.app.ui.history.SiswaHistoryViewModel
import com.absensi.app.ui.operator.ClassesScreen
import com.absensi.app.ui.operator.ClassesViewModel
import com.absensi.app.ui.operator.LocationsScreen
import com.absensi.app.ui.operator.LocationsViewModel
import com.absensi.app.ui.operator.OperatorSchedulesScreen
import com.absensi.app.ui.operator.OperatorSchedulesViewModel
import com.absensi.app.ui.operator.SubjectsScreen
import com.absensi.app.ui.operator.SubjectsViewModel
import com.absensi.app.ui.operator.UsersScreen
import com.absensi.app.ui.operator.UsersViewModel
import com.absensi.app.ui.schedule.ScheduleScreen
import com.absensi.app.ui.schedule.ScheduleViewModel
import com.absensi.app.ui.sessions.SessionsScreen
import com.absensi.app.ui.sessions.SessionsViewModel
import com.absensi.app.ui.leave.SiswaLeaveScreen
import com.absensi.app.ui.leave.SiswaLeaveViewModel
import com.absensi.app.ui.leave.LeaveApprovalScreen
import com.absensi.app.ui.leave.LeaveApprovalViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object DevicePending : Screen("device_pending")
    data object SiswaHome : Screen("siswa_home")
    data object GuruHome : Screen("guru_home")
    data object OperatorHome : Screen("operator_home")
    data object OperatorUsers : Screen("operator_users")
    data object OperatorClasses : Screen("operator_classes")
    data object OperatorSubjects : Screen("operator_subjects")
    data object OperatorLocations : Screen("operator_locations")
    data object OperatorSchedules : Screen("operator_schedules")
    data object MySchedule : Screen("my_schedule")
    data object SiswaHistory : Screen("siswa_history")
    data object SiswaLeave : Screen("siswa_leave")
    data object LeaveApproval : Screen("leave_approval")
    data object Sessions : Screen("sessions")

    companion object {
        const val ABSEN_ROUTE = "absen/{sessionId}"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { role ->
                    val destination = when (role.lowercase()) {
                        "siswa" -> Screen.SiswaHome.route
                        "guru" -> Screen.GuruHome.route
                        "operator" -> Screen.OperatorHome.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onDevicePending = {
                    navController.navigate(Screen.DevicePending.route)
                }
            )
        }

        composable(Screen.DevicePending.route) {
            DevicePendingScreen(
                onBackToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.DevicePending.route) { inclusive = true }
                    }
                }
            )
        }

        // Siswa Home, Kartu Sesi Aktif, & Riwayat
        composable(Screen.SiswaHome.route) {
            val siswaHomeViewModel: SiswaHomeViewModel = hiltViewModel()
            SiswaHomeScreen(
                viewModel = siswaHomeViewModel,
                onNavigateToSchedule = {
                    navController.navigate(Screen.MySchedule.route)
                },
                onNavigateToAbsen = { sessionId ->
                    navController.navigate("absen/$sessionId")
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.SiswaHistory.route)
                },
                onNavigateToLeave = {
                    navController.navigate(Screen.SiswaLeave.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Layar Absensi Kamera & GPS (Siswa)
        composable(Screen.ABSEN_ROUTE) {
            val absenViewModel: AbsenViewModel = hiltViewModel()
            AbsenScreen(
                viewModel = absenViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSuccessFinish = {
                    navController.navigate(Screen.SiswaHome.route) {
                        popUpTo(Screen.SiswaHome.route) { inclusive = true }
                    }
                }
            )
        }

        // Layar Riwayat Kehadiran (Siswa)
        composable(Screen.SiswaHistory.route) {
            val historyViewModel: SiswaHistoryViewModel = hiltViewModel()
            SiswaHistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Layar Pengajuan Izin & Sakit (Siswa)
        composable(Screen.SiswaLeave.route) {
            val leaveViewModel: SiswaLeaveViewModel = hiltViewModel()
            SiswaLeaveScreen(
                viewModel = leaveViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Guru Home
        composable(Screen.GuruHome.route) {
            GuruHomeScreen(
                onNavigateToSchedule = {
                    navController.navigate(Screen.MySchedule.route)
                },
                onNavigateToSessions = {
                    navController.navigate(Screen.Sessions.route)
                },
                onNavigateToLeaveApproval = {
                    navController.navigate(Screen.LeaveApproval.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Personal Schedule Screen (Siswa & Guru)
        composable(Screen.MySchedule.route) {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            ScheduleScreen(
                viewModel = scheduleViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Operator Dashboard
        composable(Screen.OperatorHome.route) {
            OperatorHomeScreen(
                onNavigateToUsers = { navController.navigate(Screen.OperatorUsers.route) },
                onNavigateToClasses = { navController.navigate(Screen.OperatorClasses.route) },
                onNavigateToSubjects = { navController.navigate(Screen.OperatorSubjects.route) },
                onNavigateToLocations = { navController.navigate(Screen.OperatorLocations.route) },
                onNavigateToSchedules = { navController.navigate(Screen.OperatorSchedules.route) },
                onNavigateToSessions = { navController.navigate(Screen.Sessions.route) },
                onNavigateToLeaveApproval = { navController.navigate(Screen.LeaveApproval.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Persetujuan Izin Siswa (Guru & Operator)
        composable(Screen.LeaveApproval.route) {
            val approvalViewModel: LeaveApprovalViewModel = hiltViewModel()
            LeaveApprovalScreen(
                viewModel = approvalViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Kelola Sesi Absensi (Guru & Operator)
        composable(Screen.Sessions.route) {
            val sessionsViewModel: SessionsViewModel = hiltViewModel()
            SessionsScreen(
                viewModel = sessionsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Operator Master: Users
        composable(Screen.OperatorUsers.route) {
            val usersViewModel: UsersViewModel = hiltViewModel()
            UsersScreen(
                viewModel = usersViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Operator Master: Classes
        composable(Screen.OperatorClasses.route) {
            val classesViewModel: ClassesViewModel = hiltViewModel()
            ClassesScreen(
                viewModel = classesViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Operator Master: Subjects
        composable(Screen.OperatorSubjects.route) {
            val subjectsViewModel: SubjectsViewModel = hiltViewModel()
            SubjectsScreen(
                viewModel = subjectsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Operator Master: Locations
        composable(Screen.OperatorLocations.route) {
            val locationsViewModel: LocationsViewModel = hiltViewModel()
            LocationsScreen(
                viewModel = locationsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Operator Master: Schedules
        composable(Screen.OperatorSchedules.route) {
            val operatorSchedulesViewModel: OperatorSchedulesViewModel = hiltViewModel()
            OperatorSchedulesScreen(
                viewModel = operatorSchedulesViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
