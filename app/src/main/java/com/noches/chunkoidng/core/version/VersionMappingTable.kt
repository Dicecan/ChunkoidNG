package com.noches.chunkoidng.core.version

import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.world.Platform

enum class RelationType {
    RECOMMENDED_MATCH,
    UPGRADE,
    DOWNGRADE,
    SAME_INPUT
}

enum class RiskLevel {
    NONE,
    MODERATE,
    HIGH
}

data class VersionRelationResult(
    val relationType: RelationType,
    val riskLevel: RiskLevel,
    val isRecommended: Boolean,
    val warningKey: String? = null
)

object VersionMappingTable {

    private val EQUIVALENT_MAPPINGS: Map<MinecraftEpoch, Pair<String, String>> = mapOf(
        MinecraftEpoch.EPOCH_1_21 to Pair("JAVA_1_21_4", "BEDROCK_1_21_50"),
        MinecraftEpoch.EPOCH_1_20 to Pair("JAVA_1_20_4", "BEDROCK_1_20_80"),
        MinecraftEpoch.EPOCH_1_19 to Pair("JAVA_1_19_4", "BEDROCK_1_19_80"),
        MinecraftEpoch.EPOCH_1_18 to Pair("JAVA_1_18_2", "BEDROCK_1_18_30"),
        MinecraftEpoch.EPOCH_1_17 to Pair("JAVA_1_17_1", "BEDROCK_1_17_40"),
        MinecraftEpoch.EPOCH_1_16 to Pair("JAVA_1_16_5", "BEDROCK_1_16_220"),
        MinecraftEpoch.EPOCH_1_15 to Pair("JAVA_1_15_2", "BEDROCK_1_14_60"),
        MinecraftEpoch.EPOCH_1_14 to Pair("JAVA_1_14_4", "BEDROCK_1_14_60"),
        MinecraftEpoch.EPOCH_1_13 to Pair("JAVA_1_13_2", "BEDROCK_1_13_0"),
        MinecraftEpoch.EPOCH_1_12 to Pair("JAVA_1_12_2", "BEDROCK_1_12_0"),
        MinecraftEpoch.EPOCH_1_9_TO_1_11 to Pair("JAVA_1_11_2", "BEDROCK_1_12_0"),
        MinecraftEpoch.EPOCH_1_8 to Pair("JAVA_1_8_8", "BEDROCK_1_12_0"),
        MinecraftEpoch.EPOCH_FUTURE to Pair("JAVA_26_1", "BEDROCK_1_26_0")
    )

    fun findEquivalentFormat(source: MinecraftVersion, targetPlatform: Platform): ChunkerFormat {
        val mapping = EQUIVALENT_MAPPINGS[source.epoch]
        val targetId = if (mapping != null) {
            if (targetPlatform == Platform.JAVA) mapping.first else mapping.second
        } else {
            if (targetPlatform == Platform.JAVA) "JAVA_1_21_4" else "BEDROCK_1_21_50"
        }
        return ChunkerFormat.findById(targetId) ?: ChunkerFormat.getDefaultFormatForOpposite(source.platform)
    }

    fun evaluateRelation(source: MinecraftVersion?, target: ChunkerFormat): VersionRelationResult {
        if (target.id == "INPUT") {
            return VersionRelationResult(RelationType.SAME_INPUT, RiskLevel.NONE, false, null)
        }
        if (source == null) {
            return VersionRelationResult(RelationType.UPGRADE, RiskLevel.NONE, false, null)
        }

        val targetVersion = MinecraftVersion.parseFromChunkerId(target.id)
            ?: return VersionRelationResult(RelationType.UPGRADE, RiskLevel.NONE, false, null)

        val isEquivalent = (source.epoch == targetVersion.epoch)
        val isSamePlatform = (source.platform == target.platform)

        if (isSamePlatform) {
            return when {
                source == targetVersion -> {
                    VersionRelationResult(RelationType.RECOMMENDED_MATCH, RiskLevel.NONE, true, null)
                }
                targetVersion.isNewerThan(source) -> {
                    VersionRelationResult(RelationType.UPGRADE, RiskLevel.NONE, false, null)
                }
                else -> {
                    val risk = calculateDowngradeRisk(source, targetVersion)
                    VersionRelationResult(RelationType.DOWNGRADE, risk.first, false, risk.second)
                }
            }
        } else {
            if (isEquivalent) {
                val equivalentFormat = findEquivalentFormat(source, target.platform)
                val isExactRecommended = (equivalentFormat.id == target.id)
                return VersionRelationResult(RelationType.RECOMMENDED_MATCH, RiskLevel.NONE, isExactRecommended, null)
            } else if (targetVersion.epoch.minorVersion > source.epoch.minorVersion) {
                return VersionRelationResult(RelationType.UPGRADE, RiskLevel.NONE, false, null)
            } else {
                val risk = calculateDowngradeRisk(source, targetVersion)
                return VersionRelationResult(RelationType.DOWNGRADE, risk.first, false, risk.second)
            }
        }
    }

    private fun calculateDowngradeRisk(source: MinecraftVersion, target: MinecraftVersion): Pair<RiskLevel, String> {
        val sourceHasHeight = source.epoch.hasExtendedHeight
        val targetHasHeight = target.epoch.hasExtendedHeight
        if (sourceHasHeight && !targetHasHeight) {
            return Pair(RiskLevel.HIGH, "risk_height_truncation")
        }

        val sourceHasFlattening = source.epoch.hasFlattening
        val targetHasFlattening = target.epoch.hasFlattening
        if (sourceHasFlattening && !targetHasFlattening) {
            return Pair(RiskLevel.HIGH, "risk_block_flattening")
        }

        return Pair(RiskLevel.MODERATE, "risk_general_downgrade")
    }
}
