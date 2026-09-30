import sys

path = 'c:/Users/Administrator/Downloads/ChunkoidNG/app/src/main/java/com/noches/chunkoidng/ui/screens/tutorial/TutorialScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if 'import com.noches.chunkoidng.R' not in content:
    content = content.replace('import android.content.Intent', 'import android.content.Intent\nimport com.noches.chunkoidng.R\nimport androidx.compose.ui.res.stringResource')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
