package com.example.aquallera.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.aquallera.R
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.Blue
import com.example.aquallera.core.theme.MidnightBlue
import com.example.aquallera.core.theme.TextCard

@Composable
fun AboutScreen(navController: NavHostController? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Aquallera Logo",
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\u201CClean Water, Anytime, Anywhere.\u201D",
            color = Color.Gray.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Aquallera v1.0",
            color = Color.Gray.copy(alpha = 0.5f),
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeading("What is the Aqua-llera App?")
        InfoCard(
            "Aqua-llera is your go-to app for finding nearby water stations and ordering clean, safe water right from your phone. No more stressing over where to get water or waiting forever for deliveries \u2014 we made it simple, organized, and hassle-free. It\u2019s all about making sure you always have quick and dependable access to clean water, whenever you need it."
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeading("Why make Aquallera?", alignEnd = true)
        InfoCard(
            "We created Aqua-llera to help Baguio City residents get clean water more easily and reliably. It also helps centralize information about water stations, so you can find what you need without all the usual hassle.",
            alignEnd = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeading("How to Order")
        InfoCard(
            "Getting started is super easy! Just open the app and allow location access to see nearby water stations. Pick your preferred water type and how much you need, then choose between delivery or pickup. Review your order, hit confirm, and you\u2019re all set \u2014 your clean water will be on its way!"
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .clickable { navController?.navigate("map") { popUpTo("map") { inclusive = true } } }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Let\u2019s start Locating ",
                color = Blue,
                fontSize = 13.sp,
            )
            Text(
                text = "Water Stations!",
                color = Blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeading(text: String, alignEnd: Boolean = false) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MidnightBlue,
        modifier = Modifier.fillMaxWidth(),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun InfoCard(text: String, alignEnd: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = TextCard),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = MidnightBlue,
                lineHeight = 22.sp,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            )
        }
    }
}
