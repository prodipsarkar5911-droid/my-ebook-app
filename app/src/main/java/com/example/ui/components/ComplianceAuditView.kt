package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ValidationResult
import com.example.ui.EBookUiState
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError

@Composable
fun ComplianceAuditView(uiState: EBookUiState) {
    val validation = uiState.validationResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (validation == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Rule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No Document Audited Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Run OCR on a PDF or scanned book page to inspect strict HTML 3.2 compliance and structural rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Overall Score Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HTML 3.2 Compliance Audit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (validation.isStrictHtml32) "Strict Standards Verified" else "Minor Issues Detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (validation.isStrictHtml32) EmeraldSuccess else MaterialTheme.colorScheme.error
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (validation.scorePercentage >= 90) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${validation.scorePercentage}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (validation.scorePercentage >= 90) EmeraldSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { validation.scorePercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (validation.scorePercentage >= 90) EmeraldSuccess else MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Rules Checklist Grid
            Text(
                text = "Strict System Instruction Rules",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Rule 1: Strict HTML 3.2 (No HTML5 tags)
            val noHtml5Passed = validation.forbiddenHtml5TagsFound.isEmpty()
            RuleItemCard(
                title = "Strict HTML 3.2 (No HTML5)",
                description = if (noHtml5Passed) "Zero modern HTML5 tags found (<main>, <section>, <article>, <header>)." else "Forbidden HTML5 tags found: ${validation.forbiddenHtml5TagsFound.joinToString()}",
                passed = noHtml5Passed,
                icon = Icons.Default.Rule
            )

            // Rule 2: No Inline Styles
            val noInlineStylesPassed = validation.inlineStylesFoundCount == 0
            RuleItemCard(
                title = "Pure Structural Markup (No Inline Styles)",
                description = if (noInlineStylesPassed) "No inline style=\"...\" attributes present in code." else "Found ${validation.inlineStylesFoundCount} inline style attributes.",
                passed = noInlineStylesPassed,
                icon = Icons.Default.Style
            )

            // Rule 3: Paragraph Indentation Class
            val indentPassed = validation.paragraphCount == 0 || validation.paragraphsWithIndentCount == validation.paragraphCount
            RuleItemCard(
                title = "Paragraph Indentation (<p class=\"indent\">)",
                description = "${validation.paragraphsWithIndentCount} of ${validation.paragraphCount} paragraphs have exact class=\"indent\".",
                passed = indentPassed,
                icon = Icons.Default.FormatIndentIncrease
            )

            // Rule 4: Headings Hierarchy
            val headingIndentPassed = validation.headingsWithIllegalIndent.isEmpty()
            val totalHeadings = validation.headingsCount.values.sum()
            RuleItemCard(
                title = "Headings Hierarchy (<h1> through <h6>)",
                description = if (headingIndentPassed) "$totalHeadings headings properly mapped without indent class." else "Headings (${validation.headingsWithIllegalIndent.joinToString()}) have illegal indent class.",
                passed = headingIndentPassed,
                icon = Icons.Default.Title
            )

            // Dynamic Spans Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dynamic Spans Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Color Spans (<span class=\"colorX\">):")
                        }
                        Text("${validation.colorSpansCount}", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Font Spans (<span class=\"fontX\">):")
                        }
                        Text("${validation.fontSpansCount}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Detailed Diagnostic Logs
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Audit Log Summary",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    validation.auditSummary.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleItemCard(
    title: String,
    description: String,
    passed: Boolean,
    icon: ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (passed) "Passed" else "Failed",
                tint = if (passed) EmeraldSuccess else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
