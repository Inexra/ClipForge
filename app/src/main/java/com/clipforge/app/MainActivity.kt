package com.clipforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0D0F14)
private val Panel = Color(0xFF171A22)
private val Accent = Color(0xFFB8F35A)
private val Muted = Color(0xFF9BA2B1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                onPrimary = Color(0xFF12150D),
                background = Background,
                surface = Panel,
                onSurface = Color(0xFFF4F5F7)
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    ClipForgeHome()
                }
            }
        }
    }
}

@Composable
private fun ClipForgeHome() {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = Background)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ClipForge", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("MAKE SOMETHING THAT MOVES", fontSize = 9.sp, color = Muted, letterSpacing = 1.4.sp)
            }
            Box(
                modifier = Modifier.clip(CircleShape).background(Panel).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("0.1 BETA", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your next", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("great edit starts here.", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Accent)
            Text("Create, cut, and polish videos right on your phone.", color = Muted, fontSize = 13.sp)
        }

        Button(
            onClick = { selectedTab = 1 },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create new project", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("QUICK TOOLS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text("Explore", color = Accent, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolChip("Trim", Icons.Default.ContentCut)
                ToolChip("Music", Icons.Default.LibraryMusic)
                ToolChip("Captions", Icons.Default.Subtitles)
                ToolChip("AI tools", Icons.Default.AutoAwesome)
                ToolChip("Adjust", Icons.Default.Tune)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RECENT PROJECTS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text("See all", color = Accent, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF242D3A), Color(0xFF20251D))))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.8f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF36475B), Color(0xFF8C9C78)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(42.dp))
                    }
                    Text("Your first masterpiece", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Start with a video or a few photos. Your projects will appear here.", color = Muted, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("Home", Icons.Default.Home, selectedTab == 0) { selectedTab = 0 }
            BottomItem("Projects", Icons.Default.VideoLibrary, selectedTab == 1) { selectedTab = 1 }
            BottomItem("AI Studio", Icons.Default.AutoAwesome, selectedTab == 2) { selectedTab = 2 }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ToolChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.width(78.dp).clip(RoundedCornerShape(16.dp))
            .background(Panel).clickable { }.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Accent, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BottomItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Accent else Muted)
        Text(label, fontSize = 10.sp, color = if (selected) Accent else Muted)
    }
}
