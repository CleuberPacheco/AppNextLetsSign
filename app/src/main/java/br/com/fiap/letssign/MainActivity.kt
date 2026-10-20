package br.com.fiap.letssign

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.presentation.LetsSignViewModel
import br.com.fiap.letssign.ui.screens.AdminHomeScreen
import br.com.fiap.letssign.ui.screens.CreateDocumentWizardScreen
import br.com.fiap.letssign.ui.screens.DocumentDetailAndSignScreen
import br.com.fiap.letssign.ui.screens.LoginScreen
import br.com.fiap.letssign.ui.screens.SignerHomeScreen
import br.com.fiap.letssign.ui.theme.LetsSignTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LetsSignApplication
        setContent {
            LetsSignTheme {
                val viewModel: LetsSignViewModel = viewModel(
                    factory = LetsSignViewModel.Factory(app.repository)
                )
                LetsSignAppNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LetsSignAppNavigation(viewModel: LetsSignViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val currentUser by viewModel.currentUser.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val uiNotification by viewModel.uiNotification.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allSigners by viewModel.allSigners.collectAsState()
    val adminDocs by viewModel.adminDocuments.collectAsState()
    val signerDocs by viewModel.signerDocuments.collectAsState()
    val wizardState by viewModel.wizardState.collectAsState()

    LaunchedEffect(uiNotification) {
        uiNotification?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearNotification()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                allUsers = allUsers,
                authError = authError,
                onClearError = { viewModel.clearAuthError() },
                onLogin = { email, password ->
                    viewModel.login(email, password) { user ->
                        val destination = if (user.role == UserRole.ADMIN) "admin_home" else "signer_home"
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("admin_home") {
            val user = currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                AdminHomeScreen(
                    adminUser = user,
                    documents = adminDocs,
                    signers = allSigners,
                    onStartNewDocument = {
                        viewModel.startNewDocumentWizard()
                        navController.navigate("wizard")
                    },
                    onOpenDocumentDetails = { docId ->
                        navController.navigate("document_detail/$docId")
                    },
                    onSaveSigner = { editingId, fullName, email, phone, password, legalDoc, onSuccess, onError ->
                        viewModel.saveSigner(
                            editingId = editingId,
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            password = password,
                            legalDocument = legalDoc,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("signer_home") {
            val user = currentUser
            if (user == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                SignerHomeScreen(
                    signerUser = user,
                    documents = signerDocs,
                    onOpenDocument = { docId ->
                        navController.navigate("document_detail/$docId")
                    },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("wizard") {
            CreateDocumentWizardScreen(
                wizardState = wizardState,
                availableSigners = allSigners,
                onUpdateTitle = viewModel::updateWizardTitle,
                onUpdateDescription = viewModel::updateWizardDescription,
                onUpdateInvitationMessage = viewModel::updateWizardInvitationMessage,
                onImportPdfUri = { uri -> viewModel.importPdfFromUri(context, uri) },
                onGenerateSamplePdf = { viewModel.generateSampleLegalContractPdf(context) },
                onSimulateCorruptedPdf = viewModel::simulateCorruptedPdfSelection,
                onAddSignerToDraft = viewModel::addSignerToDraft,
                onRemoveSignerFromDraft = viewModel::removeSignerFromDraft,
                onReplaceSignerInDraft = viewModel::replaceSignerInDraft,
                onUpdateSignerClassification = viewModel::updateSignerClassification,
                onUpdateSignerSignatureType = viewModel::updateSignerSignatureType,
                onSaveNewSigner = { editingId, fullName, email, phone, password, legalDoc, onSuccess, onError ->
                    viewModel.saveSigner(
                        editingId = editingId,
                        fullName = fullName,
                        email = email,
                        phone = phone,
                        password = password,
                        legalDocument = legalDoc,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                },
                onSetActiveSignerForPlacement = viewModel::setActiveSignerForPlacement,
                onSetPageIndex = viewModel::setWizardPageIndex,
                onAddFieldToPage = viewModel::addSignatureFieldToCurrentPage,
                onMoveField = viewModel::updateSignatureFieldPosition,
                onResizeField = viewModel::updateSignatureFieldDimensions,
                onRemoveField = viewModel::removeSignatureField,
                onSetStep = viewModel::setWizardStep,
                onToggleSimulateFailure = viewModel::toggleSimulateFailureOnNextAttempt,
                onDispatchDocument = {
                    viewModel.dispatchDocument { createdDocId ->
                        navController.navigate("document_detail/$createdDocId") {
                            popUpTo("admin_home") { inclusive = false }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "document_detail/{docId}",
            arguments = listOf(navArgument("docId") { type = NavType.LongType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            val user = currentUser
            val detailsFlow = viewModel.observeDocumentDetails(docId)
            val details by detailsFlow.collectAsState(initial = null)

            if (user != null) {
                DocumentDetailAndSignScreen(
                    currentUser = user,
                    details = details,
                    onRecordViewed = viewModel::recordDocumentViewed,
                    onSignDocument = { bmp, token, certInfo, onSuccess, onError ->
                        viewModel.signDocumentAsCurrentSigner(
                            context = context,
                            documentId = docId,
                            signatureBitmap = bmp,
                            verificationToken = token,
                            certificateInfo = certInfo,
                            onSuccess = { onSuccess() },
                            onError = onError
                        )
                    },
                    onCreateNewVersion = { existingDetails ->
                        viewModel.startNewVersionFromDocument(existingDetails) {
                            navController.navigate("wizard")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
