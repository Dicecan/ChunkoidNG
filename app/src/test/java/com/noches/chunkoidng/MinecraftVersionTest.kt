package com.noches.chunkoidng

import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.version.MinecraftEpoch
import com.noches.chunkoidng.core.version.MinecraftVersion
import com.noches.chunkoidng.core.version.RelationType
import com.noches.chunkoidng.core.version.RiskLevel
import com.noches.chunkoidng.core.version.VersionMappingTable
import com.noches.chunkoidng.core.world.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinecraftVersionTest {

    @Test
    fun testParseBedrockVersion() {
        val v = MinecraftVersion.parse("1.21.50", Platform.BEDROCK)
        assertEquals(1, v.major)
        assertEquals(21, v.minor)
        assertEquals(50, v.patch)
        assertEquals(MinecraftEpoch.EPOCH_1_21, v.epoch)

        val vWithBuild = MinecraftVersion.parse("1.20.80.20", Platform.BEDROCK)
        assertEquals(1, vWithBuild.major)
        assertEquals(20, vWithBuild.minor)
        assertEquals(80, vWithBuild.patch)
        assertEquals(20, vWithBuild.build)
        assertEquals(MinecraftEpoch.EPOCH_1_20, vWithBuild.epoch)
    }

    @Test
    fun testParseJavaVersionWithDataVersion() {
        val v = MinecraftVersion.parse(null, Platform.JAVA, 3700)
        assertEquals(1, v.major)
        assertEquals(20, v.minor)
        assertEquals(4, v.patch)
        assertEquals(MinecraftEpoch.EPOCH_1_20, v.epoch)

        val vName = MinecraftVersion.parse("1.16.5", Platform.JAVA)
        assertEquals(1, vName.major)
        assertEquals(16, vName.minor)
        assertEquals(5, vName.patch)
        assertEquals(MinecraftEpoch.EPOCH_1_16, vName.epoch)
    }

    @Test
    fun testVersionComparison() {
        val v1_21 = MinecraftVersion.parse("1.21.50", Platform.BEDROCK)
        val v1_20 = MinecraftVersion.parse("1.20.80", Platform.BEDROCK)
        val v1_16 = MinecraftVersion.parse("1.16.5", Platform.JAVA)

        assertTrue(v1_21.isNewerThan(v1_20))
        assertTrue(v1_20.isOlderThan(v1_21))
        assertTrue(v1_21.isNewerThan(v1_16))
        assertFalse(v1_21.isSameEpoch(v1_20))
    }

    @Test
    fun testCrossPlatformEquivalentRecommendation() {
        val jeSource = MinecraftVersion.parse("1.20.4", Platform.JAVA)
        val recommendedBe = VersionMappingTable.findEquivalentFormat(jeSource, Platform.BEDROCK)
        assertEquals("BEDROCK_1_20_80", recommendedBe.id)

        val beSource = MinecraftVersion.parse("1.16.220", Platform.BEDROCK)
        val recommendedJe = VersionMappingTable.findEquivalentFormat(beSource, Platform.JAVA)
        assertEquals("JAVA_1_16_5", recommendedJe.id)

        val je112 = MinecraftVersion.parse("1.12.2", Platform.JAVA)
        val be112 = VersionMappingTable.findEquivalentFormat(je112, Platform.BEDROCK)
        assertEquals("BEDROCK_1_12_0", be112.id)
    }

    @Test
    fun testRelationEvaluationAndRisks() {
        val source1_20 = MinecraftVersion.parse("1.20.4", Platform.JAVA)
        val target1_16 = ChunkerFormat.findById("JAVA_1_16_5")!!
        val result1_16 = VersionMappingTable.evaluateRelation(source1_20, target1_16)

        assertEquals(RelationType.DOWNGRADE, result1_16.relationType)
        assertEquals(RiskLevel.HIGH, result1_16.riskLevel)
        assertEquals("risk_height_truncation", result1_16.warningKey)

        val target1_21 = ChunkerFormat.findById("JAVA_1_21_4")!!
        val result1_21 = VersionMappingTable.evaluateRelation(source1_20, target1_21)
        assertEquals(RelationType.UPGRADE, result1_21.relationType)
        assertEquals(RiskLevel.NONE, result1_21.riskLevel)

        val targetEquivalent = ChunkerFormat.findById("BEDROCK_1_20_80")!!
        val resultEquivalent = VersionMappingTable.evaluateRelation(source1_20, targetEquivalent)
        assertEquals(RelationType.RECOMMENDED_MATCH, resultEquivalent.relationType)
        assertTrue(resultEquivalent.isRecommended)
    }

    @Test
    fun testCrossPlatformRelationUsesEpochOnly() {
        val source = MinecraftVersion.parse("1.20.4", Platform.JAVA)
        val target = ChunkerFormat.findById("BEDROCK_1_20_80")!!
        val result = VersionMappingTable.evaluateRelation(source, target)
        assertEquals(RelationType.RECOMMENDED_MATCH, result.relationType)
    }

    @Test
    fun testMalformedVersionFallsBackSafely() {
        val parsed = MinecraftVersion.parse("unknown-version", Platform.BEDROCK)
        assertEquals("unknown-version", parsed.displayString)
    }
}
