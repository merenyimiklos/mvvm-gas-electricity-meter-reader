package hu.merenyimiklos.meterreader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import hu.merenyimiklos.meterreader.R
import hu.merenyimiklos.meterreader.auth.AuthViewModel
import kotlinx.coroutines.launch

private data class TutorialPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val eyebrow: String,
    val previewTitle: String,
    val previewPrimary: String,
    val previewSecondary: String
)

@Composable
fun OnboardingScreen(
    authViewModel: AuthViewModel
) {
    val pages =
        listOf(
            TutorialPage(
                title =
                    t("Minden fontos adat egy helyen"),
                description =
                    t("Az Áttekintés képernyőn azonnal látod az áram-, gáz- és vízmérőket, a havi fogyasztást és a becsült költséget."),
                icon =
                    Icons.Default.Home,
                eyebrow =
                    t("ÁTTEKINTÉS"),
                previewTitle =
                    t("Aktuális mérőállások"),
                previewPrimary =
                    t("Normál áram · 12 486 kWh"),
                previewSecondary =
                    t("Becsült havi költség · 14 820 Ft")
            ),
            TutorialPage(
                title =
                    t("Fotózd le vagy írd be"),
                description =
                    t("A Rögzítésnél gyorsan felviheted az összes bekapcsolt mérőt, a vizet is, vagy fotóból felismerheted a mérőállást."),
                icon =
                    Icons.Default.CameraAlt,
                eyebrow =
                    t("RÖGZÍTÉS"),
                previewTitle =
                    t("Gyors havi leolvasás"),
                previewPrimary =
                    t("Normál ✓  Éjszakai ✓  Gáz ✓  Víz ○"),
                previewSecondary =
                    t("Fotós leolvasás és OCR")
            ),
            TutorialPage(
                title =
                    t("Minden mérés visszakereshető"),
                description =
                    t("A Naplóban szerkesztheted és törölheted a korábbi méréseket, megnézheted a fogyasztási különbséget és exportálhatsz is."),
                icon =
                    Icons.Default.ReceiptLong,
                eyebrow =
                    t("NAPLÓ"),
                previewTitle =
                    t("Mérési napló"),
                previewPrimary =
                    t("Október 23. · Gáz 1245 m³"),
                previewSecondary =
                    t("+18 m³ az előző mérés óta")
            ),
            TutorialPage(
                title =
                    t("Lásd, merre tart a fogyasztás"),
                description =
                    t("A Statisztika megmutatja a trendeket, havi célokat, kiugró fogyasztást és a rezsikerethez viszonyított állapotot."),
                icon =
                    Icons.Default.BarChart,
                eyebrow =
                    t("STATISZTIKA"),
                previewTitle =
                    t("3 havi átlag"),
                previewPrimary =
                    t("−8% az előző hónaphoz képest"),
                previewSecondary =
                    t("Havi cél · jó úton vagy")
            ),
            TutorialPage(
                title =
                    t("Használjátok együtt otthon"),
                description =
                    t("A Családi csoportban több telefonról is ugyanazokat a mérőállásokat kezelhetitek. Így bárki elvégezheti a havi leolvasást."),
                icon =
                    Icons.Default.Groups,
                eyebrow =
                    t("CSALÁDI CSOPORT"),
                previewTitle =
                    t("Otthon"),
                previewPrimary =
                    t("2 családtag csatlakozva"),
                previewSecondary =
                    t("Legutóbbi szinkron · most")
            ),
            TutorialPage(
                title =
                    t("Állítsd be magadra"),
                description =
                    t("A Beállításokban kezelheted a nyelvet, témát, tarifákat, célokat, emlékeztetőket, biztonsági mentést és a fiókodat."),
                icon =
                    Icons.Default.Settings,
                eyebrow =
                    t("BEÁLLÍTÁSOK"),
                previewTitle =
                    t("Saját beállításaid"),
                previewPrimary =
                    t("Magyar · Sötét téma"),
                previewSecondary =
                    t("Havi emlékeztető · 23. nap")
            )
        )

    val pagerState =
        rememberPagerState(
            pageCount = {
                pages.size
            }
        )
    val scope =
        rememberCoroutineScope()
    val isLast =
        pagerState.currentPage ==
            pages.lastIndex

    val composition by
        rememberLottieComposition(
            LottieCompositionSpec.RawRes(
                R.raw.auth_energy
            )
        )
    val lottieProgress by
        animateLottieCompositionAsState(
            composition = composition,
            iterations =
                LottieConstants.IterateForever
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer,
                            MaterialTheme
                                .colorScheme
                                .surface,
                            MaterialTheme
                                .colorScheme
                                .surface
                        )
                    )
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        top = 24.dp,
                        bottom = 20.dp
                    )
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {
                    Text(
                        t("Ismerd meg a Mérőóra Naplót"),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )
                    Text(
                        (pagerState.currentPage + 1)
                            .toString() +
                            " / " +
                            pages.size,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        authViewModel
                            .completeOnboarding()
                    }
                ) {
                    Text(
                        t("Kihagyás")
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {
                page ->
                TutorialPageContent(
                    page = pages[page],
                    showLottie =
                        page == 0 ||
                            page ==
                            pages.lastIndex,
                    composition =
                        composition,
                    lottieProgress =
                        lottieProgress,
                    selected =
                        page ==
                            pagerState
                                .currentPage
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        ),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                pages.indices.forEach {
                    index ->
                    val width by
                        animateDpAsState(
                            targetValue =
                                if (
                                    index ==
                                    pagerState
                                        .currentPage
                                ) {
                                    26.dp
                                } else {
                                    8.dp
                                },
                            label =
                                "tutorial-dot"
                        )

                    Box(
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = 3.dp
                                )
                                .width(width)
                                .height(8.dp)
                                .background(
                                    color =
                                        if (
                                            index ==
                                            pagerState
                                                .currentPage
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .outlineVariant
                                        },
                                    shape =
                                        CircleShape
                                )
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                if (
                    pagerState.currentPage >
                    0
                ) {
                    OutlinedButton(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        onClick = {
                            scope.launch {
                                pagerState
                                    .animateScrollToPage(
                                        pagerState
                                            .currentPage -
                                            1
                                    )
                            }
                        }
                    ) {
                        Text(
                            t("Vissza")
                        )
                    }
                } else {
                    Spacer(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }

                Button(
                    modifier =
                        Modifier.weight(
                            1.35f
                        ),
                    onClick = {
                        if (isLast) {
                            authViewModel
                                .completeOnboarding()
                        } else {
                            scope.launch {
                                pagerState
                                    .animateScrollToPage(
                                        pagerState
                                            .currentPage +
                                            1
                                    )
                            }
                        }
                    }
                ) {
                    if (isLast) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription =
                                null
                        )
                        Spacer(
                            Modifier.width(
                                8.dp
                            )
                        )
                        Text(
                            t("Kezdjük!")
                        )
                    } else {
                        Text(
                            t("Tovább")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialPageContent(
    page: TutorialPage,
    showLottie: Boolean,
    composition:
        com.airbnb.lottie
            .LottieComposition?,
    lottieProgress: Float,
    selected: Boolean
) {
    val scale by
        animateFloatAsState(
            targetValue =
                if (selected) {
                    1f
                } else {
                    0.94f
                },
            label =
                "tutorial-scale"
        )
    val alpha by
        animateFloatAsState(
            targetValue =
                if (selected) {
                    1f
                } else {
                    0.6f
                },
            label =
                "tutorial-alpha"
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 10.dp
                )
                .scale(scale)
                .alpha(alpha),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        if (showLottie) {
            LottieAnimation(
                composition =
                    composition,
                progress = {
                    lottieProgress
                },
                modifier =
                    Modifier.size(
                        145.dp
                    )
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(
                            118.dp
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer,
                            CircleShape
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    page.icon,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            56.dp
                        ),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }

        Spacer(
            Modifier.height(
                18.dp
            )
        )

        Text(
            page.eyebrow,
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .primary,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(
                6.dp
            )
        )

        Text(
            page.title,
            style =
                MaterialTheme
                    .typography
                    .headlineMedium,
            fontWeight =
                FontWeight.Bold,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            Modifier.height(
                10.dp
            )
        )

        Text(
            page.description,
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            Modifier.height(
                22.dp
            )
        )

        AnimatedContent(
            targetState =
                page.previewTitle,
            transitionSpec = {
                fadeIn() togetherWith
                    fadeOut()
            },
            label =
                "tutorial-preview"
        ) {
            title ->
            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        24.dp
                    ),
                colors =
                    CardDefaults
                        .elevatedCardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceContainerLow
                        )
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            20.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            9.dp
                        )
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            page.icon,
                            contentDescription =
                                null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                        Spacer(
                            Modifier.width(
                                10.dp
                            )
                        )
                        Text(
                            title,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    Text(
                        page.previewPrimary,
                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Text(
                        page.previewSecondary,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}
