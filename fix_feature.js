const fs = require('fs');

function replaceInFile(path, replacements) {
    let content = fs.readFileSync(path, 'utf8');
    for (const [oldStr, newStr] of replacements) {
        content = content.split(oldStr).join(newStr);
    }
    fs.writeFileSync(path, content, 'utf8');
}

const featureItemReps = [
    ['val title: String,', 'val titleRes: Int,'],
    ['val subtitle: String,', 'val subtitleRes: Int,'],
    ['val badge: String,', 'val badgeRes: Int,'],
    ['val tags: List<String>,', 'val tagsRes: List<Int>,'],
    ['title = stringResource(R.string.feature_conv_title),', 'titleRes = R.string.feature_conv_title,'],
    ['subtitle = stringResource(R.string.feature_conv_subtitle),', 'subtitleRes = R.string.feature_conv_subtitle,'],
    ['badge = stringResource(R.string.feature_conv_badge),', 'badgeRes = R.string.feature_conv_badge,'],
    ['tags = listOf(stringResource(R.string.feature_conv_tag_1), stringResource(R.string.feature_conv_tag_2), stringResource(R.string.feature_conv_tag_3)),', 'tagsRes = listOf(R.string.feature_conv_tag_1, R.string.feature_conv_tag_2, R.string.feature_conv_tag_3),'],
    ['title = stringResource(R.string.feature_decrypt_title),', 'titleRes = R.string.feature_decrypt_title,'],
    ['subtitle = stringResource(R.string.feature_decrypt_subtitle),', 'subtitleRes = R.string.feature_decrypt_subtitle,'],
    ['badge = stringResource(R.string.feature_decrypt_badge),', 'badgeRes = R.string.feature_decrypt_badge,'],
    ['tags = listOf(stringResource(R.string.feature_decrypt_tag_1), stringResource(R.string.feature_decrypt_tag_2), stringResource(R.string.feature_decrypt_tag_3)),', 'tagsRes = listOf(R.string.feature_decrypt_tag_1, R.string.feature_decrypt_tag_2, R.string.feature_decrypt_tag_3),'],
    ['title = stringResource(R.string.feature_prune_title),', 'titleRes = R.string.feature_prune_title,'],
    ['subtitle = stringResource(R.string.feature_prune_subtitle),', 'subtitleRes = R.string.feature_prune_subtitle,'],
    ['badge = stringResource(R.string.feature_prune_badge),', 'badgeRes = R.string.feature_prune_badge,'],
    ['tags = listOf(stringResource(R.string.feature_prune_tag_1), stringResource(R.string.feature_prune_tag_2), stringResource(R.string.feature_prune_tag_3)),', 'tagsRes = listOf(R.string.feature_prune_tag_1, R.string.feature_prune_tag_2, R.string.feature_prune_tag_3),'],
    ['title = stringResource(R.string.feature_nbt_title),', 'titleRes = R.string.feature_nbt_title,'],
    ['subtitle = stringResource(R.string.feature_nbt_subtitle),', 'subtitleRes = R.string.feature_nbt_subtitle,'],
    ['badge = stringResource(R.string.feature_nbt_badge),', 'badgeRes = R.string.feature_nbt_badge,'],
    ['tags = listOf(stringResource(R.string.feature_nbt_tag_1), stringResource(R.string.feature_nbt_tag_2), stringResource(R.string.feature_nbt_tag_3)),', 'tagsRes = listOf(R.string.feature_nbt_tag_1, R.string.feature_nbt_tag_2, R.string.feature_nbt_tag_3),'],
    ['title = stringResource(R.string.feature_res_title),', 'titleRes = R.string.feature_res_title,'],
    ['subtitle = stringResource(R.string.feature_res_subtitle),', 'subtitleRes = R.string.feature_res_subtitle,'],
    ['badge = stringResource(R.string.feature_res_badge),', 'badgeRes = R.string.feature_res_badge,'],
    ['tags = listOf(stringResource(R.string.feature_res_tag_1), stringResource(R.string.feature_res_tag_2), stringResource(R.string.feature_res_tag_3)),', 'tagsRes = listOf(R.string.feature_res_tag_1, R.string.feature_res_tag_2, R.string.feature_res_tag_3),'],
    ['title = stringResource(R.string.feature_cli_title),', 'titleRes = R.string.feature_cli_title,'],
    ['subtitle = stringResource(R.string.feature_cli_subtitle),', 'subtitleRes = R.string.feature_cli_subtitle,'],
    ['badge = stringResource(R.string.feature_cli_badge),', 'badgeRes = R.string.feature_cli_badge,'],
    ['tags = listOf(stringResource(R.string.feature_cli_tag_1), stringResource(R.string.feature_cli_tag_2), stringResource(R.string.feature_cli_tag_3)),', 'tagsRes = listOf(R.string.feature_cli_tag_1, R.string.feature_cli_tag_2, R.string.feature_cli_tag_3),']
];
replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/features/FeatureItem.kt', featureItemReps);

const featureCardReps = [
    ['feature.badge', 'stringResource(feature.badgeRes)'],
    ['feature.title', 'stringResource(feature.titleRes)'],
    ['feature.subtitle', 'stringResource(feature.subtitleRes)'],
    ['feature.tags.forEach { tag ->', 'feature.tagsRes.forEach { tag ->\n                                val tagStr = stringResource(tag)'],
    ['Text(tag,', 'Text(tagStr,'],
];
replaceInFile('c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/features/FeatureCard.kt', featureCardReps);
