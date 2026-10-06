<script setup>
import { computed } from 'vue'
import { site } from '../data/features'
import { t, locale, localized } from '../i18n'

const thanks = [
  { name: 'Dozener (DozenesStudio)', roleKey: 'aboutRoleOriginal' },
  { name: 'DICECAN (EncoreTeam)', roleKey: 'aboutRoleMaintainer' },
  { name: 'Ryan Steven', roleKey: 'aboutRoleContributor' }
]

const roles = computed(() => ({
  aboutRoleOriginal: { 'zh-CN': '原作者与奠基人', en: 'Original author & founder', ja: '原作者・創始者' },
  aboutRoleMaintainer: { 'zh-CN': '当前维护与重构团队', en: 'Current maintainer & rewrite team', ja: '現在のメンテナンス・リファクタリングチーム' },
  aboutRoleContributor: { 'zh-CN': '核心贡献者', en: 'Core contributor', ja: '主要コントリビューター' }
}))

const deps = [
  { name: 'The Hive - Chunker', desc: { 'zh-CN': '核心转换引擎', en: 'Core conversion engine', ja: '中核となる変換エンジン' }, license: 'MIT', url: 'https://github.com/HiveGamesOSS/Chunker' },
  { name: 'HTMonkeyG - XOR-MC-Archive-Decrypt', desc: { 'zh-CN': '网易存档异或解密算法参考', en: 'Reference for NetEase XOR decryption', ja: 'NetEase セーブの XOR 復号アルゴリズムの参考' }, license: 'GPL-3.0', url: 'https://github.com/HTMonkeyG/XOR-MC-Archive-Decrypt' },
  { name: 'PowerNukkit - NBT-Manipulator', desc: { 'zh-CN': 'NBT 读写库', en: 'NBT read/write library', ja: 'NBT 読み書きライブラリ' }, license: 'MIT', url: 'https://github.com/PowerNukkit/NBT-Manipulator' },
  { name: 'iq80 - leveldb', desc: { 'zh-CN': '纯 Java LevelDB 实现', en: 'Pure-Java LevelDB implementation', ja: '純 Java の LevelDB 実装' }, license: 'Apache-2.0', url: 'https://github.com/dain/leveldb' },
  { name: 'HiveGamesOSS - leveldb-mcpe-java', desc: { 'zh-CN': '基岩版 LevelDB 支持', en: 'Bedrock LevelDB support', ja: '統合版 LevelDB のサポート' }, license: 'Apache-2.0 / BSD', url: 'https://github.com/HiveGamesOSS/leveldb-mcpe-java' },
  { name: 'Dicecan - NetEaseDecryptorSDK', desc: { 'zh-CN': '网易解密 SDK', en: 'NetEase decryption SDK', ja: 'NetEase 復号 SDK' }, license: 'GPL-3.0', url: 'https://github.com/Dicecan/NetEaseDecryptorSDK' }
]

const people = computed(() => thanks.map((p) => ({ name: p.name, role: localized(roles.value[p.roleKey]) })))
const dependencyList = computed(() => deps.map((d) => ({ ...d, descText: localized(d.desc) })))

const stack = computed(() => [
  { k: t('about.stackLang'), v: 'Kotlin 2.0+' },
  { k: t('about.stackUi'), v: 'Jetpack Compose (Material Design 3)' },
  { k: t('about.stackAsync'), v: 'Kotlinx Coroutines & Flow' },
  { k: t('about.stackSandbox'), v: site.runtime },
  { k: t('about.stackEngine'), v: site.engine },
  { k: t('about.stackMin'), v: site.minAndroid }
])
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('about.eyebrow') }}</div>
        <h1>{{ t('about.title') }}</h1>
        <p>{{ t('about.lead', { name: site.name, maintainer: site.maintainer, author: site.author }) }}</p>
      </div>

      <section class="section">
        <h2>{{ t('about.background') }}</h2>
        <div class="card">
          <p style="margin-top: 0">{{ t('about.backgroundDesc') }}</p>
          <ul style="margin-bottom: 0">
            <li>{{ t('about.point1') }}</li>
            <li>{{ t('about.point2') }}</li>
            <li>{{ t('about.point3', { version: site.version }) }}</li>
          </ul>
        </div>
      </section>

      <section class="section">
        <h2>{{ t('about.stack') }}</h2>
        <div class="card">
          <table class="table">
            <tbody>
              <tr v-for="row in stack" :key="row.k">
                <th style="width: 170px">{{ row.k }}</th>
                <td>{{ row.v }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="section">
        <h2>{{ t('about.thanks') }}</h2>
        <div class="grid grid-3">
          <div v-for="p in people" :key="p.name" class="card">
            <div class="person-name">{{ p.name }}</div>
            <div class="small muted">{{ p.role }}</div>
          </div>
        </div>
      </section>

      <section class="section">
        <h2>{{ t('about.thirdParty') }}</h2>
        <div class="card">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t('about.thComponent') }}</th>
                <th>{{ t('about.thUsage') }}</th>
                <th style="width: 140px">{{ t('about.thLicense') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dependencyList" :key="d.name">
                <td>
                  <a :href="d.url" target="_blank" rel="noopener">{{ d.name }} ↗</a>
                </td>
                <td>{{ d.descText }}</td>
                <td>{{ d.license }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="section">
        <h2>{{ t('about.licenseTitle') }}</h2>
        <div class="card">
          <p style="margin-top: 0">{{ t('about.licenseDesc') }}</p>
          <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 14px">
            <a :href="site.repo" target="_blank" rel="noopener" class="btn">{{ t('download.viewSource') }} ↗</a>
            <a :href="site.repo + '/blob/main/LICENSE'" target="_blank" rel="noopener" class="btn">
              {{ t('about.readLicense') }} ↗
            </a>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.person-name { font-weight: 600; font-size: 0.93rem; margin-bottom: 3px; }

@media (max-width: 640px) {
  .person-name { font-size: 0.95rem; }

  .table a { display: inline-block; padding: 3px 0; }
}

@media (max-width: 640px) {
  .table a { min-height: 40px; display: inline-flex; align-items: center; }
}
</style>