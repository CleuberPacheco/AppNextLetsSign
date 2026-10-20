package br.com.fiap.letssign.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.local.DocumentFullDetails
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.DocumentStatus
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.ui.components.DocumentStatusBadge
import br.com.fiap.letssign.ui.components.SignatureTypeBadge
import br.com.fiap.letssign.ui.components.SignerClassificationBadge
import br.com.fiap.letssign.ui.components.SignerFormDialog
import br.com.fiap.letssign.ui.theme.IcpBlue
import br.com.fiap.letssign.ui.theme.NavyPrimary
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.TealAccent
import br.com.fiap.letssign.ui.theme.WarningAmber
import br.com.fiap.letssign.util.PdfEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    adminUser: UserEntity,
    documents: List<DocumentFullDetails>,
    signers: List<UserEntity>,
    onStartNewDocument: () -> Unit,
    onOpenDocumentDetails: (Long) -> Unit,
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
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Documentos, 1 = Signatários
    var statusFilter by remember { mutableStateOf<DocumentStatus?>(null) }
    var showSignerDialog by remember { mutableStateOf(false) }
    var signerBeingEdited by remember { mutableStateOf<UserEntity?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LetsSign • Painel Admin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${adminUser.fullName} (${adminUser.email})",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB8C9E8)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair / Trocar Perfil",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Documentos (${documents.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("Signatários (${signers.size})") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onStartNewDocument,
                    containerColor = TealAccent,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    text = { Text("Enviar Documento PDF", fontWeight = FontWeight.Bold) }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = {
                        signerBeingEdited = null
                        showSignerDialog = true
                    },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Novo Signatário", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            AdminDocumentsTab(
                documents = documents,
                signersCount = signers.size,
                statusFilter = statusFilter,
                onFilterChange = { statusFilter = it },
                onStartNewDocument = onStartNewDocument,
                onOpenSignersTab = {
                    selectedTab = 1
                    signerBeingEdited = null
                    showSignerDialog = true
                },
                onOpenDocumentDetails = onOpenDocumentDetails,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            AdminSignersTab(
                signers = signers,
                onAddSigner = {
                    signerBeingEdited = null
                    showSignerDialog = true
                },
                onEditSigner = { signer ->
                    signerBeingEdited = signer
                    showSignerDialog = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AdminDocumentsTab(
    documents: List<DocumentFullDetails>,
    signersCount: Int,
    statusFilter: DocumentStatus?,
    onFilterChange: (DocumentStatus?) -> Unit,
    onStartNewDocument: () -> Unit,
    onOpenSignersTab: () -> Unit,
    onOpenDocumentDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sentCount = documents.count { it.document.status == DocumentStatus.ENVIADO }
    val partialCount = documents.count { it.document.status == DocumentStatus.PARCIALMENTE_ASSINADO }
    val completedCount = documents.count { it.document.status == DocumentStatus.CONCLUIDO }

    val filteredDocs = remember(documents, statusFilter) {
        if (statusFilter == null) documents
        else documents.filter { it.document.status == statusFilter }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        // Resumo de KPIs de Documentos
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricSummaryCard(
                    title = "Enviados",
                    count = sentCount,
                    color = IcpBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Parciais",
                    count = partialCount,
                    color = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Concluídos",
                    count = completedCount,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Alerta de orientação caso ainda não haja signatários cadastrados
        if (signersCount == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Passo Inicial Recomendado: Cadastre os Signatários",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Você ainda não possui signatários cadastrados. Cadastre pelo menos 1 signatário (ou crie durante o envio do PDF) para disparar solicitações de assinatura.",
                            fontSize = 12.sp,
                            color = Color(0xFF78350F)
                        )
                        OutlinedButton(
                            onClick = onOpenSignersTab,
                            border = BorderStroke(1.dp, WarningAmber)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cadastrar Signatário Agora", color = Color(0xFF92400E))
                        }
                    }
                }
            }
        }

        // Filtros por status (Enviados, Parcialmente Assinados, Concluídos)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("Todos (${documents.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = statusFilter == DocumentStatus.ENVIADO,
                    onClick = { onFilterChange(DocumentStatus.ENVIADO) },
                    label = { Text("Enviados ($sentCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IcpBlue,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = statusFilter == DocumentStatus.PARCIALMENTE_ASSINADO,
                    onClick = { onFilterChange(DocumentStatus.PARCIALMENTE_ASSINADO) },
                    label = { Text("Parcialmente Assinados ($partialCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarningAmber,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = statusFilter == DocumentStatus.CONCLUIDO,
                    onClick = { onFilterChange(DocumentStatus.CONCLUIDO) },
                    label = { Text("Concluídos ($completedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredDocs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = NavyPrimary.copy(alpha = 0.45f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhum documento encontrado neste filtro",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Inicie um novo fluxo importando um documento PDF do celular, selecione os signatários, posicione os campos de assinatura nas páginas e dispare o envio.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onStartNewDocument,
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar Fluxo de Documento PDF")
                        }
                    }
                }
            }
        } else {
            items(filteredDocs, key = { it.document.id }) { details ->
                DocumentSummaryCard(
                    details = details,
                    onClick = { onOpenDocumentDetails(details.document.id) }
                )
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DocumentSummaryCard(
    details: DocumentFullDetails,
    onClick: () -> Unit
) {
    val doc = details.document
    val totalSigners = details.signers.size
    val signedCount = details.signers.count { it.status == SignerStatus.ASSINADO }
    val progress = if (totalSigners > 0) signedCount.toFloat() / totalSigners.toFloat() else 0f

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFE8EEF8),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "v${doc.version}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = doc.title,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (doc.description.isNotBlank()) {
                        Text(
                            text = doc.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                DocumentStatusBadge(status = doc.status)
            }

            // Progresso de assinaturas
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Assinaturas coletadas: $signedCount de $totalSigners",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "${details.fields.size} campo(s) • ${doc.pageCount} pág.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = if (signedCount == totalSigners && totalSigners > 0) SuccessGreen else TealAccent,
                    trackColor = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            // Lista resumida dos signatários e tipos de assinatura
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                details.signers.forEach { signer ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            SignerClassificationBadge(signer.classification)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = signer.signerName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        SignatureTypeBadge(signer.signatureType)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enviado em ${PdfEngine.formatTimestamp(doc.sentAt ?: doc.createdAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Text(
                    text = "Ver Acompanhamento →",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealAccent
                )
            }
        }
    }
}

@Composable
private fun AdminSignersTab(
    signers: List<UserEntity>,
    onAddSigner: () -> Unit,
    onEditSigner: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3FB)),
                border = BorderStroke(1.dp, Color(0xFFD0DCF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Base de Signatários Jurídicos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Cadastre e edite as partes que assinarão os documentos (Contratantes, Contratados e Testemunhas). Cada signatário possui Nome completo, E-mail, Telefone e Senha para autenticar no app e assinar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (signers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = NavyPrimary.copy(alpha = 0.45f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhum signatário cadastrado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque no botão abaixo para cadastrar seus signatários com Nome completo, E-mail, Telefone e Senha.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddSigner,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cadastrar Primeiro Signatário")
                        }
                    }
                }
            }
        } else {
            items(signers, key = { it.id }) { signer ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCE6FA))
                            ) {
                                Text(
                                    text = signer.fullName.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = signer.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = NavyPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = signer.email,
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = signer.phone + if (signer.legalDocument.isNotBlank()) " • ${signer.legalDocument}" else "",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onEditSigner(signer) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Signatário",
                                tint = NavyPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
