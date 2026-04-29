package com.example.paisatracker.ui.tour

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.ui.theme.PaisaTrackerTheme
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun AppTourSheetPreview() {
    PaisaTrackerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AppTourSheet(onComplete = {})
        }
    }
}

data class TourPage(
    val emoji: String,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppTourSheet(
    onComplete: () -> Unit
) {
    val pages = listOf(
        TourPage(
            "🚀",
            "Welcome to PaisaTracker",
            "Your simple, beautiful, and secure expense manager. Let's get you set up in a few seconds!"
        ),
        TourPage(
            "📊",
            "Organize Your Finances",
            "Create Projects for major life areas (like Home or Work) and Categories to track specific spending habits."
        ),
        TourPage(
            "⚡",
            "Quick Add Everything",
            "Use the lightning-fast Quick Add button to log expenses in seconds. Never miss a single transaction again."
        ),
        TourPage(
            "🔒",
            "Total Privacy",
            "Your data stays on your device. Secure it with a PIN or Biometrics via the Security settings."
        ),
        TourPage(
            "♻️",
            "Safe Deletions",
            "Accidentally deleted something? Our Recycle Bin keeps your data safe for 30 days before permanent removal."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onComplete() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(140.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = page.emoji,
                                fontSize = 72.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Page Indicator
            Row(
                modifier = Modifier.height(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val isSelected = pagerState.currentPage == i
                    val width by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 10.dp,
                        label = "width"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.weight(2f),
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
