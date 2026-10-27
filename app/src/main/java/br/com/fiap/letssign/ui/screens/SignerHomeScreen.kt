package br.com.fiap.letssign.ui.screens

import androidx.compose.material.icons.filled.Security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.local.DocumentFullDetails
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.SignerStatus
import br.com.fiap.letssign.ui.components.AssinaJaLogo
import br.com.fiap.letssign.ui.components.DocumentStatusBadge
import br.com.fiap.letssign.ui.components.SignatureTypeBadge
import br.com.fiap.letssign.ui.components.SignerClassificationBadge
import br.com.fiap.letssign.ui.theme.CardBorder
import br.com.fiap.letssign.ui.theme.CardWhite
import br.com.fiap.letssign.ui.theme.CharcoalText
import br.com.fiap.letssign.ui.theme.GoldChampagne
import br.com.fiap.letssign.ui.theme.IvoryBackground
import br.com.fiap.letssign.ui.theme.MutedText
import br.com.fiap.letssign.ui.theme.SuccessGreen
import br.com.fiap.letssign.ui.theme.WineLight
import br.com.fiap.letssign.ui.theme.WinePrimary
import br.com.fiap.letssign.util.PdfEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignerHomeScreen(
    signerUser: UserEntity,
    documents: List<DocumentFullDetails>,
    onOpenDocument: (Long) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pendentes, 1 = Assinados

    val pendingForMe = remember(documents, signerUser.id) {
        documents.filter { details ->
            details.signers.any { it.signerUserId == signerUser.id && it.status == SignerStatus.PENDENTE }
        }
    }

    val signedByMe = remember(documents, signerUser.id) {
        documents.filter { details ->
            details.signers.any { it.signerUserId == signerUser.id && it.status == SignerStatus.ASSINADO }
        }
    }

    val displayedDocs = if (selectedTab == 0) pendingForMe else signedByMe

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssinaJaLogo(
                            size = 30.dp,
                            showText = true,
                            isLightOnDark = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Portal do Signatário",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldChampagne
                            )
                            Text(
                                text = signerUser.fullName.take(24),
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
                                contentDescription = "Sair / Trocar Conta",
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
            NavigationBar(containerColor = CardWhite, tonalElevation = 6.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PendingActions, contentDescription = null) },
                    label = { Text("Pendentes (${pendingForMe.size})", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WinePrimary,
                        selectedTextColor = WinePrimary,
                        indicatorColor = WineLight,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("Já Assinados (${signedByMe.size})", fontWeight = FontWeight.SemiBold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(
                color = CardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0) "Documentos aguardando sua assinatura eletrônica:" else "Documentos que você já assinou com sucesso:",
                        fontSize = 12.5.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (displayedDocs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.CheckCircle else Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFFD4C8C2),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "Parabéns! Você não possui documentos pendentes para assinar." else "Você ainda não assinou nenhum documento.",
                            color = MutedText,
                            fontSize = 13.5.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedDocs, key = { it.document.id }) { docDetails ->
                        val doc = docDetails.document
                        val mySignerRecord = docDetails.signers.find { it.signerUserId == signerUser.id }
                        val isPendingForMe = mySignerRecord?.status == SignerStatus.PENDENTE

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 7.dp, pressedElevation = 9.dp),
                            border = BorderStroke(0.8.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDocument(doc.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Linha superior: Título (max 30 caracteres, 1 linha) e à direita Versão + Escudo discreto
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                        Text(
                                            text = doc.title.take(30),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (doc.description.isNotBlank()) {
                                            Text(
                                                text = doc.description,
                                                fontSize = 11.5.sp,
                                                color = MutedText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Lado direito: Versão e abaixo o escudo discreto sem texto
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "v${doc.version}",
                                            fontSize = 11.5.sp,
                                            color = MutedText,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Autenticação Segura",
                                            tint = WinePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Tags de classificação: Contratante, Contratado ou Testemunha
                                if (mySignerRecord != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SignerClassificationBadge(mySignerRecord.classification)
                                        DocumentStatusBadge(status = doc.status)
                                    }
                                }

                                // Texto Enviado em
                                Text(
                                    text = "Enviado em ${PdfEngine.formatTimestamp(doc.sentAt ?: doc.createdAt)}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Botão Revisar e Assinar alinhado à esquerda para facilitar o clique
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isPendingForMe) {
                                        Button(
                                            onClick = { onOpenDocument(doc.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Draw,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Revisar e Assinar",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFFE8F7EE),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.6.dp, SuccessGreen.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Assinado por Você",
                                                    color = SuccessGreen,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
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
        }
    }
}
