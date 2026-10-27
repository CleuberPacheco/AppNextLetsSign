package br.com.fiap.letssign.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.data.local.UserEntity
import br.com.fiap.letssign.domain.model.UserRole
import br.com.fiap.letssign.ui.components.AssinaJaLogo
import br.com.fiap.letssign.ui.theme.CardBorder
import br.com.fiap.letssign.ui.theme.CardWhite
import br.com.fiap.letssign.ui.theme.CharcoalText
import br.com.fiap.letssign.ui.theme.ErrorRed
import br.com.fiap.letssign.ui.theme.GoldChampagne
import br.com.fiap.letssign.ui.theme.GoldLight
import br.com.fiap.letssign.ui.theme.IvoryBackground
import br.com.fiap.letssign.ui.theme.MutedText
import br.com.fiap.letssign.ui.theme.WineDark
import br.com.fiap.letssign.ui.theme.WineLight
import br.com.fiap.letssign.ui.theme.WinePrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    allUsers: List<UserEntity>,
    authError: String?,
    onClearError: () -> Unit,
    onLogin: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("admin@assinaja.com.br") }
    var password by remember { mutableStateOf("admin123") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBackground)
            .verticalScroll(scrollState)
    ) {
        // Cabeçalho Hero Luxuoso AssinaJÁ em tom Vinho Nobre
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(WineDark, WinePrimary)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 34.dp)
        ) {
            Column {
                AssinaJaLogo(
                    size = 46.dp,
                    showText = true,
                    isLightOnDark = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Automação, Gestão e Assinatura Eletrônica e Digital",
                    fontSize = 13.5.sp,
                    color = GoldChampagne,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Acesso unificado e seguro para todas as empresas e signatários.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }
        }

        // Formulário de Login
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Entrar na Plataforma",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WinePrimary
                    )

                    Text(
                        text = "Informe seu e-mail e senha de acesso previamente cadastrados.",
                        fontSize = 12.sp,
                        color = MutedText
                    )

                    if (authError != null) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError,
                                color = ErrorRed,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearError()
                        },
                        label = { Text("E-mail corporativo ou pessoal") },
                        placeholder = { Text("Ex: seuemail@empresa.com.br") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = WinePrimary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        label = { Text("Senha de autenticação") },
                        placeholder = { Text("Digite sua senha") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = WinePrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Alternar visibilidade",
                                    tint = WinePrimary
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onLogin(email, password) },
                        colors = ButtonDefaults.buttonColors(containerColor = WinePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Autenticar com Segurança",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Atalhos rápidos para troca de perfil em testes
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldChampagne,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "USUÁRIOS DISPONÍVEIS NO DISPOSITIVO (MVP)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WinePrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Toque em uma credencial abaixo para preencher automaticamente:",
                        fontSize = 11.5.sp,
                        color = MutedText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Admin padrão
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = WineLight,
                            border = BorderStroke(1.dp, WinePrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable {
                                email = "admin@assinaja.com.br"
                                password = "admin123"
                                onClearError()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = WinePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Admin AssinaJÁ",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WinePrimary
                                )
                            }
                        }

                        // Signatários cadastrados
                        allUsers.filter { it.role == UserRole.SIGNATARIO }.forEach { user ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = GoldLight,
                                border = BorderStroke(1.dp, GoldChampagne.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    email = user.email
                                    password = user.password
                                    onClearError()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF7A5816),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${user.fullName.take(16)} (Signatário)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF5A4418)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rodapé Institucional
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "AssinaJÁ • Tecnologia Segura de Assinatura Digital e Eletrônica",
                    fontSize = 11.sp,
                    color = MutedText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Em conformidade com a MP 2.200-2/2001 e a Lei nº 14.063/2020",
                    fontSize = 10.sp,
                    color = MutedText.copy(alpha = 0.8f)
                )
            }
        }
    }
}
