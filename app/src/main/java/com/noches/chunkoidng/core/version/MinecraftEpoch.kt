package com.noches.chunkoidng.core.version

enum class MinecraftEpoch(
    val minorVersion: Int,
    val titleEn: String,
    val titleZh: String,
    val titleJa: String,
    val hasExtendedHeight: Boolean = false,
    val hasFlattening: Boolean = true
) {
    EPOCH_1_8(8, "Colorful World", "多彩世界", "カラフルワールド", false, false),
    EPOCH_1_9_TO_1_11(11, "Combat & Exploration", "战斗与探索", "戦闘と探検", false, false),
    EPOCH_1_12(12, "World of Color", "多彩世界", "色彩の世界", false, false),
    EPOCH_1_13(13, "Update Aquatic", "水域更新", "アクアティック更新", false, true),
    EPOCH_1_14(14, "Village & Pillage", "村庄与掠夺", "村と略奪", false, true),
    EPOCH_1_15(15, "Buzzy Bees", "嗡嗡蜂群", "ミツバチ更新", false, true),
    EPOCH_1_16(16, "Nether Update", "下界更新", "ネザー更新", false, true),
    EPOCH_1_17(17, "Caves & Cliffs Part I", "洞穴与山崖 I", "洞窟と崖 第1弾", false, true),
    EPOCH_1_18(18, "Caves & Cliffs Part II", "洞穴与山崖 II", "洞窟と崖 第2弾", true, true),
    EPOCH_1_19(19, "The Wild Update", "荒野更新", "ワイルド更新", true, true),
    EPOCH_1_20(20, "Trails & Tales", "足迹与故事", "旅路と物語", true, true),
    EPOCH_1_21(21, "Tricky Trials", "棘巧试炼", "トリッキートライアル", true, true),
    EPOCH_FUTURE(26, "Future Updates", "未来版本", "将来の更新", true, true);

    companion object {
        fun fromMinor(minor: Int): MinecraftEpoch {
            return when {
                minor <= 8 -> EPOCH_1_8
                minor in 9..11 -> EPOCH_1_9_TO_1_11
                minor == 12 -> EPOCH_1_12
                minor == 13 -> EPOCH_1_13
                minor == 14 -> EPOCH_1_14
                minor == 15 -> EPOCH_1_15
                minor == 16 -> EPOCH_1_16
                minor == 17 -> EPOCH_1_17
                minor == 18 -> EPOCH_1_18
                minor == 19 -> EPOCH_1_19
                minor == 20 -> EPOCH_1_20
                minor == 21 -> EPOCH_1_21
                else -> EPOCH_FUTURE
            }
        }
    }
}
