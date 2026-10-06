<script setup>
import { features } from '../data/features'
import { t } from '../i18n'
import FeatureCard from '../components/FeatureCard.vue'
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div class="eyebrow">{{ t('features.eyebrow') }}</div>
        <h1>{{ t('features.title') }}</h1>
        <p>{{ t('features.lead') }}</p>
      </div>

      <div class="grid grid-2">
        <FeatureCard v-for="f in features" :key="f.id" :feature="f" />
      </div>

      <div class="note" style="margin-top: 32px">
        <div class="note-title">{{ t('features.safetyTitle') }}</div>
        <ul style="font-size: 0.89rem; margin: 0">
          <li>{{ t('features.safety1') }}</li>
          <li>{{ t('features.safety2') }}</li>
          <li>{{ t('features.safety3') }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>
