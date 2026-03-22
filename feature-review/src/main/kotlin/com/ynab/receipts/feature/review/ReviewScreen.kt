package com.ynab.receipts.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReviewScreen(
    payeeInput: String,
    amountInput: String,
    dateInput: String,
    currency: String,
    confidence: Map<String, Float>,
    ocrContextLines: List<String>,
    onPayeeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Review extracted fields")
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = payeeInput,
            onValueChange = onPayeeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Payee") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount ($currency)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dateInput,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Date (YYYY-MM-DD or MM/DD/YYYY)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Confidence")
        Text("Payee: ${confidence["payee"]?.times(100)?.toInt() ?: 0}%")
        Text("Amount: ${confidence["amount"]?.times(100)?.toInt() ?: 0}%")
        Text("Date: ${confidence["date"]?.times(100)?.toInt() ?: 0}%")

        Spacer(modifier = Modifier.height(16.dp))
        Text("OCR context")
        if (ocrContextLines.isEmpty()) {
            Text("No OCR lines available")
        } else {
            ocrContextLines.forEach { line ->
                Text("• $line")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onConfirm) {
            Text("Confirm and queue")
        }
    }
}
