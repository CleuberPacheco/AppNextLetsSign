package br.com.fiap.letssign.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.local.AuditEventEntity
import br.com.fiap.letssign.data.local.DocumentFullDetails
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.ui.components.AssinaJaLogo
import br.com.fiap.letssign.ui.components.AssinaJaMetricCard
import br.com.fiap.letssign.ui.components.DocumentStatusBadge
import br.com.fiap.letssign.ui.components.SignatureTypeBadge
import br.com.fiap.letssign.ui.components.SignerClassificationBadge
import br.com.fiap.letssign.ui.components.SignerFormDialog
import br.com.fiap.letssign.ui.theme.CardBorder
import br.com.fiap.letssign.ui.theme.CardWhite
import br.com.fiap.letssign.ui.theme.CharcoalText
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.GoldChampagne
import br.com.fiap.letssign.ui.theme.GoldLight
import br.com.fiap.letssign.ui.theme.IvoryBackground
import br.com.fiap.letssign.ui.theme.MutedText
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.WarningAmber
import br.com.fiap.letssign.ui.theme.WineDark
import br.com.fiap.letssign.ui.theme.WineLight
import br.com.fiap.letssign.ui.theme.WinePrimary
import br.com.fiap.letssign.util.PdfEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    adminUser: UserEntity,
    documents: List<DocumentFullDetails>,
    signers: List<UserEntity>,
    auditEvents: List<AuditEventEntity> = emptyList(),
    onStartNewDocument: () -> Unit,
    onImportPdfFromGallery: (Uri) -> Unit,
    onOpenDocumentDetails: (Long) -> Unit,
    onDeleteSigner: (userId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onSaveSigner: (
        editingId: Long?,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        legalDocument: String,
        onSuccess: (UserEntity) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onRecordShare: (documentId: Long, channel: String) -> Unit = { _, _ -> },
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Documentos, 2: Importar, 3: Signatários, 4: Auditoria
    var statusFilter by remember { mutableStateOf<DocumentStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSignerDialog by remember { mutableStateOf(false) }
    var signerBeingEdited by remember { mutableStateOf<UserEntity?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Seletor de arquivo PDF nativo do celular
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportPdfFromGallery(uri)
        }
    }

    if (showSignerDialog) {
        SignerFormDialog(
            initialSigner = signerBeingEdited,
            onDismiss = {
                showSignerDialog = false
                signerBeingEdited = null
            },
            onConfirm = { editingId, fullName, email, phone, password, legalDoc, setError ->
                onSaveSigner(
                    editingId,
                    fullName,
                    email,
                    phone,
                    password,
                    legalDoc,
                    {
                        showSignerDialog = false
                        signerBeingEdited = null
                    },
                    { err -> setError(err) }
                )
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssinaJaLogo(size = 26.dp, showText = false)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Novo Documento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WinePrimary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Selecione como deseja importar o documento para coleta de assinaturas:",
                        fontSize = 12.5.sp,
                        color = CharcoalText
                    )

                    // Opção 1: Selecionar PDF do celular
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        border = BorderStroke(1.dp, WinePrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = WineLight,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = WinePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Arquivo PDF do Dispositivo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "Escolher arquivo da galeria/arquivos",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = WinePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Opção 2: Gerar Minuta de Teste
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        border = BorderStroke(1.dp, GoldChampagne.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                onStartNewDocument()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GoldLight,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF7A5816),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Minuta de Teste Corporativa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "Modelo pronto de contrato de 2 páginas",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = GoldChampagne,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar / Fechar", color = WinePrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssinaJaLogo(
                            size = 32.dp,
                            showText = true,
                            isLightOnDark = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Painel Gestor",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldChampagne
                            )
                            Text(
                                text = adminUser.fullName.take(24),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sair",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Sair",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldChampagne
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WinePrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardWhite,
                tonalElevation = 8.dp
            ) {
                // 1. Dashboard (Esquerda)
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.5.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WinePrimary,
                        selectedTextColor = WinePrimary,
                        indicatorColor = WineLight,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )

                // 2. Documentos (Esquerda)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Documentos") },
                    label = { Text("Documentos", fontSize = 10.5.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WinePrimary,
                        selectedTextColor = WinePrimary,
                        indicatorColor = WineLight,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )

                // 3. Botão Central de Destaque: Importar PDF da Galeria
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        showImportDialog = true
                    },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = WinePrimary,
                            border = BorderStroke(2.dp, GoldChampagne),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Importar PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = "Importar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WinePrimary
                        )
                    }
                )

                // 4. Signatários (Direita)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Signatários") },
                    label = { Text("Signatários", fontSize = 10.5.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WinePrimary,
                        selectedTextColor = WinePrimary,
                        indicatorColor = WineLight,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )

                // 5. Auditoria (Direita)
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Auditoria") },
                    label = { Text("Auditoria", fontSize = 10.5.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WinePrimary,
                        selectedTextColor = WinePrimary,
                        indicatorColor = WineLight,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
            }
        },
        containerColor = IvoryBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    documents = documents,
                    onNavigateToDocuments = { filter ->
                        statusFilter = filter
                        selectedTab = 1
                    },
                    onOpenDocument = onOpenDocumentDetails,
                    onStartNew = onStartNewDocument,
                    onPickPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )
                1 -> DocumentsTab(
                    documents = documents,
                    selectedStatusFilter = statusFilter,
                    onStatusFilterChange = { statusFilter = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onOpenDocument = onOpenDocumentDetails,
                    onShareInvite = { doc, channel ->
                        onRecordShare(doc.document.id, channel)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Olá! Você foi convidado para assinar o documento '${doc.document.title}' na plataforma AssinaJÁ. Acesse pelo aplicativo com seu login."
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Enviar convite via $channel"))
                    }
                )
                3 -> SignersTab(
                    signers = signers,
                    onDeleteSigner = onDeleteSigner,
                    onAddSigner = {
                        signerBeingEdited = null
                        showSignerDialog = true
                    },
                    onEditSigner = { s ->
                        signerBeingEdited = s
                        showSignerDialog = true
                    }
                )
                4 -> AuditTab(
                    auditEvents = auditEvents,
                    documents = documents
                )
            }
        }
    }
}

/**
 * ABA 0: Dashboard Executiva com Cards de Métricas em Tempo Real
 */
@Composable
private fun DashboardTab(
    documents: List<DocumentFullDetails>,
    onNavigateToDocuments: (DocumentStatus?) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onStartNew: () -> Unit,
    onPickPdf: () -> Unit
) {
    val totalCreated = documents.size
    val pendingCount = documents.count { it.document.status == DocumentStatus.ENVIADO }
    val partiallyCount = documents.count { it.document.status == DocumentStatus.PARCIALMENTE_ASSINADO }
    val completedCount = documents.count { it.document.status == DocumentStatus.CONCLUIDO }
    val rejectedCount = documents.count { it.document.status == DocumentStatus.REJEITADO }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner de Boas-Vindas Executivo
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WineDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gestão Corporativa de Documentos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldChampagne
                        )
                        Text(
                            text = "Visão Geral de Assinaturas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GoldChampagne,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPickPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = WineDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar PDF", color = WineDark, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = onStartNew,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Criar Minuta", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // Seção: Indicadores Principais
        Text(
            text = "INDICADORES DE FLUXO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WinePrimary,
            letterSpacing = 0.8.sp
        )

        // Linha 1 de Cards: Total e Pendentes
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssinaJaMetricCard(
                title = "Total de Documentos",
                count = totalCreated,
                icon = Icons.Default.Description,
                accentColor = WinePrimary,
                bgColor = WineLight,
                subText = "Cadastrados no app",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDocuments(null) }
            )

            AssinaJaMetricCard(
                title = "Pendentes de Assinatura",
                count = pendingCount,
                icon = Icons.Default.HourglassTop,
                accentColor = WarningAmber,
                bgColor = Color(0xFFFFF7E6),
                subText = "Aguardando 1ª ação",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDocuments(DocumentStatus.ENVIADO) }
            )
        }

        // Linha 2 de Cards: Parcialmente Assinados e Concluídos
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssinaJaMetricCard(
                title = "Parcialmente Assinados",
                count = partiallyCount,
                icon = Icons.Default.Edit,
                accentColor = GoldChampagne,
                bgColor = GoldLight,
                subText = "Faltam signatários",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDocuments(DocumentStatus.PARCIALMENTE_ASSINADO) }
            )

            AssinaJaMetricCard(
                title = "Concluídos & Selados",
                count = completedCount,
                icon = Icons.Default.CheckCircle,
                accentColor = SuccessGreen,
                bgColor = Color(0xFFE8F7EE),
                subText = "Com selo e manifesto",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDocuments(DocumentStatus.CONCLUIDO) }
            )
        }

        // Linha 3 de Cards: Rejeitados
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssinaJaMetricCard(
                title = "Documentos Rejeitados",
                count = rejectedCount,
                icon = Icons.Default.Cancel,
                accentColor = ErrorRed,
                bgColor = Color(0xFFFFEBEE),
                subText = "Recusados formalmente",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDocuments(DocumentStatus.REJEITADO) }
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToDocuments(null) }
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Taxa de Sucesso",
                        fontSize = 12.sp,
                        color = MutedText,
                        fontWeight = FontWeight.SemiBold
                    )
                    val rate = if (totalCreated > 0) ((completedCount.toFloat() / totalCreated) * 100).toInt() else 100
                    Text(
                        text = "$rate%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WinePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCreated > 0) (completedCount.toFloat() / totalCreated) else 1f },
                        color = SuccessGreen,
                        trackColor = Color(0xFFEAE5E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Documentos Recentes
        Text(
            text = "DOCUMENTOS RECENTES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WinePrimary,
            letterSpacing = 0.8.sp
        )

        if (documents.isEmpty()) {
            Surface(
                color = CardWhite,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Nenhum documento cadastrado ainda.",
                        color = MutedText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onPickPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = WinePrimary)
                    ) {
                        Text("Importar Meu Primeiro PDF")
                    }
                }
            }
        } else {
            documents.take(4).forEach { docDetails ->
                DocumentCardItem(
                    doc = docDetails,
                    onClick = { onOpenDocument(docDetails.document.id) },
                    onShare = null
                )
            }
        }
    }
}

/**
 * ABA 1: Listagem Completa de Documentos com Filtros e Busca
 */
@Composable
private fun DocumentsTab(
    documents: List<DocumentFullDetails>,
    selectedStatusFilter: DocumentStatus?,
    onStatusFilterChange: (DocumentStatus?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onShareInvite: (DocumentFullDetails, String) -> Unit
) {
    val filtered = documents.filter { d ->
        val matchesStatus = selectedStatusFilter == null || d.document.status == selectedStatusFilter
        val matchesSearch = searchQuery.isBlank() ||
            d.document.title.contains(searchQuery, ignoreCase = true) ||
            d.document.originalFileName.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de Busca
        Surface(
            color = CardWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar por título ou arquivo...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WinePrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chips de filtro
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { onStatusFilterChange(null) },
                        label = { Text("Todos (${documents.size})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WinePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == DocumentStatus.ENVIADO,
                        onClick = { onStatusFilterChange(DocumentStatus.ENVIADO) },
                        label = { Text("Pendentes (${documents.count { it.document.status == DocumentStatus.ENVIADO }})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmber,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == DocumentStatus.PARCIALMENTE_ASSINADO,
                        onClick = { onStatusFilterChange(DocumentStatus.PARCIALMENTE_ASSINADO) },
                        label = { Text("Parciais (${documents.count { it.document.status == DocumentStatus.PARCIALMENTE_ASSINADO }})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldChampagne,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == DocumentStatus.CONCLUIDO,
                        onClick = { onStatusFilterChange(DocumentStatus.CONCLUIDO) },
                        label = { Text("Concluídos (${documents.count { it.document.status == DocumentStatus.CONCLUIDO }})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == DocumentStatus.REJEITADO,
                        onClick = { onStatusFilterChange(DocumentStatus.REJEITADO) },
                        label = { Text("Rejeitados (${documents.count { it.document.status == DocumentStatus.REJEITADO }})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ErrorRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Lista
        if (filtered.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFFD0C8C2),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum documento encontrado para este filtro.",
                        color = MutedText,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.document.id }) { docDetails ->
                    DocumentCardItem(
                        doc = docDetails,
                        onClick = { onOpenDocument(docDetails.document.id) },
                        onShare = { channel -> onShareInvite(docDetails, channel) }
                    )
                }
            }
        }
    }
}

/**
 * ABA 3: Gestão de Signatários (Cadastro, Edição e Contato)
 */
@Composable
private fun SignersTab(
    signers: List<UserEntity>,
    onAddSigner: () -> Unit,
    onEditSigner: (UserEntity) -> Unit,
    onDeleteSigner: (userId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    var signerToDelete by remember { mutableStateOf<UserEntity?>(null) }

    if (signerToDelete != null) {
        val target = signerToDelete!!
        AlertDialog(
            onDismissRequest = { signerToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir Signatário", fontWeight = FontWeight.Bold, color = ErrorRed)
                }
            },
            text = {
                Text(
                    text = "Deseja realmente remover o signatário '' do quadro de signatários?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        signerToDelete = null
                        onDeleteSigner(id, {}, {})
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirmar Exclusão", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { signerToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    var searchSigner by remember { mutableStateOf("") }
    val filteredSigners = signers.filter { s ->
        searchSigner.isBlank() ||
            s.fullName.contains(searchSigner, ignoreCase = true) ||
            s.email.contains(searchSigner, ignoreCase = true) ||
            s.phone.contains(searchSigner)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = CardWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Quadro de Signatários",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WinePrimary
                        )
                        Text(
                            text = "${signers.size} pessoas cadastradas para assinar",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                    Button(
                        onClick = onAddSigner,
                        colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Novo Signatário", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchSigner,
                    onValueChange = { searchSigner = it },
                    placeholder = { Text("Buscar por nome, e-mail ou telefone...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WinePrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredSigners.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Text(
                    text = if (signers.isEmpty()) "Nenhum signatário cadastrado. Toque em 'Novo Signatário' para cadastrar." else "Nenhum resultado para a busca.",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSigners, key = { it.id }) { signer ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = WineLight,
                                border = BorderStroke(1.dp, GoldChampagne),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = signer.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = WinePrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = signer.fullName,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MutedText, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = signer.email,
                                        fontSize = 12.sp,
                                        color = MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MutedText, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = signer.phone,
                                        fontSize = 12.sp,
                                        color = MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (signer.legalDocument.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = GoldLight,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.6.dp, GoldChampagne.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "CPF: ${signer.legalDocument}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF6B4E1B),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEditSigner(signer) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = WinePrimary
                                    )
                                }
                                IconButton(onClick = { signerToDelete = signer }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ABA 4: Trilha de Auditoria Forense Completa (IPs, Horários, Links e Assinaturas)
 */
@Composable
private fun AuditTab(
    auditEvents: List<AuditEventEntity>,
    documents: List<DocumentFullDetails>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            color = CardWhite,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = WinePrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Trilha Forense de Auditoria",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = WinePrimary
                        )
                        Text(
                            text = "Histórico imutável de criação, links, acessos, IPs e assinaturas",
                            fontSize = 11.5.sp,
                            color = MutedText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (auditEvents.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Text(
                    text = "Nenhum evento registrado ainda. Ao criar e movimentar documentos, todas as ações aparecerão aqui.",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(auditEvents, key = { it.id }) { event ->
                    val relatedDoc = documents.find { it.document.id == event.documentId }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        border = BorderStroke(0.8.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = WineLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = event.eventType.label.uppercase(),
                                        color = WinePrimary,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = PdfEngine.formatTimestamp(event.timestamp),
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = event.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )

                            if (relatedDoc != null) {
                                Text(
                                    text = "Documento: ${relatedDoc.document.title}",
                                    fontSize = 11.5.sp,
                                    color = WinePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = event.description,
                                fontSize = 12.sp,
                                color = Color(0xFF333333)
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color(0xFFF0ECE8))
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Responsável: ${event.actorName} (${event.actorEmail})",
                                fontSize = 11.sp,
                                color = MutedText,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item de Documento reutilizável
 */
@Composable
private fun DocumentCardItem(
    doc: DocumentFullDetails,
    onClick: () -> Unit,
    onShare: ((channel: String) -> Unit)?
) {
    val signedCount = doc.signers.count { it.status == SignerStatus.ASSINADO }
    val totalSigners = doc.signers.size
    val progress = if (totalSigners > 0) signedCount.toFloat() / totalSigners else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                DocumentStatusBadge(doc.document.status)
                Text(
                    text = "v${doc.document.version} • ${doc.document.pageCount} pág.",
                    fontSize = 11.5.sp,
                    color = MutedText,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = doc.document.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (doc.document.description.isNotBlank()) {
                Text(
                    text = doc.document.description,
                    fontSize = 12.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de progresso de assinaturas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Assinaturas: $signedCount de $totalSigners",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WinePrimary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 1f) SuccessGreen else WinePrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = if (progress >= 1f) SuccessGreen else WinePrimary,
                trackColor = Color(0xFFF0ECE8),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rodapé do card: Signatários e botão de compartilhar
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Criado em ${PdfEngine.formatTimestamp(doc.document.createdAt)}",
                        fontSize = 11.sp,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (doc.document.status == DocumentStatus.CONCLUIDO || signedCount > 0) {
                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldLight,
                            border = BorderStroke(0.8.dp, GoldChampagne),
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    val fileName = "${doc.document.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)}_v${doc.document.version}_Assinado.pdf"
                                    val res = PdfEngine.exportPdfToPublicDownloads(context, doc.document.currentPdfPath, fileName)
                                    res.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "✦ Documento salvo em Downloads!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, "Erro: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = WinePrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Salvar PDF",
                                    color = WinePrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (onShare != null && doc.document.status != DocumentStatus.CONCLUIDO && doc.document.status != DocumentStatus.REJEITADO) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.12f),
                            border = BorderStroke(0.6.dp, Color(0xFF25D366)),
                            modifier = Modifier.clickable { onShare("WhatsApp") }
                        ) {
                            Text(
                                text = "WhatsApp",
                                color = Color(0xFF128C7E),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WineLight,
                            border = BorderStroke(0.6.dp, WinePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onShare("E-mail/SMS") }
                        ) {
                            Text(
                                text = "SMS/E-mail",
                                color = WinePrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
